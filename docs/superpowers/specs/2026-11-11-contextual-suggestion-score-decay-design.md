# Decaimento da pontuação de sugestões contextuais

## Intenção

Fazer as sugestões refletirem hábitos recentes, impedindo que uma única abertura ou um uso antigo mantenha um aplicativo elegível indefinidamente.

## Objetivos

- Exigir pelo menos 20 pontos para uma sugestão baseada somente no histórico de uso.
- Manter cada abertura recente com valor-base de 10 pontos.
- Reduzir pela metade, a cada 7 dias, a contribuição de cada agregado diário.
- Descartar definitivamente agregados com mais de 30 dias.
- Preservar as regras explícitas: uma regra válida continua recebendo bônus de 200 pontos e não depende de histórico de uso.
- Manter a aprendizagem separada nas seis faixas atuais de quatro horas.
- Migrar o histórico existente sem apagar imediatamente o aprendizado acumulado.

## Não objetivos

- Alterar os bônus de calendário, localização, navegação ou regras explícitas.
- Alterar a seleção de candidatos, a quantidade de espaços disponíveis ou o desempate alfabético.
- Registrar cada abertura individualmente.
- Adicionar configurações de decaimento à interface nesta primeira versão.

## Restrições e comportamento

O histórico passará de um contador acumulado por aplicativo e faixa horária para agregados diários. Cada registro identificará aplicativo, faixa de quatro horas, data local e quantidade de aberturas.

Ao calcular a pontuação, cada agregado contribuirá com `aberturas × 10 × 0,5^(idadeEmDias / 7)`. O resultado do histórico será arredondado para um inteiro somente depois de somar todas as contribuições válidas. Registros cuja idade seja maior que 30 dias serão ignorados e removidos durante leituras ou gravações posteriores.

Uma sugestão sem regra explícita precisará atingir no mínimo 20 pontos após o decaimento e antes dos bônus contextuais existentes. Os bônus de calendário e localização continuam participando da pontuação final como hoje; portanto, um aplicativo relevante para um evento ainda poderá aparecer mesmo sem duas aberturas recentes.

Datas serão calculadas no fuso local do aparelho. Mudanças de relógio ou fuso não poderão gerar idade negativa; registros com data futura contribuirão como se tivessem idade zero até a data ser alcançada.

## Migração

O formato novo terá uma versão própria. Na primeira leitura do formato antigo, cada contador acumulado será convertido em um agregado datado do dia da migração, preservando o aplicativo e a faixa de quatro horas. Isso conserva temporariamente o aprendizado atual, que então sofrerá o mesmo decaimento e expirará em 30 dias.

A migração deve ser idempotente: depois que o formato novo for salvo, leituras seguintes não poderão duplicar os contadores antigos. Dados inválidos devem ser ignorados sem impedir o carregamento dos demais registros.

## Abordagens consideradas

1. **Agregados diários por faixa horária — escolhida.** Permite decaimento e expiração precisos com armazenamento limitado e sem registrar cada abertura.
2. **Lista de aberturas individuais.** É mais precisa, mas aumenta desnecessariamente o volume e o custo de limpeza.
3. **Contador único com última atualização.** É menor, mas não consegue expirar contribuições antigas separadamente quando o aplicativo continua sendo usado.

## Critérios de aceitação

- Uma única abertura recente gera 10 pontos e, sozinha, não torna o aplicativo elegível.
- Duas aberturas no mesmo dia geram 20 pontos e tornam o aplicativo elegível, na ausência de outros filtros.
- Após 7 dias, a contribuição dessas duas aberturas é aproximadamente 10 pontos.
- Registros com mais de 30 dias não contribuem e são removidos do histórico persistido.
- Uso em uma faixa de quatro horas não aumenta a pontuação das demais faixas.
- Uma regra válida continua recomendando o aplicativo mesmo com histórico zerado.
- A migração conserva os contadores existentes uma única vez e eles passam a decair normalmente.

