# Contextual Suggestion Score Decay Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fazer sugestões aprendidas refletirem os últimos 30 dias, com meia-vida de 7 dias e mínimo de 20 pontos para recomendações baseadas somente em uso.

**Architecture:** Extrair o histórico para um modelo puro de agregados diários, separado do JSON e de `SharedPreferences`. Um codec versionado fará a migração idempotente do mapa legado; `Prefs` continuará sendo a fronteira de persistência e entregará ao ranking uma pontuação já decaída.

**Tech Stack:** Kotlin/JVM 17, Android `SharedPreferences`, `org.json`, JUnit 4 e Gradle.

**Spec:** `docs/superpowers/specs/2026-11-11-contextual-suggestion-score-decay-design.md`

## Global Constraints

- Cada abertura recente vale 10 pontos.
- Usar `count * 10 * 0.5^(ageDays / 7.0)` e arredondar somente a soma final.
- Idade acima de 30 dias não pontua e deve ser removida na próxima leitura ou escrita.
- O limite padrão do ranking passa de 10 para 20; bônus contextuais ainda podem atingir esse limite.
- Preservar as seis faixas `hour.coerceIn(0, 23) / 4` e a chave de backup `CONTEXTUAL_HISTORY`.
- Não adicionar interface de configuração nem dependência de produção.
- Usar o calendário local; datas futuras têm idade efetiva zero.

## Review Focus

- Mudança de offset/fuso não pode alterar o dia civil local; teste no Task 1.
- Identidade legada contendo `#` deve ser separada pelo último `#`; teste no Task 2.
- JSON parcialmente inválido deve preservar entradas válidas; teste no Task 2.
- Migração repetida não pode duplicar contagens; teste no Task 2.
- Limpeza em leitura deve persistir a remoção sem perder registros válidos; teste no Task 3.

---

### Task 1: Modelo puro de histórico e decaimento

**Files:**
- Create: `app/src/main/java/app/olauncher/data/ContextualUsageHistory.kt`
- Create: `app/src/test/java/app/olauncher/data/ContextualUsageHistoryTest.kt`

**Interfaces:**
- Produces: `ContextualUsageEntry(appIdentity: String, timeBucket: Int, epochDay: Long, count: Int)`.
- Produces: `ContextualUsageHistory.timeBucket(hour)`, `localEpochDay(nowMillis, timeZone)`, `record(...)`, `prune(...)` e `score(...)`.

- [ ] **Step 1: Escrever testes falhos para faixa e dia local**

Testar os limites 0/3/4/23 horas e dois instantes do mesmo dia civil em `America/Sao_Paulo`, incluindo data com mudança histórica de offset.

- [ ] **Step 2: Confirmar a falha**

Run: `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualUsageHistoryTest`

Expected: FAIL porque o modelo não existe.

- [ ] **Step 3: Implementar tipos e conversores temporais**

Derivar ano/mês/dia com `Calendar` no fuso informado e converter esses campos em um dia UTC sintético, evitando divisão de timestamps locais por 24 horas.

- [ ] **Step 4: Escrever testes falhos para registro, pontuação e limpeza**

Cobrir incremento sem duplicata; isolamento por app/faixa/dia; 1 abertura = 10; 2 = 20; duas aberturas com 7 dias = 10; data futura = idade zero; idade 30 conta; idade 31 é removida; arredondamento após a soma.

- [ ] **Step 5: Implementar `record`, `prune` e `score`**

Ignorar contagem não positiva, identidade vazia e faixa fora de `0..5`. Usar `kotlin.math.pow` e `roundToInt`.

- [ ] **Step 6: Verificar e commit**

Run: `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualUsageHistoryTest`

Expected: PASS.

```bash
git add app/src/main/java/app/olauncher/data/ContextualUsageHistory.kt app/src/test/java/app/olauncher/data/ContextualUsageHistoryTest.kt
git commit -m "feat: model decaying contextual usage"
```

### Task 2: Codec versionado e migração idempotente

**Files:**
- Modify: `app/build.gradle`
- Create: `app/src/main/java/app/olauncher/data/ContextualUsageHistoryCodec.kt`
- Create: `app/src/test/java/app/olauncher/data/ContextualUsageHistoryCodecTest.kt`

**Interfaces:**
- Consumes: modelo do Task 1.
- Produces: `DecodedContextualUsage(entries: List<ContextualUsageEntry>, requiresRewrite: Boolean)`.
- Produces: `ContextualUsageHistoryCodec.decode(serialized, migrationEpochDay)` e `encode(entries)`.

- [ ] **Step 1: Habilitar testes locais de JSON**

Adicionar `testImplementation "org.json:json:20240303"` em `app/build.gradle`, sem mudar produção.

- [ ] **Step 2: Escrever testes falhos do formato v2**

Fixar `{"version":2,"entries":[...]}`. Testar round-trip, ordenação estável, descarte parcial com `requiresRewrite` e recuperação de JSON inválido.

- [ ] **Step 3: Confirmar a falha e implementar v2**

Run: `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualUsageHistoryCodecTest`

Expected antes: FAIL; depois de implementar validação campo a campo: PASS.

- [ ] **Step 4: Escrever testes falhos da migração legada**

Para `{"package|user#bucket":count}`, testar data de migração, último `#`, descarte de faixa/contagem/chave inválida e round-trip sem duplicação.

- [ ] **Step 5: Implementar migração e verificar**

