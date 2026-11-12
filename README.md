# Olauncher

Olauncher é um launcher minimalista para Android, sem ícones na tela inicial, criado para reduzir distrações e manter o acesso aos aplicativos simples e rápido.

Este repositório contém um **fork independente e modificado** do Olauncher. Ele mantém o nome porque ainda compartilha a base e grande parte da experiência do projeto original, mas não é a distribuição oficial e possui decisões e funcionalidades próprias.

## Principais recursos

- Tela inicial minimalista com nomes de aplicativos.
- Gestos configuráveis para abrir aplicativos, notificações e bloqueio de tela.
- Busca e abertura rápida no menu de aplicativos.
- Temas claro e escuro, tamanho, peso e alinhamento de texto configuráveis.
- Ocultação e renomeação de aplicativos.
- Suporte a perfis de trabalho, espaço privado e atalhos fixados.
- Papéis de parede diários opcionais.
- Pausa consciente antes de abrir aplicativos selecionados.
- Sugestões contextuais baseadas em uso, horário e contexto do dispositivo.
- Agenda e clima opcionais na tela inicial.
- Backup e restauração das configurações.

## Diferenças desta versão

Esta versão evolui de forma independente e inclui recursos que podem não existir no projeto original, especialmente sugestões contextuais, controles de bem-estar, integrações informativas na tela inicial e opções adicionais de personalização.

Ela também remove da interface promoções, recomendações de outros aplicativos e links comerciais associados à distribuição original.

## Compilação

Abra o projeto em uma versão atual do Android Studio com o Android SDK instalado ou compile pelo terminal:

```bash
./gradlew assembleDebug
```

O APK de desenvolvimento será gerado em `app/build/outputs/apk/debug/`.

O código, as mudanças e as versões desta distribuição ficam em:

- [Repositório douradorobert/Olauncher](https://github.com/douradorobert/Olauncher)
- [Releases deste fork](https://github.com/douradorobert/Olauncher/releases)

## Créditos e origem

Este projeto é baseado no [Olauncher original](https://github.com/tanujnotes/Olauncher), criado por [Tanuj](https://x.com/tanujnotes). A base do aplicativo e a maior parte do trabalho original pertencem a Tanuj e aos contribuidores do projeto upstream.

As modificações mantidas neste repositório não devem ser atribuídas aos autores do projeto original. Da mesma forma, este fork não pretende substituir nem se apresentar como a versão oficial.

## Licença

O projeto continua distribuído sob a [GNU GPLv3](LICENSE). Consulte o arquivo de licença para conhecer os termos de uso, modificação e redistribuição.
