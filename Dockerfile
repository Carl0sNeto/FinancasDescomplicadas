# Imagem do backend para o Render (que não tem runtime nativo de Java).

# 1) Build: compila e gera o .jar executável
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
# Baixa as dependências numa camada separada, reaproveitada enquanto o pom.xml não muda
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src src
RUN mvn -B -q package -DskipTests

# 2) Execução: só o JRE + o .jar
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
# O plano gratuito do Render tem 512 MB: limita o heap e usa o GC mais econômico
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-jar", "app.jar"]
