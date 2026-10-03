# Segurança e autenticação

## Decisão de produto

Sem tela de cadastro público — login direto com credenciais criadas manualmente no banco (ou via um endpoint admin simples, ainda não implementado). Isso simplifica o Spring Security: sem fluxo de verificação de email, sem recuperação de senha pública.

## Fluxo de login

1. Cliente envia `POST /auth/login` com email e senha
2. `AuthController` repassa pro `AuthenticationManager` do Spring Security
3. O `AuthenticationManager` chama `UserDetailsServiceImpl.loadUserByUsername(email)` pra buscar o usuário no banco
4. Compara a senha enviada com o hash salvo, usando `BCryptPasswordEncoder`
5. Se bate, a autenticação passa; `JwtTokenProvider` gera o token usando o **id do usuário** (não o email) como subject
6. `AuthController` devolve o token pro cliente
7. O cliente guarda o token e manda em toda requisição seguinte no header `Authorization: Bearer <token>`
8. `JwtAuthFilter` intercepta cada requisição, valida o token, recarrega o usuário (agora pelo id) e popula o `SecurityContext`

## Dependências (pom.xml)

Além dos starters padrão (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`, `spring-boot-starter-validation`) e do driver PostgreSQL, adicionar manualmente (não vêm pelo Spring Initializr):

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.13.0</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
```

## application.properties

```properties
jwt.secret=uma-chave-bem-longa-e-aleatoria-de-pelo-menos-32-caracteres
jwt.validade-ms=86400000
```

(86400000 ms = 24 horas de validade do token)

## Classes já implementadas

Em `backend/src/main/java/com/carlos/financasdescomplicadas/`:

| Arquivo | Papel |
|---|---|
| `security/JwtTokenProvider.java` | Gera e valida o token JWT |
| `security/JwtAuthFilter.java` | Intercepta requisições, valida o token, popula o `SecurityContext` |
| `security/UserDetailsServiceImpl.java` | Busca o usuário no banco (por email no login, por id nas requisições seguintes) |
| `config/SecurityConfig.java` | Filter chain stateless, libera `/auth/**`, exige token no resto, define `PasswordEncoder` e `AuthenticationManager` |
| `controller/AuthController.java` | Endpoint `POST /auth/login` |
| `dto/LoginRequestDTO.java` | Request do login (email, senha) |
| `dto/LoginResponseDTO.java` | Response do login (token, nome, email) |

## Complementos implementados

- `config/CorsConfig.java` — libera as origens de `app.cors.origens` (variável `CORS_ORIGENS`)
- `SecurityConfig` passou a usar o CORS, liberar `/error` e o preflight `OPTIONS`, e responder **401 em JSON** quando falta o token ou ele é inválido
- `JwtAuthFilter` não quebra (500) quando o token é de um usuário que não existe mais — só não autentica
- `GlobalExceptionHandler` responde login inválido com 401 `"Email ou senha inválidos"`
- `security/UsuarioAutenticado.java` — entrega aos controllers o id do usuário logado
- `config/UsuarioInicialConfig.java` — cria a conta inicial na subida a partir de `app.usuario-inicial.*`, já com a senha em BCrypt
- Classes `model/Usuario.java` e `repository/UsuarioRepository.java`