Detectar legado pela ausência de `version`; todo legado retorna `requiresRewrite == true`.

Run: `./gradlew testDebugUnitTest --tests 'app.olauncher.data.ContextualUsageHistory*'`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/build.gradle app/src/main/java/app/olauncher/data/ContextualUsageHistoryCodec.kt app/src/test/java/app/olauncher/data/ContextualUsageHistoryCodecTest.kt
git commit -m "feat: migrate contextual usage history"
```

### Task 3: Integrar persistência, ranking e limite mínimo

**Files:**
- Modify: `app/src/main/java/app/olauncher/data/ContextualUsageHistory.kt`
- Modify: `app/src/main/java/app/olauncher/data/Prefs.kt`
- Modify: `app/src/main/java/app/olauncher/data/ContextualSuggestions.kt`
- Modify: `app/src/main/java/app/olauncher/ui/HomeFragment.kt`
- Modify: `app/src/test/java/app/olauncher/data/ContextualSuggestionsTest.kt`
- Create: `app/src/test/java/app/olauncher/data/ContextualUsageRepositoryTest.kt`

**Interfaces:**
- Consumes: codec e modelo dos Tasks 1–2.
- Produces em `Prefs`: `recordContextualLaunch(appIdentity: String, hour: Int, nowMillis: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): Unit`.
- Produces em `Prefs`: `contextualUsageScore(appIdentity: String, hour: Int, nowMillis: Long = System.currentTimeMillis(), timeZone: TimeZone = TimeZone.getDefault()): Int`.
- Produces em `ContextualApp`: substituir `launchesInTimeBucket` por `usageScore`.

- [ ] **Step 1: Definir uma fronteira pura de persistência**

Adicionar `ContextualUsageUpdate(serialized: String, score: Int, changed: Boolean)` e `updateSerializedHistory(serialized, appIdentity: String?, hour: Int?, nowMillis, timeZone)`. A função decodifica, poda, registra opcionalmente e devolve o JSON a persistir.

- [ ] **Step 2: Escrever testes falhos da fronteira**

Provar migração com `changed == true`; releitura sem duplicação e `changed == false`; limpeza de idade 31 preservando idade 30; incremento somente do app/faixa/dia; recuperação de JSON corrompido.

- [ ] **Step 3: Confirmar a falha e implementar a fronteira**

Run: `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualUsageRepositoryTest`

Expected antes: FAIL; depois: PASS.

- [ ] **Step 4: Substituir a persistência em `Prefs`**

Remover a manipulação direta de JSON. Persistir apenas quando `changed`. Manter a guarda que só registra apps habilitados para sugestões. Substituir `contextualLaunches` por `contextualUsageScore`.

- [ ] **Step 5: Escrever testes falhos do limite e bônus**

Atualizar helpers para `usageScore` e testar: 10 sem bônus não recomenda; 20 recomenda; zero mais regra válida recomenda; zero mais evento de saúde recomenda; ordenação respeita a pontuação recebida.

- [ ] **Step 6: Confirmar a falha e integrar o ranking**

Run: `./gradlew testDebugUnitTest --tests app.olauncher.data.ContextualSuggestionsTest`

Alterar `minimumScore` para 20, iniciar o score com `candidate.usageScore` sem multiplicar por 10, e fazer `HomeFragment` chamar `prefs.contextualUsageScore(identity, hour)`.

- [ ] **Step 7: Verificar testes focados**

Run: `./gradlew testDebugUnitTest --tests 'app.olauncher.data.Contextual*'`

Expected: PASS.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/app/olauncher/data/Prefs.kt app/src/main/java/app/olauncher/data/ContextualSuggestions.kt app/src/main/java/app/olauncher/ui/HomeFragment.kt app/src/test/java/app/olauncher/data/ContextualSuggestionsTest.kt app/src/test/java/app/olauncher/data/ContextualUsageRepositoryTest.kt
git commit -m "feat: decay contextual suggestion scores"
```

### Task 4: Verificação integrada e aparelho

**Files:**
- Modify only if verification identifies a defect in the files above.

**Interfaces:**
- Consumes: implementação completa dos Tasks 1–3.
- Produces: APK verificado sem regressão no histórico, ranking ou backup.

- [ ] **Step 1: Executar toda a suíte**

Run: `./gradlew testDebugUnitTest`

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Verificar o diff**

Run: `git diff --check`

Expected: nenhuma saída.

- [ ] **Step 3: Executar lint e classificar falhas novas**

Run: `./gradlew lintDebug`

Expected: nenhuma falha nova nos arquivos alterados. Comparar com a dívida preexistente; não criar baseline silenciosamente.

- [ ] **Step 4: Instalar no aparelho**

Run: `adb devices -l && ./gradlew installDebug`

Expected: dispositivo autorizado e `Installed on 1 device`.

- [ ] **Step 5: Validar migração no dispositivo**

Abrir o launcher e confirmar sugestões, regras e abertura de apps. Com `run-as app.olauncher.debug`, verificar que `CONTEXTUAL_HISTORY` virou `version: 2` e que uma abertura incrementa apenas o agregado atual, sem editar preferências manualmente.

- [ ] **Step 6: Reexecutar verificações após correções**

Run: `./gradlew testDebugUnitTest && git diff --check`

Expected: sucesso e nenhuma saída do diff.

- [ ] **Step 7: Commit final somente se houver correções**

```bash
git add app/src/main app/src/test app/build.gradle
git commit -m "fix: verify contextual score decay"
```

