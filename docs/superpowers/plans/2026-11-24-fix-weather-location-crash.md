# Correção do crash e reutilização da localização na tela inicial

> Para executar este plano, use `superpowers:executing-plans` ou `superpowers:subagent-driven-development`.

O `logcat` registrou 10 ocorrências de `NullPointerException` entre 21 e 24 de novembro de 2026. A mais recente ocorreu às 19:04:51 em `HomeFragment.requestCurrentLocation`. O Android entregou `null` ao callback e o Kotlin lançou a exceção antes de o código consultar a última localização conhecida.

## Mudanças

- Em `WeatherSnapshot`, centralizar a validade de 30 minutos da previsão. Em `HomeFragment`, exibir a previsão em cache e pedir localização para o clima automático somente quando a previsão precisar ser atualizada. A cidade manual continua usando suas coordenadas.
- Usar `HomeLocationCache` para compartilhar uma consulta em andamento e reaproveitar em memória uma posição com menos de dois minutos, medidos desde a captura pelo Android. O mesmo callback serve ao clima e às sugestões contextuais. Manter o cache no `MainViewModel` permite reaproveitar a consulta durante a recriação da tela.
- Receber o retorno como `Consumer<Location?>`. Se o Android não obtiver uma posição, usar a última conhecida para o clima. Considerar essa posição nas sugestões somente quando estiver dentro da janela de dois minutos. Sem posição disponível, conservar a previsão em cache se houver.
- Revisar `README.md`, `CONTRIBUTING.md` e `docs/contextual-suggestions.md`. Documentar o comportamento de clima e sugestões.

## Verificação

- [x] Cobrir validade, compartilhamento, retorno nulo, uso da última posição e descarte quando a localização ficar indisponível com testes unitários.
- [x] Executar `./gradlew :app:testDebugUnitTest :app:assembleDebug`. A compilação passou com 86 testes.
- [x] Verificar no emulador Android que clima automático com previsão expirada e sugestões compartilham uma consulta, mesmo durante a recriação da tela. O provedor simulado retornou sem posição e o processo permaneceu ativo, sem exceção no `logcat`.
- [x] Confirmar no emulador que uma previsão válida, a reutilização de uma posição recente e o modo manual evitam as respectivas consultas de localização.
