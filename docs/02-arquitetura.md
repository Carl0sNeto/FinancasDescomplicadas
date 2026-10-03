# Arquitetura

## Visão geral do fluxo

```
Frontend (HTML/CSS/JS)
        |  HTTP/JSON
        v
Backend (Spring Boot)
  Controller -> Service -> Repository
        |  JPA/Hibernate
        v
PostgreSQL
```

## Camadas do backend

- **Controller**: recebe as requisições HTTP, valida entrada (`@Valid`), delega pro Service. Não tem lógica de negócio.
- **Service**: contém as regras de negócio (ex: calcular progresso de uma meta, decidir se uma transação bate com uma regra de categorização).
- **Repository**: interfaces Spring Data JPA, acesso direto ao banco.

## Deploy desacoplado

Backend e frontend rodam separados, como em arquitetura real de mercado:

- **Backend**: Render ou Railway (tier gratuito com suporte a Java/Spring Boot) + PostgreSQL gerenciado (Render ou Supabase)
- **Frontend**: Vercel, Netlify ou GitHub Pages (hospedagem estática, deploy automático a cada push)

Como backend e frontend ficam em domínios diferentes, é necessário CORS configurado no backend liberando a origem do frontend (ver `docs/08-proximos-passos.md` — `CorsConfig` ainda não foi criado).

## Organização do repositório

Monorepo: o projeto Spring Boot fica na raiz (como gerado pelo Initializr) e o frontend numa pasta própria:

```
financasdescomplicadas/
├── pom.xml, src/   (backend Spring Boot)
├── frontend/       (HTML/CSS/JS)
└── docs/
```

Motivo de ser monorepo em vez de dois repositórios: sendo um projeto mantido por uma única pessoa, fica mais simples de gerenciar sem perder a separação de responsabilidades entre as duas partes.
