# Meu Financeiro (v2)

Gestão financeira pessoal para Android, offline. Arquitetura aprovada no documento "Gestão Financeira Pessoal — Arquitetura".

## Módulos
| Módulo | Tipo | Conteúdo |
|---|---|---|
| `:app` | Android application | Interface Compose (Material 3, tema escuro), navegação, injeção manual |
| `:core:domain` | Kotlin puro (JVM) | Motor financeiro e tipos de valor (`Dinheiro`, `Competencia`), testados com JUnit |
| `:core:data` | Android library | Room: entidades, DAOs, migrações; esquemas em `core/data/schemas` |

Dependências: `app → data → domain`, `app → domain`. Versões em `gradle/libs.versions.toml`.

## Build
Cada push no `main` roda no GitHub Actions: testes (`testDebugUnitTest`, que inclui `:core:domain:test`) e APK em Releases.

## Andamento
- [x] Etapa 1: estrutura do projeto
- [x] Etapa 2: banco de dados e modelos
- [x] Etapa 3: motor financeiro
- [ ] Etapa 4: interface
- [ ] Etapa 5: backup e segurança
- [ ] Etapa 6: validação final
