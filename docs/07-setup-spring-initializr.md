# Gerando o projeto base com o Spring Initializr

1. **Acesse o Spring Initializr** — vá em start.spring.io. É o gerador oficial do time do Spring, não precisa instalar nada.
2. **Escolha Maven e Java** — Project: Maven. Language: Java.
3. **Selecione a versão do Spring Boot** — uma versão 3.5.x (evite as marcadas SNAPSHOT ou M1/RC, são versões de teste).
4. **Preencha Group e Artifact** — Group: `com.carlos`, Artifact: `financasdescomplicadas`. Define o pacote base: `com.carlos.financasdescomplicadas`.
5. **Defina Java 17** — baseline mínima do Spring Boot 3.x, LTS mais usada no mercado hoje.
6. **Adicione as dependências** — Spring Web, Spring Data JPA, Spring Security, Validation, PostgreSQL Driver, Lombok.
7. **Gere e baixe o projeto** — clique em "Generate", extraia o .zip e abra na IDE.
8. **Complete o pom.xml e copie os arquivos** — o jjwt não está no Initializr (não é lib oficial do Spring), então adicione as três dependências do jjwt manualmente (ver `docs/05-seguranca-autenticacao.md`). Depois copie o conteúdo de `backend/src/main/java/com/carlos/financasdescomplicadas/` deste projeto pra dentro da estrutura gerada.
