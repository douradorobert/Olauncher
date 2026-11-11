# Remoção de divulgação do projeto original — desenho

## Intenção

Manter o nome **Olauncher** porque este fork ainda é essencialmente o projeto original, mas retirar do aplicativo a divulgação, os links visíveis e as chamadas comerciais do projeto upstream. A autoria original continuará reconhecida no repositório, sem apresentar este fork como distribuição oficial.

## Objetivos

- Remover da interface links para páginas, redes sociais, repositório e lojas do desenvolvedor original.
- Remover recomendações do Pro Launcher, Note to Self, Pentastic e outros produtos do upstream.
- Remover pedidos automáticos ou manuais de avaliação e compartilhamento associados à publicação oficial.
- Manter o funcionamento atual, inclusive serviços remotos necessários ao wallpaper diário.
- Reescrever o README para descrever este fork, suas diferenças e sua relação com o Olauncher original.
- Preservar a licença GPLv3 e dar crédito claro ao autor e ao projeto de origem na documentação.

## Não objetivos

- Renomear o aplicativo, alterar o package name ou criar uma nova identidade visual.
- Substituir a fonte remota dos wallpapers ou outras integrações que funcionam sem links promocionais visíveis.
- Reescrever toda a documentação técnica ou todas as traduções.
- Remover atribuições legais, histórico Git ou autoria existente.

## Restrições e cuidados

- Há alterações locais ainda não commitadas em `SettingsFragment.kt`, `strings.xml` e outros arquivos; a implementação deve preservá-las.
- Strings sem uso poderão permanecer em traduções antigas quando removê-las de todas as localidades trouxer risco desnecessário. As strings-base e referências executáveis removidas devem ficar limpas.
- O README deve distinguir claramente o fork `douradorobert/Olauncher` do projeto oficial `tanujnotes/Olauncher`.
- A licença `LICENSE` permanece intacta. O crédito documental não será movido para dentro do aplicativo.

## Abordagens consideradas

1. **Remoção cirúrgica (escolhida):** retirar apenas elementos visíveis e fluxos promocionais, mantendo dependências funcionais silenciosas. Menor risco e escopo alinhado ao pedido.
2. **Conteúdo informativo local:** substituir páginas externas por ajuda e privacidade dentro do app. Aumentaria o escopo e criaria conteúdo adicional para manter.
3. **Independência completa:** substituir também serviços hospedados pelo upstream. Não é necessário neste momento e elevaria significativamente o risco.

## Mudanças previstas

### Aplicativo

- Remover de Configurações os itens “Sobre e perguntas frequentes” quando forem apenas um link ao site upstream e “Mais recursos” quando promoverem o Pro Launcher.
- Remover do rodapé de Configurações “Privacidade”, “Compartilhar”, “Avaliar”, “GitHub”, “X/Twitter” e a recomendação alternada de outros aplicativos.
- Remover listeners e branches correspondentes de `SettingsFragment`.
- Desativar a sequência automática de mensagens que solicita avaliação e compartilhamento; preservar mensagens estritamente funcionais, como wallpaper, teclado e permissões.
- Remover o diálogo promocional do Pro Launcher e constantes de URLs que ficarem sem uso.
- Manter URLs internas usadas pelo wallpaper diário e por funcionalidades técnicas que não aparecem como divulgação na interface.
- Ajustar textos funcionais que ainda mandem o usuário para uma página “Sobre” removida.

### Repositório e documentação

- Reescrever o README em português para apresentar este repositório como fork independente mantido por Robert Dourado.
- Incluir uma seção de créditos com link para o Olauncher original e para Tanuj, explicando que a base e a maior parte do trabalho original são deles.
- Manter a GPLv3 e destacar que esta versão contém modificações próprias.
- Remover badges e instruções que direcionam para as distribuições oficiais, bem como a seção de outros aplicativos do time Olauncher.
- Apontar clonagem, releases e acompanhamento para `douradorobert/Olauncher` quando aplicável.

## Verificação

- Compilar recursos e código Kotlin para detectar IDs, imports, strings ou constantes órfãs.
- Executar os testes unitários existentes.
- Pesquisar referências visíveis restantes às URLs, produtos e redes sociais do upstream.
- Revisar o diff para confirmar que as alterações locais pré-existentes não foram sobrescritas.

