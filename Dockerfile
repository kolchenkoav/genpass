# Общий образ: linux/amd64 + linux/arm64 (PLAN.md 8.1)
# Java 21 (решение пользователя) → temurin-21 builder/runtime (план: 17, актуализировано)

# ---------- Stage 1: build ----------
# сборка всегда нативно на хосте сборки ($BUILDPLATFORM), fat-jar платформо-независим
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
# кэш зависимостей отдельным слоем
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
# тесты выполняются здесь
RUN mvn -B clean package

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:21-jre
RUN groupadd -r app && useradd -r -g app -d /app app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/genpass.jar /app/app.jar
# non-root обязательно
USER app
EXPOSE 8080
ENV PORT=8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD ["java", "-cp", "/app/app.jar", "org.example.genpass.web.HealthCheck"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
