# genpass — Генератор паролей

State-less офлайн-генератор паролей, passphrase и PIN на Java 21 с минимальным веб-интерфейсом.
Приложение ничего не хранит и не логирует: сгенерированный секрет существует только в ответе
сервера и в вашем браузере. Предназначен для запуска в Docker-контейнере на Synology NAS
(Container Manager, DSM 7+) или локально через `java -jar`.

- Ядро генерации — чистый JDK (`SecureRandom`, без сторонних зависимостей).
- Веб-слой — встроенный `com.sun.net.httpserver` (JDK), JSON API + статический UI.
- Полностью офлайн: словарь EFF и все ресурсы UI внутри jar, никаких внешних запросов.
- Оценка стойкости: энтропия в битах, словесная метка, время перебора при 10¹⁰ попыток/сек.

---

## Быстрый старт (локально)

Требования: JDK 21, Maven 3.8+.

```bash
mvn clean verify          # 56 тестов: unit (TestNG) + интеграционные (rest-assured)
java -jar target/genpass.jar          # http://localhost:8080
PORT=9090 java -jar target/genpass.jar        # свой порт
HOST=127.0.0.1 PORT=9090 java -jar target/genpass.jar   # свой адрес
```

### HTTP API

| Метод/путь | Назначение | Успех | Ошибка |
|---|---|---|---|
| `GET /api/health` | healthcheck контейнера | `200 {"status":"ok"}` | — |
| `POST /api/password` | пароль | `200` + JSON | `400` при некорректных параметрах |
| `POST /api/passphrase` | passphrase из словаря EFF | `200` + JSON | `400` |
| `POST /api/pin` | PIN | `200` + JSON | `400` |
| `GET /`, `/app.js`, `/style.css` | веб-интерфейс | `200` | `404` |

Формат ответа единый:

```json
{"result":"...","entropyBits":57.0,"strength":"средний","crackTime":"~2 час"}
```

Примеры запросов:

```bash
curl -X POST http://localhost:8080/api/password \
  -H "Content-Type: application/json" \
  -d '{"length":20,"lowercase":true,"uppercase":true,"digits":true,"special":true,"excludeAmbiguous":true}'

curl -X POST http://localhost:8080/api/passphrase \
  -H "Content-Type: application/json" \
  -d '{"wordCount":5,"separator":"-","capitalize":true,"addDigit":true}'

curl -X POST http://localhost:8080/api/pin \
  -H "Content-Type: application/json" \
  -d '{"length":6,"noLeadingZero":true}'
```

Параметры:

- **password**: `length` 4–128; наборы `lowercase`/`uppercase`/`digits`/`special`
  (хотя бы один); `excludeAmbiguous` убирает `I l 1 O 0` (и `|` при наличии).
  Гарантируется хотя бы один символ каждого включённого набора.
- **passphrase**: `wordCount` 3–12; `separator` — `-`, `_`, пробел или пустая строка;
  `capitalize` — первая буква каждого слова в верхнем регистре;
  `addDigit` — одна случайная цифра в конец одного случайного слова.
- **pin**: `length` 3–12; `noLeadingZero` — первая цифра не `0`.

Все API-ответы несут заголовки безопасности: `Cache-Control: no-store`, CSP,
`X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY`.

---

## Docker

### Сборка образа (amd64)

```bash
docker buildx build --load -t genpass:local .
docker run -d -p 8080:8080 --name genpass genpass:local
```

Контейнер: non-root (пользователь `app`), healthcheck через Java-класс
(`org.example.genpass.web.HealthCheck`), `PORT=8080`.

### Multi-arch: amd64 + arm64 + arm/v7

Два Dockerfile: общий (`Dockerfile`, runtime `eclipse-temurin:21-jre` — amd64/arm64)
и `Dockerfile.armv7` (runtime `debian:13-slim` + `openjdk-21-jre-headless` armhf —
Temurin armv7-образов не выпускает). Builder-стейдж в обоих выполняется нативно
(`--platform=$BUILDPLATFORM`), тесты прогоняются на этапе сборки.

Сборка универсального образа через registry:

```bash
docker buildx create --use
docker buildx build --platform linux/amd64,linux/arm64 -t <registry>/genpass:1.0-core --push .
docker buildx build --platform linux/arm/v7 -f Dockerfile.armv7 -t <registry>/genpass:1.0-armv7 --push .
docker buildx imagetools create -t <registry>/genpass:1.0 \
  <registry>/genpass:1.0-core <registry>/genpass:1.0-armv7
```

