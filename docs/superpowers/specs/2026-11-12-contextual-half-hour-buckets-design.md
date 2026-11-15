# Pontuação contextual em buckets de 30 minutos — desenho

## Intenção

Aumentar a precisão temporal das sugestões aprendidas sem fazer uma abertura valer apenas para um intervalo rígido. Cada abertura será registrada em um dos 48 intervalos de 30 minutos do dia e contribuirá também para horários próximos por meio de uma janela suave de três horas.

## Objetivos

- Registrar aberturas em buckets de 30 minutos, numerados de `0` a `47`.
- Dar peso máximo ao bucket da abertura e pesos progressivamente menores aos buckets vizinhos.
- Tratar o início e o fim do dia como adjacentes: `23h30–0h` fica ao lado de `0h–0h30`.
- Manter o decaimento histórico atual de meia-vida de sete dias e retenção máxima de trinta dias.
- Migrar automaticamente o histórico atual de buckets de quatro horas sem inventar um horário preciso.
- Preservar a pontuação base de dez pontos para uma abertura recente consultada no mesmo bucket.

## Não objetivos

- Alterar o limite mínimo de vinte pontos usado para exibir sugestões aprendidas.
- Alterar regras explícitas, bônus de calendário, casa, direção ou navegação.
- Registrar o minuto exato de cada abertura individual.
- Aumentar a retenção além de trinta dias.
- Manter equivalência exata entre a pontuação antiga e a nova após a migração; o histórico antigo é temporalmente impreciso e deve ter confiança diluída.

## Modelo temporal

`timeBucket(hour, minute)` retorna `hour * 2 + minute / 30`, com hora limitada a `0..23` e minuto limitado a `0..59`.

A distância entre dois buckets usa o menor caminho em um relógio de 48 posições:

`min(abs(a - b), 48 - abs(a - b))`.

O peso temporal por distância de bucket será:

| Distância | Diferença nominal | Peso |
|---:|---:|---:|
| 0 | mesmo intervalo | 1,0 |
| 1 | 30 minutos | 0,75 |
| 2 | 1 hora | 0,5 |
| 3 | 1h30 | 0,375 |
| 4 | 2 horas | 0,25 |
| 5 | 2h30 | 0,125 |
| 6 ou mais | 3 horas ou mais | 0 |

Uma abertura às 10h20 pertence ao bucket `10h–10h30`. Ela vale `1,0` nesse bucket, `0,75` entre 9h30–10h e 10h30–11h, e continua decaindo simetricamente até zerar a partir de três horas.

## Pontuação

Cada entrada persistida terá uma `launchMass` decimal em vez de uma contagem inteira. Uma nova abertura acrescenta `1,0` à massa do app, bucket e dia correspondentes.

A contribuição de cada entrada será:

`launchMass × 10 × pesoTemporal × 0,5^(idadeEmDias / 7)`.

As contribuições serão somadas em ponto flutuante e arredondadas apenas uma vez no final. Datas futuras continuam com idade zero. Massa inválida, não finita, nula ou negativa não contribui.

## Persistência e migração

O JSON passará da versão 2 para a versão 3. Entradas v3 usarão os campos `app`, `bucket`, `day` e `mass`.

Ao ler a versão 2 ou o formato legado sem versão:

- cada bucket antigo de quatro horas será convertido nos oito buckets de 30 minutos que cobrem o mesmo período;
- a contagem antiga será dividida igualmente entre eles (`count / 8,0`);
- a soma da massa será preservada, mas a confiança em qualquer horário específico será menor;
- o resultado será marcado para regravação imediata em v3;
- entradas inválidas serão descartadas sem impedir a migração das válidas.

Exemplo: uma entrada antiga de contagem `8` no bucket `8h–12h` vira oito entradas com massa `1,0`, cobrindo de `8h–8h30` até `11h30–12h`.

## Integração

O repositório de uso passará hora e minuto ao registrar e calcular pontuação. Os chamadores obterão ambos do mesmo `Calendar`, evitando divergência na virada de meia hora ou de dia.

`ContextualSuggestions` continuará recebendo um `usageScore` inteiro e não precisará conhecer buckets, pesos ou versões de persistência.

## Abordagens consideradas

1. **Buckets de 30 minutos com janela suave de três horas (escolhida):** oferece precisão sem tornar o aprendizado excessivamente esparso.
2. **Buckets de uma hora com pesos `1 / 0,5 / 0,25`:** mais simples e mais estável, porém distingue menos rotinas que acontecem em metades diferentes da mesma hora.
3. **Timestamp de cada abertura e kernel contínuo:** seria mais preciso, mas aumentaria armazenamento, complexidade e custo de cálculo sem benefício proporcional para um launcher.

## Compatibilidade e limites

- A versão 3 deve aceitar números inteiros ou decimais no campo `mass`, mas rejeitar strings, `NaN`, infinito e valores não positivos.
- A massa acumulada deve saturar em um limite seguro, evitando overflow de pontuação.
- A ordenação serializada permanece estável por app, bucket e dia.
- Buckets inválidos fora de `0..47` nunca afetam score nem são persistidos.

## Verificação

- Testes unitários cobrirão limites de meia hora, distância circular na meia-noite e todos os pesos.
- Testes de score cobrirão combinação do peso temporal com o decaimento por idade e arredondamento único.
- Testes do codec cobrirão round-trip v3, migração v2, migração legada e dados decimais inválidos.
- Testes do repositório confirmarão que hora e minuto selecionam o bucket correto e que uma migração é regravada apenas uma vez.
- A suíte completa e `assembleDebug` validarão integração e recursos Android.
