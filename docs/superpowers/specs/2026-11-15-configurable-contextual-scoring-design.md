# Pontuação contextual configurável — desenho

## Intenção

Tornar o sistema de sugestões compreensível e ajustável, permitindo escolher a pontuação mínima para exibição e, opcionalmente, aprender rotinas separadas para dias úteis e fins de semana.

## Comportamento atual a preservar

Cada abertura elegível registra massa `1,0` para o aplicativo, dia civil local e um dos 48 buckets de meia hora. A contribuição histórica é:

`massa × 10 × pesoTemporal × 0,5^(idadeEmDias / 7)`

O peso temporal é `1,0`, `0,75`, `0,5`, `0,375`, `0,25` e `0,125` para distâncias de zero a cinco buckets; a partir de três horas é zero. Registros permanecem por trinta dias. As contribuições são somadas antes do arredondamento.

A pontuação final soma ao histórico os bônus aplicáveis: regra explícita `+200`, aplicativo de saúde durante evento de saúde `+100`, navegação quando o evento possui destino `+80` e navegação fora de casa `+30`. Candidatos abaixo do limiar são ocultados; os demais são ordenados por pontuação decrescente e nome como desempate.

## Limiar configurável

- Adicionar `contextualMinimumScore`, padrão `20`.
- Aceitar valores de `0` a `200`, sempre em passos de `10`; leituras e gravações inválidas são normalizadas para esse domínio.
- Aplicar o limiar à pontuação final, depois dos bônus.
- Expor a configuração na seção “Sugestões”.
- Abrir um diálogo personalizado com cabeçalho, ícone `ic_info`, controle `− / valor / +`, confirmação e cancelamento.
- O ícone abre uma explicação da fórmula, bônus e efeito dos valores baixo e alto.

## Perfis de dias úteis e fim de semana

- Adicionar `separateContextualDayProfiles`, desativado por padrão.
- Desativado: manter exatamente o cálculo atual, usando todos os registros.
- Ativado: consultas de segunda a sexta usam somente registros de segunda a sexta; consultas de sábado e domingo usam somente registros de sábado e domingo.
- Derivar o perfil diretamente de `epochDay`; não armazenar informação duplicada nem alterar o JSON v3.
- Alternar a opção nunca apaga, regrava ou duplica o histórico. Ao desativar, todos os registros voltam a contribuir.
- Continuar calculando idade real em dias; por exemplo, um registro do fim de semana anterior mantém idade de sete dias e recebe o decaimento correspondente.

`epochDay` zero representa quinta-feira, 1º de janeiro de 1970. A classificação deve usar aritmética modular segura também para valores negativos, evitando criar um `Calendar` para cada entrada.

## Persistência e backup

As duas novas opções são preferências comuns e entram automaticamente no backup. `CONTEXTUAL_HISTORY` permanece local e excluído do backup, como hoje. Não haverá migração nem incremento da versão do codec.

Registros legados já migrados receberam o dia civil da migração; enquanto permanecerem na janela de trinta dias, serão classificados conforme esse dia.

## Interface

Na seção “Sugestões”:

- “Pontuação mínima” mostra o valor atual e abre o diálogo descrito acima.
- “Separar dias úteis e fim de semana” mostra ligado/desligado e alterna com um toque.
- Ambas seguem o estado habilitado/desabilitado da opção principal de sugestões contextuais.

O diálogo de pontuação reutiliza o padrão visual do editor de regras: título/descrição à esquerda e `ImageView` de `48dp` alinhado à direita, com descrição de acessibilidade e diálogo informativo separado.

## Documentação permanente

- O `README.md` ganhará um resumo das sugestões contextuais, das duas configurações e um link para a documentação detalhada.
- Criar `docs/contextual-suggestions.md` como referência permanente da fórmula, pesos, bônus, limiar, perfis, retenção, ordenação e exemplos.
- Os documentos em `docs/superpowers/` registram decisões e execução, mas não substituem essa referência.

## Abordagens consideradas

1. **Derivar o perfil de `epochDay` durante o cálculo — escolhida.** Mantém o formato v3 e tem custo desprezível no histórico limitado a trinta dias.
2. **Persistir `weekday/weekend` em cada entrada.** Rejeitada por duplicar informação, exigir versão 4 e criar risco de inconsistência sem ganho perceptível.
3. **Sete perfis independentes.** Rejeitada por produzir somente quatro ou cinco amostras comparáveis por dia da semana dentro da retenção atual.

## Verificação

- Testar classificação de quinta, sábado, domingo e segunda, inclusive `epochDay` negativo.
- Testar compatibilidade exata com a toggle desativada e isolamento em ambos os perfis quando ativada.
- Testar normalização e fronteiras do limiar (`0`, `20`, `200`) e aplicação depois dos bônus.
- Testar que alternar perfis não altera o JSON serializado.
- Testar os recursos e a estrutura do diálogo nas duas variantes de layout de configurações.
- Executar testes unitários, compilação de recursos, `assembleDebug` e lint, relatando separadamente falhas preexistentes.