Без registry (офлайн-сценарий) — per-platform tar, см. ниже.

---

## Деплой на Synology NAS (Container Manager, DSM 7+)

### 1. Определите архитектуру NAS

SSH на NAS: `uname -m`, или DSM → **Центр управления → Общие сведения → ЦП**:

| `uname -m` | Архитектура | Файл образа |
|---|---|---|
| `x86_64` | amd64 | `genpass-1.0-amd64.tar` |
| `aarch64` | arm64 | `genpass-1.0-arm64.tar` |
| `armv7l` | arm/v7 (32-bit) | `genpass-1.0-armv7.tar` |

### 2. Доставьте образ на NAS

Без доступа к registry — офлайн-импорт:

1. Соберите tar на машине сборки (Docker с buildx, требуется QEMU/binfmt для arm64/armv7):

   ```bash
   docker buildx create --name multi --driver docker-container --use
   docker buildx build --builder multi --platform linux/amd64 \
     -t genpass:1.0 -o type=docker,dest=genpass-1.0-amd64.tar .
   docker buildx build --builder multi --platform linux/arm64 \
     -t genpass:1.0 -o type=docker,dest=genpass-1.0-arm64.tar .
   docker buildx build --builder multi --platform linux/arm/v7 -f Dockerfile.armv7 \
     -t genpass:1.0 -o type=docker,dest=genpass-1.0-armv7.tar .
   ```

2. Скопируйте нужный tar на NAS (SMB/SCP), например в `/volume1/docker/`.
3. Импортируйте: **Container Manager → Образ → Импорт → выбрать tar**
   (или по SSH: `docker load < /volume1/docker/genpass-1.0-<arch>.tar`).

> Внимание: tar содержит ОДНУ платформу — не перепутайте файл с архитектурой NAS.
> Универсальный multi-arch манифест доступен только через registry (см. раздел Docker).

### 3. Создайте проект

1. **Container Manager → Проект → Создать**.
2. Имя: `genpass`, путь — папка на томе (например, `/volume1/docker/genpass`).
3. Источник: **«Загрузить docker-compose.yml»** → вставьте содержимое
   [`docker-compose.yml`](docker-compose.yml) из этого репозитория → Готово.

Проект поднимет контейнер `genpass` с `restart: unless-stopped`, порт хоста **8088**,
healthcheck каждые 30 с.

### 4. Проверьте

- **Container Manager → Проект → genpass**: статус «Работает», вкладка «Состояние» — healthy.
- Откройте `http://<NAS_IP>:8088` — интерфейс генератора.
- Проверьте генерацию всех трёх типов (Пароль / Passphrase / PIN).
- Контейнер переживает перезагрузку DSM (restart-политика) — проверьте после рестарта.

---

## Reverse proxy / HTTPS (рекомендуется)

Кнопка «Копировать» использует `navigator.clipboard`, который работает только в
безопасном контексте (HTTPS или localhost). По HTTP в LAN работает fallback
(`document.execCommand`); полный функционал копирования — после настройки HTTPS.

1. DSM → **Панель управления → Порт входа → Дополнительно → Обратный прокси → Создать**:
   - Источник: `https://pass.<ваш-домен>:443` (протокол HTTPS);
   - Назначение: `http://localhost:8088` (протокол HTTP).
2. Сертификат: **Панель управления → Сертификат → Добавить → Let's Encrypt**
   для домена (NAS должен быть доступен извне по 80/443).

---

## Структура и безопасность

- `src/main/java/org/example/genpass/core` — ядро генерации (JDK-only, без веб-зависимостей);
- `src/main/java/org/example/genpass/web` — HTTP-слой (HttpServer, JSON, статика, заголовки);
- `src/main/resources/wordlists/eff-short.txt` — словарь EFF (public domain, 1296 слов);
- Случайность — только `java.security.SecureRandom`; без `Math.random()`/`Random`;
- Результаты генерации не логируются и не сохраняются; `Cache-Control: no-store` на API;
- Приложение не содержит внешних URL, телеметрии и CDN — полностью офлайн.

Лицензия словаря — в `src/main/resources/wordlists/eff-short.LICENSE.txt`.
