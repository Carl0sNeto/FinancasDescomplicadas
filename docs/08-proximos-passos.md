# Próximos passos

## Feito

- [x] Arquitetura geral definida (`docs/02-arquitetura.md`)
- [x] Modelo de dados / ERD definido (`docs/03-modelo-de-dados.md`)
- [x] Design da importação automática definido (`docs/04-importacao-automatica.md`)
- [x] Fluxo de login/JWT — `security/`, `controller/AuthController.java`, `dto/`, `config/SecurityConfig.java`
- [x] Projeto base gerado pelo Spring Initializr (Spring Boot 4.0.8) + dependências do jjwt
- [x] Entidades `model/` e repositórios `repository/`
- [x] `config/CorsConfig.java` — origens do frontend via `app.cors.origens` / `CORS_ORIGENS`
- [x] CRUD de Transação, Categoria e Meta (controllers + services), resumo mensal pro dashboard
- [x] Importação automática — `ExtratoParser`, `OfxParser`, `CsvParser` (Nubank cartão e conta),
      `ImportacaoService`, `CategorizacaoService`; desfazer importação; regras aprendidas com as correções
- [x] `exception/GlobalExceptionHandler` com respostas de erro padronizadas
- [x] `application.properties` (PostgreSQL via variáveis de ambiente) + perfil `dev` com H2
- [x] Conta inicial criada na subida (`config/UsuarioInicialConfig.java`)
- [x] Frontend em `frontend/`: login, painel, transações, categorias, metas e importação
- [x] Testes: unitários (parsers, categorização, metas) e de integração do fluxo principal (MockMvc + H2)

## Decisões que divergem das orientações originais

- **Spring Boot 4.0.8** em vez de 3.5.x — foi a versão gerada no Initializr; tudo funciona nela.
  Diferença prática: anotações de teste mudaram de pacote (ex.: `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`).
- **OFX lido sem a ofx4j** — leitura direta dos blocos `<STMTTRN>`, mais tolerante com arquivos de
  bancos brasileiros que fogem da especificação, e uma dependência a menos.
- **Backend na raiz do repositório** e frontend em `frontend/` (em vez de `backend/` + `frontend/`):
  mantém o projeto gerado pelo Initializr como está. No Render, basta apontar o *root directory* pra raiz.
- **Categorizar com "lembrar"** também aplica a regra nova nas outras transações pendentes,
  não só nas próximas importações.

## Pendente — sugestões

1. **Deploy** — `Dockerfile` e `render.yaml` prontos; passo a passo em `docs/09-deploy-render-neon.md`
2. **Migrações com Flyway** no lugar de `ddl-auto=update` — mais seguro pra evoluir o banco em produção
3. **Detecção de duplicadas na importação** — hoje importar o mesmo extrato duas vezes duplica as
   transações (dá pra desfazer pela tela de importação). Uma opção é guardar o `FITID` do OFX
4. **Paginação** em `GET /transacoes` quando o volume crescer
5. ~~**Endpoint admin** pra criar contas~~ — feito: papel `ADMIN`, rotas `/admin/usuarios` e página `admin.html`
6. **Gráfico de evolução mensal** no painel (receitas x despesas dos últimos meses)
