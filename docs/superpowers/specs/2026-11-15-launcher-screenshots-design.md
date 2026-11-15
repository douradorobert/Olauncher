# Capturas reais do launcher — desenho

## Intenção

Adicionar imagens reais do Olauncher em funcionamento para que o README e a documentação expliquem visualmente os principais recursos deste fork. As capturas devem ser produzidas no emulador a partir do aplicativo real, com dados fictícios quando necessário, sem expor informações pessoais relevantes.

## Objetivos

- Mostrar a tela inicial com sugestões contextuais, próximo evento e clima atual visíveis ao mesmo tempo.
- Mostrar as configurações de sugestões, incluindo limiar mínimo e perfis de dias úteis/fim de semana.
- Mostrar o editor de uma regra contextual preenchida de forma compreensível.
- Mostrar o diálogo informativo da pontuação ou das regras.
- Mostrar a seção de backup e restauração.
- Armazenar as imagens em `docs/images/` e reutilizá-las no README e em `docs/contextual-suggestions.md` conforme o assunto.
- Permitir que o usuário confira o estado visual do launcher antes de qualquer captura definitiva.

## Não objetivos

- Criar mockups, ilustrações ou imagens geradas por IA.
- Alterar o código do produto apenas para facilitar as capturas.
- Documentar toda tela ou configuração existente.
- Publicar dados pessoais, tokens, identificadores do dispositivo ou arquivos de backup.

## Restrições

- Usar o AVD `still_api37`, autorizado pelo usuário para limpeza dos dados quando necessário, e o APK de debug atual de `develop`.
- Preferir dados fictícios neutros para agenda e regras; dados reais que apareçam naturalmente no aplicativo são aceitáveis quando não forem sensíveis.
- Não capturar até que o usuário confirme que o launcher aberto e visível está adequado.
- Preservar o arquivo local não versionado `AGENTS.md` e mudanças não relacionadas.
- Imagens devem manter proporção de telefone, resolução suficiente para leitura no GitHub e nomes estáveis em inglês.
- Não apagar, redefinir ou adicionar contas a um AVD sem autorização específica do usuário.

## Abordagens consideradas

### 1. Preparação manual pela interface

Configurar todos os estados tocando apenas na interface do emulador. Produz um fluxo completamente orgânico, porém é lento e torna difícil reproduzir histórico, agenda e clima simultaneamente.

### 2. Preparação controlada por ADB — escolhida

Instalar e abrir o aplicativo normalmente, usando ADB para preparar dados locais fictícios e estados reproduzíveis quando necessário. Toda imagem continua mostrando a interface real do launcher. Essa abordagem reduz tempo, evita depender de contas pessoais e permite repetir as cenas.

Mockups foram descartados porque poderiam divergir do produto efetivamente entregue.

## Conjunto visual

1. `home-contextual.png`: tela inicial com relógio/data, evento fictício, clima e sugestões contextuais.
2. `suggestion-settings.png`: seção de sugestões habilitada, com aplicativos sugeridos, máximo de itens, limiar e separação de perfis visíveis.
3. `contextual-rule.png`: editor de regra preenchido com uma combinação clara de horário, dia ou contexto.
4. `scoring-info.png`: diálogo informativo que explica pontuação e bônus, ou, se ele ficar pouco legível na escala do README, a explicação de regras.
5. `backup-restore.png`: seção de backup e restauração mostrando as ações de exportar e importar.

O README recebe uma galeria compacta com as imagens mais representativas. `docs/contextual-suggestions.md` recebe as imagens relacionadas a sugestões, regra e pontuação perto das seções correspondentes. A imagem de backup pode ficar no README ou em uma subseção própria, sem criar documentação artificial apenas para acomodá-la.

## Preparação e revisão

O `still_api37`, designado pelo usuário como seguro para limpeza e preparação, será iniciado em uma janela visível. O APK será instalado, o Olauncher definido como launcher do emulador e os estados necessários serão preparados.

O agente navegará pelas cinco cenas preparadas na janela visível, uma por vez, sem capturá-las. Antes de executar `adb exec-out screencap` ou qualquer captura equivalente, avisará o usuário e aguardará uma confirmação explícita que cubra o conjunto completo. Qualquer ajuste visual posterior invalida essa aprovação e exige uma nova conferência das cenas afetadas.

Se o usuário pedir ajustes, eles serão feitos no emulador e uma nova conferência ocorrerá antes das capturas. Depois da aprovação, cada tela será navegada e capturada individualmente, sem incluir barras ou janelas do computador fora do dispositivo.

## Preparação segura dos dados

- Parar o aplicativo antes de preparar preferências locais e usar apenas `run-as` no pacote de debug.
- Não imprimir, copiar nem exportar preferências, calendários ou identificadores existentes para os logs.
- Conceder somente as permissões necessárias; para agenda, apenas leitura de calendário.
- Preparar clima pelo cache local do aplicativo, com valores fictícios e timestamp recente, evitando dependência de localização ou rede durante a sessão.
- Preparar sugestões com aplicativos neutros já instalados e uma regra correspondente ou histórico no bucket atual.
- Para o evento fictício, tentar primeiro um calendário local já disponível, usando a interface visível ou automação ADB. Se o AVD não tiver calendário gravável, parar e consultar o usuário; não adicionar conta, instalar aplicativo auxiliar, obter root nem alterar o produto.
- Não concluir exportação/importação durante a cena de backup. A captura deve mostrar somente as ações no app, sem abrir um seletor que revele nomes de arquivos ou contas.

Antes da aprovação e novamente nos PNGs finais, conferir que não aparecem e-mail ou conta, evento/local real, SSID/BSSID, dispositivo Bluetooth, notificações, identificadores, nome de backup, geolocalização ou elementos da área de trabalho do computador.

## Tratamento das imagens

- Preservar a resolução original do emulador; usar largura HTML no Markdown para controlar exibição.
- Não aplicar filtros que alterem a aparência do aplicativo.
- Recortar somente se houver área externa ao display; manter barras do Android quando fizerem parte da tela do dispositivo.
- Usar texto alternativo descritivo em todas as referências Markdown/HTML.
- Conferir visualmente os PNGs finais antes de versioná-los.
- Incluir ou excluir barras de status/navegação de forma consistente em todas as imagens.
- Reabrir e conferir visualmente cada cena imediatamente antes da captura, pois a navegação pode alterar o estado preparado.

## Verificação

- Confirmar que os cinco arquivos são PNGs válidos e têm dimensões consistentes.
- Renderizar ou inspecionar o README e a documentação para verificar caminhos, escala e legibilidade.
- Confirmar que nenhuma captura revela dados sensíveis.
- Executar `git diff --check` e revisar apenas as mudanças de documentação/imagens.
