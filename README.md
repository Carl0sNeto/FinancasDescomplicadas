# Finanças Descomplicadas

Site de organização financeira pessoal: receitas e despesas por categoria, metas, e importação
automática de extrato bancário (OFX/CSV) com categorização que aprende com as suas correções.

Projeto de portfólio com foco em backend Java/Spring Boot.

## Stack

- **Backend**: Java 17+ · Spring Boot 4.0 (API REST) · Spring Security + JWT (jjwt) · Spring Data JPA
- **Banco**: PostgreSQL (produção) · H2 (perfil `dev` e testes)
- **Frontend**: HTML, CSS e JavaScript puro, na pasta [`frontend/`](frontend/)
- **Testes**: JUnit 5, Mockito, MockMvc — `mvnw test`

## Como rodar localmente

Pré-requisito: **JDK 17 ou mais novo** (`java -version`). Não precisa instalar PostgreSQL pra testar.

**1. Backend** (perfil `dev`, banco H2 salvo em `./data`):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

A API sobe em `http://localhost:8080` e cria uma conta de demonstração com categorias padrão —
o email e a senha estão em [`application-dev.properties`](src/main/resources/application-dev.properties).

**2. Frontend** — sirva a pasta `frontend/` por HTTP (abrir o `.html` direto pelo explorador não funciona por causa do CORS):

- VS Code: extensão **Live Server** → botão direito em `frontend/index.html` → *Open with Live Server* (porta 5500), ou
- Pelo JDK: `jwebserver -p 8000 -d "<caminho absoluto>/frontend"` e abra `http://localhost:8000`

Extratos de exemplo para testar a importação: [`docs/exemplos/`](docs/exemplos/).

## Rodando com PostgreSQL

Sem o perfil `dev`, a aplicação usa PostgreSQL e lê a configuração de variáveis de ambiente:

| Variável | Padrão | Descrição |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/financas` | URL JDBC do banco |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | Credenciais do banco |
| `JWT_SECRET` | — (**obrigatória**) | Chave do JWT, mínimo de 32 caracteres |
| `JWT_VALIDADE_MS` | `86400000` (24 h) | Validade do token |
| `CORS_ORIGENS` | `http://localhost:5500,...` | Origens do frontend liberadas, separadas por vírgula |
| `USUARIO_INICIAL_NOME` / `_EMAIL` / `_SENHA` | vazio | Conta **administradora**: criada na subida se não existir (se já existir, é promovida a admin) |
| `PORT` | `8080` | Porta HTTP (o Render define sozinho) |
| `DB_POOL_SIZE` | `5` | Máximo de conexões com o banco |

Não existe cadastro público. A conta de `USUARIO_INICIAL_*` é a administradora (ver
[`UsuarioInicialConfig`](src/main/java/com/carlos/financasdescomplicadas/config/UsuarioInicialConfig.java)):
ela vê o menu **Administração** no site, onde cria as contas das outras pessoas, troca senhas e exclui contas.

## API

Todas as rotas, exceto `/auth/login`, exigem o header `Authorization: Bearer <token>`.
Erros voltam como JSON `{status, erro, mensagem, campos}`.

| Método | Rota | Descrição |
|---|---|---|
| POST | `/auth/login` | Login (`{email, senha}`) → token JWT |
| GET | `/transacoes?inicio=&fim=&categoriaId=` | Lista do período (padrão: mês atual) |
| GET | `/transacoes?pendentes=true` | Transações sem categoria |
| GET | `/transacoes/resumo?inicio=&fim=` | Receitas, despesas, saldo e despesas por categoria |
| POST / PUT / DELETE | `/transacoes`, `/transacoes/{id}` | CRUD (valor positivo = receita, negativo = despesa) |
| PATCH | `/transacoes/{id}/categoria` | Categoriza e (por padrão) cria uma regra de categorização |
| GET / POST / PUT / DELETE | `/categorias`, `/categorias/{id}` | CRUD de categorias |
| GET / POST / PUT / DELETE | `/metas`, `/metas/{id}` | CRUD de metas |
| POST | `/metas/{id}/movimentos` | Guarda (`valor` > 0) ou retira (`valor` < 0) dinheiro da meta |
| POST | `/importacoes` | Upload multipart (campo `arquivo`, `.ofx` ou `.csv`) |
| GET / DELETE | `/importacoes`, `/importacoes/{id}` | Histórico / desfazer importação |
| GET / DELETE | `/regras`, `/regras/{id}` | Regras de categorização aprendidas |
| GET / POST | `/admin/usuarios` | **Só ADMIN**: lista / cria contas (`{nome, email, senha, papel}`) |
| PUT | `/admin/usuarios/{id}/senha` | **Só ADMIN**: troca a senha de uma conta |
| DELETE | `/admin/usuarios/{id}` | **Só ADMIN**: exclui a conta e todos os dados dela |

## Documentação

| Arquivo | Conteúdo |
|---|---|
| [`docs/02-arquitetura.md`](docs/02-arquitetura.md) | Arquitetura e decisões de deploy |
| [`docs/03-modelo-de-dados.md`](docs/03-modelo-de-dados.md) | Entidades e decisões de modelagem |
| [`docs/04-importacao-automatica.md`](docs/04-importacao-automatica.md) | Parsers de extrato e motor de categorização |
| [`docs/05-seguranca-autenticacao.md`](docs/05-seguranca-autenticacao.md) | Fluxo de login e JWT |
| [`docs/06-estrutura-pastas.md`](docs/06-estrutura-pastas.md) | Organização de pacotes |
| [`docs/08-proximos-passos.md`](docs/08-proximos-passos.md) | O que foi feito e o que falta |
| [`docs/09-deploy-render-neon.md`](docs/09-deploy-render-neon.md) | Passo a passo do deploy no Render + Neon |
