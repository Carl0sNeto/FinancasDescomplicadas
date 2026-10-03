# Deploy: Render (API + frontend) + Neon (PostgreSQL)

O Render não tem runtime nativo de Java, por isso a API sobe por **Docker** (`Dockerfile` na raiz).
O `render.yaml` cria os dois serviços de uma vez: `financas-api` (Docker) e `financas-web` (site estático).

## 1. Banco no Neon

1. Crie um projeto em [neon.tech](https://neon.tech) e abra **Connect** (ou *Connection Details*).
2. Desmarque **Connection pooling**: o Spring já tem o próprio pool (Hikari), então use a conexão direta.
3. O Neon mostra uma URL assim:

   ```
   postgresql://neondb_owner:SENHA@ep-nome-123456.sa-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require
   ```

4. O Spring **não aceita** esse formato. Separe em três variáveis:

   | Variável | Valor |
   |---|---|
   | `DB_URL` | `jdbc:postgresql://ep-nome-123456.sa-east-1.aws.neon.tech/neondb?sslmode=require` |
   | `DB_USERNAME` | `neondb_owner` |
   | `DB_PASSWORD` | `SENHA` |

   Ou seja: troque `postgresql://` por `jdbc:postgresql://`, **tire o `usuario:senha@`** da URL e
   deixe só `?sslmode=require` no final.

As tabelas são criadas automaticamente na primeira subida (`ddl-auto=update`).

## 2. Serviços no Render

1. No Render: **New → Blueprint** → conecte o repositório `FinancasDescomplicadas`.
2. O Render lê o `render.yaml` e pede os valores marcados como `sync: false`:

   | Variável | Valor |
   |---|---|
   | `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Da etapa 1 |
   | `CORS_ORIGENS` | URL do frontend, ex.: `https://financas-web.onrender.com` (sem `/` no final) |
   | `USUARIO_INICIAL_NOME` / `_EMAIL` / `_SENHA` | Sua conta de acesso (não existe cadastro público) |

   `JWT_SECRET` é gerado pelo próprio Render.
3. Clique em **Apply**. O primeiro build da API leva alguns minutos.

> Se já tinha criado um *Web Service* manualmente, dá pra aproveitar: em **Settings**, mude o
> **Runtime/Language** para **Docker** e cadastre as mesmas variáveis em **Environment**.

## 3. Ligar o frontend na API

1. Copie a URL que o Render deu ao serviço `financas-api` (ex.: `https://financas-api-xyz1.onrender.com`).
2. Coloque essa URL em `API_PRODUCAO`, no arquivo `frontend/js/config.js`, e faça commit/push.
3. Se a URL do `financas-web` for diferente da que você pôs em `CORS_ORIGENS`, corrija a variável
   na API (**Environment**). O Render reinicia o serviço sozinho.

## Problemas comuns

| Sintoma | Causa |
|---|---|
| Render não oferece Java / build falha procurando `package.json` etc. | Runtime não está como **Docker** |
| `Driver claims to not accept jdbcUrl` / `URL must start with jdbc` | `DB_URL` no formato do Neon, sem `jdbc:` |
| `password authentication failed` | Usuário e senha deixados dentro da `DB_URL`, ou senha errada |
| `Could not resolve placeholder 'JWT_SECRET'` | Variável `JWT_SECRET` ausente |
| Login no site dá "Não foi possível conectar à API" | `API_PRODUCAO` errada em `config.js`, ou API dormindo (veja abaixo) |
| Erro de CORS no console do navegador | `CORS_ORIGENS` diferente da URL exata do frontend |
| Primeira requisição demora ~1 minuto | Normal no plano gratuito: a API "dorme" após 15 min sem uso |
