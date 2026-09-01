# Task 04: M3 — Docker: multi-arch (amd64 + arm64 + arm/v7), healthcheck, non-root

**Type:** Code Modification

## Goal

Создать production-контейнеризацию по решению пользователя: ОДИН универсальный
multi-arch образ сразу под linux/amd64, linux/arm64 и linux/arm/v7. Два multi-stage
Dockerfile (общий temurin-runtime + отдельный armv7-runtime), объединяемые buildx
в единый манифест; плюс офлайн-сценарий per-platform tar.

## What to Do

- **`Dockerfile`** (общий, linux/amd64 + linux/arm64) по разделу 8.1 PLAN.md:
  - Stage `build`: `FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-17`,
    `COPY pom.xml` → `dependency:go-offline` (кэш-слой) → `COPY src` → `mvn -B clean package`
    (тесты выполняются на этом stage; Maven всегда работает нативно на хосте сборки).
  - Stage runtime: `eclipse-temurin:17-jre`, group/user `app`, `USER app`,
    `target/genpass.jar` → `/app/app.jar`, `EXPOSE 8080`, `ENV PORT=8080`,
    `HEALTHCHECK` через `java -cp /app/app.jar org.example.jenpass.web.HealthCheck`,
    `ENTRYPOINT ["java","-jar","/app/app.jar"]`.
- **`Dockerfile.armv7`** (только linux/arm/v7) по разделу 8.2 PLAN.md:
  - Builder идентичен (с `--platform=$BUILDPLATFORM`).
  - Runtime: `debian:12-slim` + `apt-get install -y --no-install-recommends
    openjdk-17-jre-headless` (пакет armhf существует в bookworm — проверено
    packages.debian.org; Temurin armv7-образов JDK 17 не выпускает) + тот же non-root,
    jar, HEALTHCHECK, ENTRYPOINT, что и в общем Dockerfile.
- **`.dockerignore`**: `target/`, `.git/`, `.idea/`, `.tasks/`, `.veai/`, `*.tar`.
- **Локальная проверка** (amd64-хост): `docker buildx build --load -t genpass:local .`,
  запуск с маппингом порта, curl `http://localhost:8080/api/health`,
  `docker inspect` → `Health.Status: healthy`, `docker exec <c> id -un` → `app` (non-root).
  Зафиксировать размеры образов (amd64 и armv7 через `--platform` + build history) в outcome.
- **Multi-arch сборка** по разделу 8.3 PLAN.md:
  ```bash
  docker buildx build --platform linux/amd64,linux/arm64 -t <registry>/genpass:1.0-core --push .
  docker buildx build --platform linux/arm/v7 -f Dockerfile.armv7 -t <registry>/genpass:1.0-armv7 --push .
  docker buildx imagetools create -t <registry>/genpass:1.0 \
    <registry>/genpass:1.0-core <registry>/genpass:1.0-armv7
  ```
  Проверить: `docker buildx imagetools inspect <registry>/genpass:1.0` — в манифесте
  присутствуют linux/amd64, linux/arm64, linux/arm/v7.
- **Офлайн-запасной сценарий** (раздел 9.3 PLAN.md): собрать хотя бы tar своей платформы:
  `docker buildx build --platform linux/amd64 -o type=docker,dest=genpass-1.0-amd64.tar .`
  (и аналогично arm64/armv7 при наличии времени); проверить загрузку `docker load`.
- Если registry недоступен — задокументировать в outcome выполненные команды и результат;
  полный push выполнить при появлении доступа (или согласовать с пользователем
  отказ от registry в пользу tar-сценария).

## Files/Areas

- `Dockerfile` — создать (общий, amd64+arm64)
- `Dockerfile.armv7` — создать (arm/v7: debian:12-slim + openjdk-17-jre-headless)
- `.dockerignore` — создать

## Key Points

- НЕ устанавливать curl/wget в runtime-образы — healthcheck решён Java-классом `HealthCheck`
  (создан в задаче 03; если нет — вернуться к задаче 03).
- `--platform=$BUILDPLATFORM` в builder-стейдже обязателен в ОБОИХ Dockerfile: Maven не должен
  запускаться под QEMU (armv7-варианта maven-образа Temurin вообще нет — без этого флага
  сборка linux/arm/v7 упадёт на первом же FROM).
- Multi-arch manifest-list живёт только в registry; для офлайн-деплоя — per-platform tar
  (`-o type=docker`), импорт на NAS через Container Manager (задача 05).
- Альтернатива Debian для armv7-runtime — `bellsoft/liberica-openjre-debian:17`
  (Dockerfile вендора поддерживает armv[67]l), но тег без суффикса arch может покрывать
  только amd64+arm64 — при желании заменить, сначала проверив `imagetools inspect`.
- HEALTHCHECK: interval 30s / timeout 5s / start-period 10s / retries 3.
- Ожидание: armv7-образ заметно крупнее (openjdk-17-jre-headless ~157 МБ установленного).

## Done When

- [ ] `Dockerfile`, `Dockerfile.armv7`, `.dockerignore` созданы по разделам 8.1–8.2 PLAN.md
- [ ] В обоих Dockerfile builder-стейдж использует `--platform=$BUILDPLATFORM`
- [ ] Локальная сборка и запуск успешны; health=healthy; контейнер работает от non-root
- [ ] Тесты прошли внутри stage сборки (`mvn clean package` в логе образа)
- [ ] Универсальный multi-arch манифест `<registry>/genpass:1.0` создан и содержит
      linux/amd64 + linux/arm64 + linux/arm/v7 (проверено `imagetools inspect`),
      ИЛИ (если registry недоступен) подготовлены per-platform tar-файлы и это согласовано
      с пользователем
