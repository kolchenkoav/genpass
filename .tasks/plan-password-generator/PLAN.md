# План разработки: приложение «Генератор паролей» (genpass)

> State-less веб-приложение на Java 17 для генерации паролей, passphrase и PIN,
> с деплоем в Docker-контейнере на Synology NAS (Container Manager, DSM 7+).

**Plan File:** `.tasks/plan-password-generator/PLAN.md`
**Tasks Directory:** `.tasks/plan-password-generator/`

---

## Your Mission (миссия проекта)

Довести репозиторий `genpass` от текущего состояния (пустой каркас) до
работающего офлайн-приложения «Генератор паролей»: ядро генерации на Java 21
минимальный веб-интерфейс, покрытое тестами TestNG, упакованное в multi-arch
Docker-образ и задеплоенное на Synology NAS через Container Manager.

---

## Execution Steps (порядок работы исполнителя)

1. **Прочитать этот план** — найти следующий незачёркнутый task в `## Task Plan`.
2. **Прочитать файл своей задачи** — `.tasks/plan-password-generator/task-XX-*.md`
   (Goal / What to Do / Key Points / Done When).
3. **Выполнить задачу** — правки кода, тесты (`mvn clean verify`), проверка критериев Done When.
4. **Обновить этот план** — отметить задачу в `## Task Plan`, добавить 1–2 предложения
   в `## Shared Context → Outcomes` о том, что сделано и какие приняты решения.
5. **Дождаться одобрения пользователя** перед переходом к следующей задаче.
6. **Пересмотреть список задач** — если всплыла новая сложность, предложить
   разбить/слить/добавить задачи и согласовать с пользователем.

---

## Task Plan (чек-лист задач)

- [x] **task-01-cleanup-baseline.md** — M0. Чистка pom.xml и починка тестового каркаса (базовая линия сборки)
- [x] **task-02-core-engine.md** — M1. Ядро генерации (пароль / passphrase / PIN / оценка стойкости) + unit-тесты TestNG
- [ ] **task-03-web-layer.md** — M2. Веб-слой (JDK HttpServer + JSON API + статический UI) + интеграционные тесты
- [ ] **task-04-docker.md** — M3. Docker: два multi-stage Dockerfile (общий + armv7), multi-arch (amd64/arm64/armv7) в одном манифесте, healthcheck, non-root
- [ ] **task-05-synology-deploy.md** — M4. docker-compose.yml + инструкция деплоя на Synology (Container Manager, reverse proxy)
- [ ] **task-06-acceptance.md** — M5. Финальная приёмка: прогон всех критериев, хардненинг-проверки, README

Зависимости: 01 → 02 → 03 → 04 → 05 → 06 (строго последовательно; каждая задача
опирается на результат предыдущей).

---

## Текущее состояние проекта (аудит от Setup-агента)

Фактическое состояние репозитория `C:\projects\genpass` на момент планирования:

| Что проверено | Факт                                                                                                                                                                                                                                                                                          |
|---|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `pom.xml` | groupId `org.example`, artifactId `genpass`, version `1.0-SNAPSHOT`; Java **17** (`maven.compiler.source/target=17`), UTF-8; packaging — по умолчанию **jar** (не указан)                                                                                                                     |
| Зависимости | `testng:7.9.0` (**без `<scope>test</scope>` — дефект**), `io.rest-assured:rest-assured:5.4.0` + `json-path:5.4.0` + `json-schema-validator:5.4.0` (последний — compile-scope), `jackson-databind:2.17.2`, `gson:2.13.1`, `slf4j-api:2.0.16` + `slf4j-nop:2.0.16`, `lombok:1.18.44` (provided) |
| Плагины | `maven-surefire-plugin:3.2.5` с `<suiteXmlFiles>` → `src/test/resources/testNG.xml` (этого файла **нет** — есть `src/test/java/resources/testNG.xml`, т.е. путь не совпадает)                                                                                                                 |
| `src/main/java` | **пусто** — ни одного класса                                                                                                                                                                                                                                                                  |
| `src/main/resources` | **пусто**                                                                                                                                                                                                                                                                                     |
| `src/test/java` | только файл `src/test/java/resources/testNG.xml` — чужой остаток шаблона («Booker Test Suite», классы `testCases.BookingSmokeTests` / `testCases.GetSmokeTests` не существуют)                                                                                                                |
| Dockerfile / docker-compose | **отсутствуют**                                                                                                                                                                                                                                                                               |
| README | **отсутствует**                                                                                                                                                                                                                                                                               |
| Maven wrapper (`mvnw`, `.mvn`) | **нет** (каталог `.mvn` пуст) — сборка требует системный/контейнерный Maven                                                                                                                                                                                                                   |

**Вывод:** проект — каркас из шаблона REST-API-тестирования, кода приложения нет.
Фактически разработка идёт «с чистого листа», но с готовыми (частично лишними)
зависимостями, которые надо почистить в первую очередь. Вся прикладная ценность
(ядро, веб, Docker, деплой) — впереди.

**Дефекты, которые нужно исправить (задача 01):**
1. `testng` без scope `test` попадёт в production classpath (и в fat-jar).
2. `rest-assured` и его модули должны быть только `test` scope (используем для
   интеграционных тестов HTTP-слоя); `json-schema-validator` со scope `compile` — убрать scope.
3. `testNG.xml` ет ссылок на классы → `mvn test` упадёт.
4. Дублирование JSON-библиотек (jackson + gson) — оставить одну (Jackson).

---

## 1. Цели и границы (Scope)

### Входит в объём (in-scope)

- Генератор случайных паролей с настраиваемыми наборами символов и длиной.
- Генератор passphrase из встроенного офлайн-словаря (EFF short wordlist).
- Генератор PIN-кода.
- Оценка стойкости (энтропия в битах + словесная оценка).
- Веб-интерфейс: форма параметров, вывод результата, копирование в буфер обмена (клиентский JS).
- JSON HTTP API (для curl/интеграционных тестов).
- Unit-тесты ядра на TestNG + интеграционные тесты HTTP-слоя (rest-assured).
- Multi-stage Dockerfile (multi-arch: linux/amd64 + linux/arm64 + linux/arm/v7), non-root, healthcheck.
- docker-compose.yml и пошаговая инструкция деплоя на Synology NAS (Container Manager).
- README.md (RU): сборка, запуск, деплой.

### НЕ входит (out-of-scope)

- Хранение паролей / менеджер паролей / «сейф» — приложение строго state-less, без БД.
- Аккаунты, авторизация, мультипользовательский режим.
- История сгенерированных паролей (принципиально: ничего не сохраняем и не логируем).
- Мобильные приложения, десктоп-клиенты, CLI-интерфейс.
- Интеграции с внешними сервисами, телеметрия, аналитика — приложение полностью офлайн.
- ~~Поддержка старых NAS на armv7~~ — ИЗМЕНЕНО решением пользователя: armv7 ВКЛЮЧЁН в scope через отдельный armv7-runtime (см. разделы 8.2–8.3).
- Локализация UI на языки, кроме русского (i18n-каркас можно заложить, переводы — нет).

---

## 2. Функциональные требования

### 2.1. Генерация пароля (`POST /api/password`)

- Длина: целое от **4 до 128** включительно; вне диапазона — HTTP 400 + JSON `{"error": ...}`.
- Наборы символов (флаги, можно комбинировать; хотя бы один должен быть включён):
  - строчные латинские `a–z`;
  - прописные латинские `A–Z`;
  - цифры `0–9`;
  - специальные `!@#$%^&*()-_=+[]{};:,.<>?/~`.
- **Исключение неоднозначных символов** (флаг): из результата убираются `I l 1 O 0`
  (а также `|`, если спецнабор это позволяет), чтобы пароль читался без ошибок при вводе.
- **Гарантия покрытия**: если набор включён, в пароле гарантированно есть хотя бы один
  символ этого набора (заполнение по одному из каждого набора + добор + перемешивание
  Фишера–Йетса на SecureRandom).
- Ответ: `{"result": "...", "entropyBits": N, "strength": "...", "crackTime": "..."}` — единый формат для всех трёх эндпоинтов [решение M2; исходный ключ «password» заменён на «result»].

### 2.2. Генерация passphrase (`POST /api/passphrase`)

- Словарь: встроенный ресурс `src/main/resources/wordlists/eff-short.txt` — публичный
  EFF short wordlist (~1296 слов, public domain, полностью офлайн).
- Количество слов: целое от **3 до 12**; вне диапазона — 400.
- Разделитель: `-`, `_`, пробел или пустой (на выбор).
- Опция «капитализировать слова» (первая буква каждого слова в верхнем регистре).
- Опция «добавить цифру» (одна случайная цифра 0–9 добавляется в конец одного случайно выбранного слова). [Уточнено в M1-triage: исходная формулировка «в случайной позиции» заменена — реализация проще, а оценка +3.32 бита остаётся консервативной (не завышенной).]
- Энтропия: `words × log2(dictSize)` (+ ~3.32 бита за цифру) — опции должны честно
  отражаться в оценке.

### 2.3. Генерация PIN (`POST /api/pin`)

- Длина: от **3 до 12**, только цифры `0–9`.
- Опция «без ведущих нулей» (для PIN, которые вводятся «вслепую»).

### 2.4. Оценка стойкости

- Энтропия = `length × log2(alphabetSize)` для случайных паролей/PIN; для passphrase —
  из размера словаря (см. 2.2).
- Словесные оценки по битам: `< 40` — «слабый», `40–59` — «средний», `60–79` — «сильный»,
  `≥ 80` — «очень сильный».
- Дополнительно — справочная оценка времени перебора при офлайн-атаке ~10^10 попыток/сек.

### 2.5. Веб-интерфейс (SPA, одна страница)

- Три вкладки/режима: «Пароль», «Passphrase», «PIN» с элементами управления из п. 2.1–2.3.
- Кнопка «Сгенерировать» → запрос к API → отображение результата моноширинным шрифтом
  с индикатором стойкости и энтропии.
- Кнопка «Копировать» — буфер обмена на стороне клиента (см. 3.5).
- Все запросы идут только к этому же origin; никаких внешних URL в разметке/скриптах.

---

## 3. Нефункциональные требования и безопасность

1. **КСГП** — источником случайности только `java.security.SecureRandom` (один экземпляр,
   потокобезопасно). Никаких `java.util.Random` / `Math.random()` / `System.currentTimeMillis()`.
2. **Отсутствие смещения (modulo bias)** — выбор символа/слова через `SecureRandom.nextInt(bound)`
   (внутри JDK уже равномерно), не через `значение % n`.
3. **Запрет логирования секретов**: в коде нет ни одного вызова логгера/`System.out` с
   результатом генерации. Binding `slf4j-nop` (уже в pom) дополнительно гарантирует,
   что случайное стороннее логирование ничего не выведет. В stdout — только сообщения
   старта/остановки сервера (порт, версия).
4. **HTTP-заголовки для всех ответов API**: `Cache-Control: no-store` (пароль не должен
   попадать в кэши браузера/посредников). Для статики — `Cache-Control: no-cache`.
5. **CSP и прочие заголовки**: `Content-Security-Policy: default-src 'none'; script-src 'self';
   style-src 'self'; connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'`,
   плюс `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`,
   `X-Frame-Options: DENY`. Инлайн-скрипты запрещены — JS в отдельном файле `app.js`.
6. **Полностью офлайн**: словарь и все ресурсы UI внутри jar; никаких CDN, внешних шрифтов,
   телеметрии. Проверяется ревью кода + отсутствием внешних хостов в коде.
7. **State-less**: нет БД, нет файлов на диске, нет сессий — контейнер можно перезапускать/масштабировать.
8. **Кодировки**: всё в UTF-8 (`project.build.sourceEncoding` уже UTF-8); JSON отдаётся с
   `Content-Type: application/json; charset=UTF-8`; спецсимволы корректно экранируются Jackson'ом.
9. **Производительность**: отклик генерации < 50 мс на LAN; сервер держит параллельные
   запросы (thread-pool executor).
10. **Легковесность образа**: runtime-слой на базе `eclipse-temurin:17-jre`, итоговый образ
    без лишних утилит; — см. раздел Docker.

---

## 4. Технологический стек (по факту зависимостей проекта)

| Слой | Технология | Обоснование (факт из pom/репо) |
|---|---|---|
| Язык/сборка | Java 17 + Maven | Уже задано в `pom.xml` (`maven.compiler.*=17`) |
| Ядро генерации | Чистый JDK: `SecureRandom` | Ядро сознательно без сторонних зависимостей — проще аудировать безопасность |
| Веб-сервер | **Встроенный `com.sun.net.httpserver.HttpServer`** (модуль `jdk.httpserver`, входит в JDK 17) | Spring Boot в pom отсутствует и избыточен для state-less генератора; сторонние лёгкие серверы (Javalin/Undertow) = новые зависимости. JDK-сервер — ноль новых зависимостей, многопоточность через `ExecutorService`, достаточен для LAN. Меньше зависимостей = меньше CVE-поверхность |
| JSON | `jackson-databind` 2.17.2 (уже в pom) | Уже есть; `gson` — убрать как дубль |
| Модель/DTO | Lombok (уже в pom, provided) | Сокращает бойлерплейт DTO запросов/ответов |
| Логирование | `slf4j-api` + `slf4j-nop` (уже в pom) | NOP-биндинг — гарантия «ничего не логируется»; для приложения с паролями это фича безопасности |
| Unit-тесты | TestNG 7.9.0 (уже в pom; перевести в scope `test`) | Задано: «Java + Maven + TestNG» |
| Интеграционные тесты HTTP | rest-assured 5.4.0 (уже в pom; перевести в scope `test`) | Уже есть — переиспользуем для проверки эндпоинтов/заголовков вместо ручного curl |
| Fat-jar | `maven-shade-plugin` | jar-packaging по умолчанию не пакует зависимости; shade даёт самодостаточный jar для Docker |
| Контейнеры | Builder: `--platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-17`; runtime: `eclipse-temurin:17-jre` (amd64/arm64) или `debian:12-slim` + `openjdk-17-jre-headless` (arm/v7) | Multi-stage, multi-arch: linux/amd64 + linux/arm64 + linux/arm/v7 (см. раздел 8) |

---

## 5. Архитектура

### 5.1. Пакеты (`src/main/java`, базовый пакет `org.example.jenpass`)

```
org.example.genpass
├── App                        // main(): конфиг из env (PORT, HOST), запуск сервера
├── core/                      // ЯДРО: не знает о HTTP/Jackson — только JDK
│   ├── CryptoRandom           // обёртка SecureRandom: nextInt(bound), shuffle()
│   ├── CharGroups             // константы наборов + AMBIGUOUS = {I,l,1,O,0,|}
│   ├── PasswordOptions        // record: length, наборы, excludeAmbiguous
│   ├── PinOptions              // record: length, noLeadingZero
│   ├── PassphraseOptions      // record: wordCount, separator, capitalize, addDigit
│   ├── PasswordGenerator      // гарантия покрытия наборов + перемешивание
│   ├── Wordlist               // загрузка /wordlists/eff-short.txt (UTF-8, ~1296 слов)
│   ├── PassphraseGenerator
│   ├── PinGenerator
│   └── StrengthEstimator      // entropyBits, метка, время перебора @10^10/s
└── web/                       // ВЕБ-СЛОЙ: зависит от core, не наоборот
    ├── WebServer              // HttpServer + маршруты + ExecutorService
    ├── ApiHandlers            // парсинг JSON → core → ответ Jackson
    ├── StaticHandler          // отдача /, /app.js, /style.css из classpath
    ├── SecurityHeadersFilter  // no-store, CSP, nosniff, no-referrer...
    └── HealthCheck            // main() для Docker HEALTHCHECK: GET /api/health → exit 0/1
```

### 5.2. Ключевое правило

**Зависимости направлены только внутрь**: `web → core`; ядро `core` не импортирует
`com.sun.net.httpserver`, Jackson и классы веб-слоя. Благодаря этому ядро тестируется
чистыми TestNG-тестами без поднятия сервера.

### 5.3. HTTP API (v1; все ответы — JSON + заголовки безопасности)

| Метод/путь | Назначение | Успех | Ошибка |
|---|---|---|---|
| `GET /api/health` | healthcheck контейнера | 200 `{"status":"ok"}` | — |
| `POST /api/password` | пароль (п.2.1) | 200 + JSON | 400 при некорректных параметрах |
| `POST /api/passphrase` | passphrase (п.2.2) | 200 + JSON | 400 |
| `POST /api/pin` | PIN (п.2.3) | 200 + JSON | 400 |
| `GET /`, `/app.js`, `/style.css` | статический UI | 200 | 404 |

Пример запроса: `{"length":20,"lowercase":true,"uppercase":true,"digits":true,"special":true,"excludeAmbiguous":true}`.

### 5.4. Статический UI

- `src/main/resources/web/index.html`, `app.js`, `style.css` — ванильные HTML/JS/CSS без
  фреймворков (никаких CDN, офлайн). Моноширинный вывод пароля, индикатор стойкости,
  три режима (Пароль / Passphrase / PIN).

---

## 6. Этапы (Milestones) и задачи

> Каждый этап = одна задача = один файл task-XX-*.md в этом каталоге.
> Готовность этапа проверяется прогоном `mvn clean verify` + пунктами Done When задачи.

### M0. Чистка и базовая линия сборки — `task-01-cleanup-baseline.md`

Задачи:
- Исправить `pom.xml`: `testng` и все три артефакта `rest-assured` → scope `test`;
  удалить `gson` (дубль Jackson); проверить итоговый состав зависимостей.
- Перенести/пересоздать suite-файл по пути из surefire: `src/test/resources/testNG.xml`
- Подключить `maven-shade-plugin` (fat-jar, `Main-Class: org.example.genpass.App`).
- Создать пустые пакеты `org.example.genpass.core` / `...web` и заглушку `App`.
- Убедиться, что `mvn clean verify` зелёный (минимальный smoke-тест TestNG).

Готовность M0: `mvn clean verify` проходит; в dependency-tree нет testng/rest-assured в compile
scope; jar собирается и запускается.

### M1. Ядро генерации + unit-тесты — `task-02-core-engine.md`

Задачи:
- `CryptoRandom` (SecureRandom), `CharGroups` (+ AMBIGUOUS), `PasswordOptions`/`PassphraseOptions`.
- `PasswordGenerator` (гарантия покрытия наборов, Фишер–Йетс), `PinGenerator`,
  `Wordlist` (EFF short, ресурс UTF-8), `PassphraseGenerator`, `StrengthEstimator`.
- Валидация опций (границы длин из п.2.1–2.3) — `IllegalArgumentException`.
- Unit-тесты TestNG по разделу 7.1 (ядро НЕ зависит от веб-слоя).

Готовность M1: классы ядра реализованы; `mvn test` зелёный; статистические и граничные тесты
проходят; в коде ядра нет импортов веб-слоя/Jackson.

### M2. Веб-слой + UI + интеграционные тесты — `task-03-web-layer.md`

Задачи:
- `WebServer` (JDK HttpServer, `ExecutorService.newFixedThreadPool`, graceful shutdown),
  маршруты из п.5.3; конфиг `PORT`/`HOST` из env (по умолчанию 8080/0.0.0.0).
- `ApiHandlers` + DTO Jackson; `SecurityHeadersFilter` (no-store, CSP, nosniff, no-referrer).
- `StaticHandler` + `src/main/resources/web/{index.html,app.js,style.css}` (RU-интерфейс,
  кнопка «Копировать» через `navigator.clipboard` с fallback `execCommand`).
- `HealthCheck` (Java-main для Docker HEALTHCHECK).
- Интеграционные тесты rest-assured (раздел 7.2): старт сервера на свободном порту.

Готовность M2: `mvn clean verify` зелёный (unit + integration); ручной смоук `java -jar` —
страница открывается, все эндпоинты отвечают, заголовки безопасности присутствуют;
копирование в буфер работает (минимум на localhost).

### M3. Docker — `task-04-docker.md`

Решение пользователя: ОДИН универсальный multi-arch образ сразу под linux/amd64,
linux/arm64 и linux/arm/v7 — уточнять заранее архитектуру NAS не нужно.

Задачи:
- Multi-stage `Dockerfile` (раздел 8.1): builder с `--platform=$BUILDPLATFORM`
  `maven:3.9-eclipse-temurin-17` (`dependency:go-offline` + `mvn package`), runtime
  `eclipse-temurin:17-jre` + non-root + fat-jar + Java-`HEALTHCHECK` + `EXPOSE 8080`.
- Отдельный `Dockerfile.armv7` (раздел 8.2): тот же builder; runtime `debian:12-slim` +
  `openjdk-17-jre-headless` (armhf) — Temurin не выпускает armv7-образов JDK 17.
- Локальная проверка: сборка amd64 (`--load`), запуск, `docker inspect` — health=healthy,
  процесс от non-root; размеры образов (amd64 и armv7) зафиксировать в outcome.
- Multi-arch по разделу 8.3: temurin-платформы + armv7 объединяются в один манифест
  (`docker buildx imagetools create`); проверить `imagetools inspect` — 3 платформы.
- Офлайн-запасной сценарий: per-platform tar через `-o type=docker,dest=...` (раздел 9.3).

Готовность M3: образ собирается; локально запускается и проходит healthcheck; универсальный
multi-arch манифест (amd64+arm64+arm/v7) собран и запушен в registry (или подготовлены
платформенно-специфичные tar для офлайн-импорта на NAS).

### M4. Деплой на Synology — `task-05-synology-deploy.md`

Задачи:
- `docker-compose.yml`: `restart: unless-stopped`, `ports: "8088:8080"`,
  `environment: PORT=8080`, healthcheck, `container_name: genpass`.
- `README.md` (RU): определение архитектуры NAS (SSH `uname -m` или DSM → Центр управления →
  Общие сведения → ЦП); два сценария — (а) pull универсального multi-arch образа из registry,
  (б) без registry: перенос per-platform tar (раздел 9.3) и импорт через Container Manager →
  Образ → Импорт; далее Container Manager → Проект → Создать → загрузка compose-файла
  из shared folder.
- Инструкция reverse-proxy/HTTPS: DSM → Панель управления → Порт входа → Дополнительно →
  Обратный прокси (https://pass.<домен> → http://localhost:8088), сертификат Let's Encrypt;
  заметка: `navigator.clipboard` требует secure context — по HTTP в LAN работает fallback,
  полный функционал копирования — после HTTPS.
- Чек-лист после деплоя (health, генерация трёх типов, заголовки, no-store).

Готовность M4: контейнер развёрнут на целевом NAS (или пользователь подтвердил готовность
инструкции), restart-политика активна, страница доступна по http://NAS_IP:8088.

### M5. Финальная приёмка и хардненинг — `task-06-acceptance.md`

Задачи:
- Прогон всех критериев приёмки (раздел 10) + ревью безопасности: поиск в коде
  `System.out|System.err` рядом с генерацией, внешних URL, `java.util.Random`.
- Проверка ответов: `Cache-Control: no-store` и CSP на всех эндпоинтах; сценарии 400/404.
- Финальная версия README, версия `1.0.0` в pom, (опционально) git-тег.

Готовность M5: все пункты раздела 10 отмечены; пользователь подтвердил приёмку.

---

## 7. Тестирование

### 7.1. Unit-тесты ядра (TestNG, `src/test/java/org/example/genpass/core/`)

- **Границы длины**: 4/128 валидны, 3/129 → `IllegalArgumentException`; PIN 3/12, passphrase 3/12 слов.
- **Принадлежность алфавиту**: каждый символ результата входит в выбранные наборы.
- **Исключение неоднозначных**: с `excludeAmbiguous=true` в результате нет `I,l,1,O,0,|`.
- **Гарантия покрытия**: при 1000 генераций length=4 с 4 наборами каждый набор представлен.
- **Статистика**: частота символов в 100 000 генераций (первый символ генераций длиной 4 —
  длина 1 невалидна по 2.1) укладывается в доверительный
  интервал (хи-квадрат или допуск ±20% от равномерного); нет смещения между наборами.
- **Passphrase**: словарь загружен (≈1296 слов), слово — элемент словаря, разделитель/
  капитализация/цифра применяются; повторные генерации различаются.
- **PIN**: только цифры, длина, опция без ведущего нуля.
- **Энтропия**: точечные значения (8 строчных ≈ 37.6 бит; 5 слов из 1296 ≈ 51.70 бит —
  log2(1296)=10.3399, ×5=51.699; поправка от 51.65) с допуском ±0.01.
- **Кодировки**: словарь читается в UTF-8 без искажений; спецсимволы проходят генерацию.

### 7.2. Интеграционные тесты HTTP-слоя (rest-assured, `src/test/java/.../web/`)

- `@BeforeSuite`: запуск `WebServer` на свободном порту, `@AfterSuite` — остановка.
- `GET /api/health` → 200, `status=ok`.
- `POST /api/password|passphrase|pin` → 200, схема ответа (строка ожидаемой длины,
  `entropyBits` — число, `strength` — одна из меток).
- Заголовки всех API-ответов: `Cache-Control=no-store`, CSP, `X-Content-Type-Options=nosniff`.
- 400 на некорректные параметры (`length=3`, `words=100`, все наборы выключены);
  404 на неизвестный путь; `GET /` отдаёт HTML.

---

## 8. Docker

### 8.1. Dockerfile — общий multi-stage (linux/amd64 + linux/arm64)

```dockerfile
# ---------- Stage 1: build ----------
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-17 AS build   # сборка всегда нативно на хосте
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline          # кэш зависимостей отдельным слоем
COPY src ./src
RUN mvn -B clean package                       # тесты выполняются здесь

# ---------- Stage 2: runtime ----------
FROM eclipse-temurin:17-jre
RUN groupadd -r app && useradd -r -g app -d /app app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/genpass.jar /app/app.jar
USER app                                       # non-root обязательно
EXPOSE 8080
ENV PORT=8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD ["java", "-cp", "/app/app.jar", "org.example.jenpass.web.HealthCheck"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

Решения:
- **Java-based HEALTHCHECK** (`HealthCheck.main()` → `HttpClient` GET `localhost:$PORT/api/health`):
  базовые образы temurin не содержат curl/wget — не тянем лишние пакеты в образ.
- **non-root**: выделенная группа/пользователь `app`; jar принадлежит ей.
- Тесты выполняются на stage сборки — в runtime-образ попадает только проверенный артефакт.
- `--platform=$BUILDPLATFORM`: Maven-стейдж исполняется нативно на хосте сборки (QEMU не
  эмулирует тяжёлую Java-сборку — fat-jar платформо-независим).
- Этот Dockerfile покрывает amd64+arm64; отдельный armv7-вариант — в п. 8.2.

### 8.2. Dockerfile.armv7 — отдельный вариант для 32-bit ARM (linux/arm/v7)

У Eclipse Temurin нет armv7-образов JDK 17, поэтому arm/v7 получает собственный runtime.
Проверено по первоисточникам (web_fetch, план-время):
- **Debian 12 (bookworm)** публикует `openjdk-17-jre-headless` для архитектуры **armhf**
  (версия `17.0.20.1+1-1~deb12u1`, ~157 МБ установленного) — источник: packages.debian.org/bookworm/openjdk-17-jre-headless.
- Альтернатива — `bellsoft/liberica-openjre-debian:17`: официальный Dockerfile Liberica
  обрабатывает `armv[67]l` (arch `arm32-vfp-hflt`), НО по README репозитория тег без суффикса
  архитектуры по умолчанию покрывает только amd64+arm64 — перед использованием проверять
  `docker buildx imagetools inspect`. Azul Zulu 17 для armv7 подтвердить не удалось
  (страница загрузок рендерится скриптом) — не выбран.

Выбор: **Debian-путь** — детерминированный и не зависящий от vendor-тегов.

```dockerfile
# Dockerfile.armv7 — только linux/arm/v7
FROM --platform=$BUILDPLATFORM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B clean package

FROM debian:12-slim
RUN apt-get update \
 && apt-get install -y --no-install-recommends openjdk-17-jre-headless \
 && rm -rf /var/lib/apt/lists/* \
 && groupadd -r app && useradd -r -g app -d /app app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/genpass.jar /app/app.jar
USER app
EXPOSE 8080
ENV PORT=8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD ["java", "-cp", "/app/app.jar", "org.example.genpass.web.HealthCheck"]
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
```

### 8.3. Multi-arch сборка: amd64 + arm64 + arm/v7 в одном манифесте

```bash
docker buildx create --use                                        # один раз

# 1) платформы с temurin-runtime (общий Dockerfile)
docker buildx build --platform linux/amd64,linux/arm64 \
  -t <registry>/genpass:1.0-core --push .
# 2) armv7 из отдельного Dockerfile
docker buildx build --platform linux/arm/v7 -f Dockerfile.armv7 \
  -t <registry>/genpass:1.0-armv7 --push .
# 3) объединить в один универсальный манифест
docker buildx imagetools create \
  -t <registry>/genpass:1.0 \
  <registry>/genpass:1.0-core <registry>/genpass:1.0-armv7
```

- Итоговый тег `genpass:1.0` универсальный: Docker на любой модели Synology сам
  выбирает слой под свою архитектуру — узнавать модель заранее не нужно.
- Manifest-list живёт только в registry (в локальный docker загрузить нельзя) — сценарий
  без registry (per-platform tar) см. п. 9.3.
- QEMU эмулирует только лёгкие runtime-стейджи — Maven собирается нативно ($BUILDPLATFORM).
- Проверка платформ манифеста: `docker buildx imagetools inspect <registry>/genpass:1.0`
  — должны присутствовать linux/amd64, linux/arm64 и linux/arm/v7.

---

## 9. Деплой на Synology NAS

### 9.1. docker-compose.yml (в корне репо)

```yaml
services:
  genpass:
    image: genpass:1.0        # или <registry>/genpass:1.0
    container_name: genpass
    restart: unless-stopped
    ports:
      - "8088:8080"                  # хост:контейнер
    environment:
      - PORT=8080
    healthcheck:
      test: ["CMD", "java", "-cp", "/app/app.jar", "org.example.genpass.web.HealthCheck"]
      interval: 30s
      timeout: 5s
      retries: 3
```

### 9.2. Порядок деплоя через Container Manager (DSM 7+)

1. **Узнать архитектуру NAS** (нужно только для офлайн-импорта tar и финального теста): SSH
   (`uname -m`) или DSM → Центр управления → Общие сведения → ЦП: `x86_64` → amd64;
   `aarch64` → arm64; `armv7l` → arm/v7. При `docker pull` универсального манифеста (п. 8.3)
   платформа выбирается автоматически — шаг можно пропустить.
2. **Доставить образ**: (а) `docker pull <registry>/gen-pass:1.0` (Container Manager →
   Реестр); (б) без registry — импорт per-platform tar, см. п. 9.3.
3. **Создать проект**: Container Manager → Проект → Создать → имя `genpass`,
   путь — папка на выбранном томе (напр. `/volume1/docker/genpass`),
   источник «Загрузить docker-compose.yml» → вставить содержимое из репо → Готово.
4. **Проверить**: статус контейнера «Работает», вкладка «Состояние» — healthy;
   открыть `http://<NAS_IP>:8088` — интерфейс генератора.

### 9.3. Сценарий без registry (NAS без доступа к Docker Hub)

1. **Экспорт образа под платформу целевого NAS** на машине сборки:
   ```bash
   docker buildx build --platform linux/amd64 -o type=docker,dest=genpass-1.0-amd64.tar .
   docker buildx build --platform linux/arm64 -o type=docker,dest=genpass-1.0-arm64.tar .
   docker buildx build --platform linux/arm/v7 -f Dockerfile.armv7 -o type=docker,dest=genpass-1.0-armv7.tar .
   # либо из уже собранных локальных одноплатформенных образов: docker save <image> -o file.tar
   ```
   `-o type=docker` даёт обычный одноплатформенный tar; выбор файла — по архитектуре NAS (п. 9.2 шаг 1).
2. **Скопировать tar на NAS** (SMB/SCP) в shared folder, например `/volume1/docker/`.
3. **Импортировать**: Container Manager → **Образ** → **Импорт** → выбрать tar
   (или SSH: `docker load < /volume1/docker/genpass-1.0-<arch>.tar`),
   затем Container Manager → **Проект** → docker-compose с `image: genpass:1.0`.
4. Важно: tar содержит ОДНУ платформу — не перепутать файл под архитектуру NAS;
   универсальный multi-arch доступен только через registry (п. 8.3).

### 9.4. HTTPS / reverse proxy (опционально, рекомендуется)

- DSM → Панель управления → Порт входа → Дополнительно → Обратный прокси → Создать:
  источник `https://pass.<ваш-домен>:443` → назначение `http://localhost:8088` (протокол HTTP).
- Сертификат: Панель управления → Сертифик → Добавить → Let's Encrypt для домена
  (для валидации NAS должен быть доступен извне по 80/443, либо DSM-домен Synology).
- После HTTPS: `navigator.clipboard` (безопасный контекст) работает во всех браузерах —
  кнопка «Копировать» без fallback.

---

## 10. Критерии приёмки проекта

1. `mvn clean verify` зелёный: unit-тесты ядра (TestNG) + интеграционные (rest-assured).
2. `java -jar target/genpass.jar` стартует локально; `GET /api/health` → 200.
3. Все три режима генерации работают из браузера (пароль с настраиваемыми наборами и
   исключением неоднозначных символов; passphrase из словаря; PIN) + оценка стойкости.
4. Копирование результата в буфер обмена работает (localhost и/или HTTPS).
5. Заголовки `Cache-Control: no-store` и CSP присутствуют во всех API-ответах (проверено тестами).
6. В коде/логах отсутствуют: логирование паролей, внешние URL, `java.util.Random`, телеметрия.
7. Docker-образ собирается multi-stage; контейнер работает под non-root и становится healthy.
8. Образ собран под linux/amd64, linux/arm64 и linux/arm/v7 и объединён в универсальный
   multi-arch манифест (`imagetools create`), или подготовлены платформенно-специфичные
   tar для офлайн-импорта под целевой NAS.
9. docker-compose развёрнут на Synology через Container Manager: `restart: unless-stopped`,
   доступен по `http://<NAS_IP>:8088`, переживает перезагрузку DSM.
10. README.md покрывает: сборку, тесты, Docker, multi-arch, деплой на NAS, reverse proxy.

---

## 11. Риски и их снижение

| Риск | Влияние | Митигация |
|---|---|---|
| armv7-runtime (32-bit) | Temurin не выпускает armv7-образов JDK 17; armv7-слой строится на Debian 12 + `openjdk-17-jre-headless` (armhf, ~157 МБ) — образ тяжелее temurin-вариантов и зависит от пакетной базы Debian | Риск архитектуры NAS ЗАКРЫТ решением «универсальный multi-arch» (раздел 8.2–8.3): Debian-база проверена по packages.debian.org; Liberica `openjre-debian:17` — альтернатива (проверять тег через `imagetools inspect`); контролировать размер образа в M3; если armv7-вариант окажется неработоспособен на целевом NAS — исключить arm/v7 из манифеста с явным предупреждением в README |
| Multi-arch сборка требует registry и QEMU | Manifest-list нельзя загрузить в локальный docker; эмуляция медленная | Builder-стейдж закреплён за `$BUILDPLATFORM` (Maven всегда нативен; QEMU — только лёгкие runtime-стейджи); офлайн-запасной сценарий — per-platform tar + импорт (п. 8.3, 9.3) |
| 32-bit JVM на слабых armv7-NAS | Ограничения кучи/производительности 32-bit JVM | Приложение лёгкое (state-less); тестовый прогон на целевом NAS в M4; при проблемах — флаг `-Xmx256m` в ENTRYPOINT |
| `navigator.clipboard` требует secure context | «Копировать» может не работать по HTTP с LAN-IP в части браузеров | Fallback `document.execCommand('copy')` + рекомендация HTTPS reverse-proxy (п. 9.4) |
| JDK HttpServer — низкоуровневый API | Больше ручного кода (роутинг, обработка ошибок) | Тонкий слой `ApiHandlers` + интеграционные тесты rest-assured |
| Остаточные зависимости шаблона (rest-assured compile-scope, gson) | Лишний вес jar, CVE-поверхность | M0 строго выверяет scope/состав зависимостей |
| `testNG.xml` не по пути surefire + фантомные классы | `mvn test` падает сразу | Фиксируется первым действием в M0 |

---

## Shared Context (общий контекст для исполнителей)

### Overview
State-less офлайн-генератор паролей/passphrase/PIN на Java 17 с минимальным веб-UI,
деплой Docker-контейнером на Synology NAS. Ядро — чистый JDK; веб — встроенный HttpServer;
тесты — TestNG + rest-assured (всё уже есть в pom после чистки M0).

### Project Context
- `pom.xml` — Java 17, jar; после M0: testng/rest-assured в test scope; jackson + lombok + slf4j-nop в compile
- `src/main/java` — пуст (заполняется с M1); suite после M0: `src/test/resources/testNG.xml`
- Базовый пакет: `org.example.jenpass` (подпакеты `core`, `web`)

### Key Decisions
- Веб-сервер — встроенный `com.sun.net.httpserver` (ноль новых зависимостей; Spring Boot
  отвергнут: отсутствует в pom и избыточен для state-less генератора).
- JSON — Jackson (gson удаляется); логирование — slf4j-nop: ничего не логируется, это
  осознанная мера безопасности для приложения, генерирующего секреты.
- Healthcheck контейнера — Java-класс `HealthCheck`, а не curl/wget (их нет в temurin-образах).
- Словарь passphrase — EFF short wordlist (~1296 слов, public domain), ресурс внутри jar.
- Fat-jar — maven-shade-plugin; фиксированное имя артефакта: `genpass.jar`.
- Multi-arch (решение пользователя): ОДИН универсальный манифест под linux/amd64, linux/arm64
  и linux/arm/v7. Два Dockerfile: общий (temurin-runtime, amd64+arm64) и `Dockerfile.armv7`
  (debian:12-slim + openjdk-17-jre-headless armhf — проверено packages.debian.org/bookworm,
  пакет 17.0.20.1+1-1~deb12u1). Альтернатива armv7 — bellsoft/liberica-openjre-debian:17
  (Dockerfile вендора обрабатывает armv[67]l → arm32-vfp-hflt), но тег без суффикса arch,
  по README репозитория, по умолчанию покрывает только amd64+arm64 — проверять перед заменой.
  Builder-стейдж везде `--platform=$BUILDPLATFORM`.

### Caveats & Problems
- `src/test/java/resources/testNG.xml` (Booker) — мусор из шаблона, удаляется в M0.
- До M0 `mvn test` гарантированно падает: suite-файл лежит не по пути, указанному в surefire.
- Архитектура целевого NAS неизвестна, но это больше НЕ блокер: образ универсальный
  (amd64+arm64+arm/v7). Модель NAS нужна только для выбора per-platform tar при
  офлайн-импорте (п. 9.3) и для финального теста в M4.
- Доступность armv7 JRE 17 проверена web_fetch'ом: Debian bookworm — подтверждено;
  Liberica — подтверждено с оговоркой о тегах; Azul Zulu 17 armv7 — НЕ подтверждено
  (страница загрузок рендерится скриптом), поэтому не выбран.

### Outcomes (заполняется исполнителями по ходу работ)

> Примечание (rebuild 2026-09-01): записи ниже — история ПРЕЖНЕЙ реализации
> (jenerator-pass → generator-pass, коммиты в другом репозитории). Текущий репозиторий
> `genpass` пересобирается с нуля по этому плану: Java **21** (решение пользователя),
> базовый пакет **`org.example.genpass`** (PLAN 5.1; task-файлы со ссылками на
> `org.example.jenpass` считать устаревшими). Новые записи добавляются сверху.

- **M1 (task-02) выполнен.** Ядро `org.example.genpass.core` (10 классов):
  CryptoRandom (SecureRandom-синглтон INSTANCE, nextInt(bound), shuffle(char[]/String[])),
  CharGroups (SPECIAL 27 символов, AMBIGUOUS={I,l,1,O,0,|}), records PasswordOptions
  (4–128, ≥1 набор), PinOptions (3–12, noLeadingZero), PassphraseOptions (3–12 слов,
  разделитель -/_/пробел/пусто), PasswordGenerator (гарантия покрытия + Фишер–Йетс,
  alphabetSize(): пул 89, после исключения — 84; '|' нет в SPECIAL — фильтр no-op),
  PinGenerator (noLeadingZero: log2(9)+(n-1)log2(10)), Wordlist (ленивый volatile-кэш
  getDefault, load(InputStream) с пропуском пустых строк), PassphraseGenerator (цифра
  в конец случайного слова, решение A2), StrengthEstimator (метки RU, crackTime @1e10/с).
  Словарь eff-short.txt: 1296 слов с eff.org (префиксы кубиков срезаны, 100% [a-z-]+),
  LICENSE-файл атрибуции; .gitattributes eol=lf. Тесты test-first: сначала 7 классов
  тестов → падение «cannot find symbol» → реализация → зелёные. 39 тестов (7.1: границы,
  алфавит, исключение неоднозначных, покрытие 1000×4 набора, статистика 100k ±20%,
  passphrase словарь/разделитель/капитализация/цифра, PIN, энтропия 37.60/51.70/19.93/
  19.78/127.85, crackTime-якоря). 3 правки тестов: длина 1 невалидна (мин 4) — статистика
  по первому символу длины 4; пустой разделитель = сумма длин слов (19±); 40 бит = ~2 мин.
  `mvn clean verify` SUCCESS; core не импортирует web/Jackson (grep подтверждён).
- **M0 (task-01) выполнен.** Базовый коммит 3e232b8 (план, pom, тест-ресурсы; .idea исключён из индекса и добавлен
  в .gitignore). pom: finalName `genpass`, Java 21 (решение пользователя), maven-shade-plugin
  3.6.0 (Main-Class `org.example.genpass.App`, фильтр сигнатур); scope зависимостей уже
  корректны (testng/rest-assured×3 — test, gson отсутствует). Созданы App.java (печатает
  `genpass <version>`, version из манифеста с fallback), package-info для core/web,
  suite src/test/resources/testNG.xml (smoke: AppTest, 2 теста). `mvn -B clean verify` SUCCESS
  (2 теста, 1:13 мин); compile-scope: jackson×3 + slf4j-api/nop (lombok provided);
  `java -jar target/genpass.jar` → «genpass 1.0-SNAPSHOT», exit 0. Shade-warnings
  (module-info/LICENSE overlap) — штатные, не вредят.
- **M0 (task-01) выполнен, коммит 525b7e3.** testng/rest-assured → test scope, gson удалён, maven-shade-plugin 3.6.0 (finalName `genpass`, Main-Class `org.example.jenpass.App`, фильтр подписей). Созданы App.java + package-info для core/web, suite `src/test/resources/testNG.xml`, smoke AppTest (2 теста). `mvn clean verify` SUCCESS; compile-scope = jackson + slf4j(+nop) только; `java -jar` работает. Хвост: `dependency-reduced-pom.xml` (untracked) — добавить в .gitignore в M1.
- **M1 (task-02) выполнен + полный quality-loop.** Коммиты bf7f068 (ядро: 9 классов core, словарь EFF 1296 слов пословно сверен с оригиналом, LICENSE) → b057e18 (fix-wave ревью) → 7d2e6df (javadoc-фикс). Review×2 + triage: 12 TP/0 FP, все закрыты; ключевые: добавлен `PasswordGenerator.alphabetSize(PasswordOptions)` (фактический пул после excludeAmbiguous — ИТОГ 84, а не 83; '|' НЕ входит в SPECIAL — фильтр спецнабора no-op), Wordlist получил ленивый volatile-кэш дефалтного словаря, pickRandom → package-private, .gitattributes (eol=lf). Решение по A2: спека 2.2 исправлена — цифра addDigit добавляется в КОНЕЦ одного случайного слова (оценка +3.32 бита консервативна). Итог: 64 теста TestNG зелёные. ДЛЯ M2: энтропию пароля считать через alphabetSize(options); для PIN с noLeadingZero — точная формула log2(9)+(n−1)·log2(10) (javadoc StrengthEstimator).
- **M2 (task-03) выполнен + quality-loop.** Коммиты a5d050c (WebServer/ApiHandlers/SecurityHeaders/StaticHandler/HealthCheck, RU-UI без инлайн-скриптов, clipboard+fallback, 19 интеграционных тестов rest-assured) → 553b19a (fix-wave 12 TP: строгая int-коэрсия Jackson Float/String→Fail, лимит тела 64 КБ→413, 405+Allow, HEAD на статике, path-guard'ы, no-store на /api/*-404, валидация PORT+чистые ошибки, graceful stop, favicon, +11 тестов). Итог: 94 теста. Формат ответа {"result","entropyBits","strength","crackTime"} — PLAN 2.1 правлен.
- **M3 (task-04) выполнен, коммит 51d6233.** Docker 25.0.3 + buildx + QEMU доступны. Dockerfile (8.1) + Dockerfile.armv7 (8.2) + .dockerignore; образ 297 МБ, 94 теста зелёные ВНУТРИ build-stage; контейнер healthy, non-root (app), без curl/wget. Per-platform tar (офлайн-путь, registry недоступен): genpass-1.0-amd64.tar 106.3 МБ, -arm64.tar 104.8 МБ, -armv7.tar 93 МБ — лежат в корне проекта, в git не входят. Нюанс: тест «//app.js» переписан на raw-сокет + платформо-независимый ассерт (JDK в контейнере отдаёт 400, на Windows 404; никогда 200). Реестр-команды на будущее — PLAN 8.3.
- **M4 (task-05) выполнен, коммит e7b96ef.** docker-compose.yml (image genpass:1.0, 8088:8080, restart unless-stopped, healthcheck через HealthCheck, без устаревшего version:) провалидирован полным циклом: up → healthy → curl :8088 → docker restart → снова healthy → down. README.md (RU): быстрый старт, API + формат ответа, Docker/multi-arch (binfmt-подсказка), деплой на Synology (архитектура → выбор tar → импорт Образ → Проект → compose), reverse-proxy/HTTPS, таблица tar↔NAS.
- **M5 (task-06) выполнен, коммиты e7b96ef→15bc137.** Финальная приёмка свежим ревью-агентом (эмпирически): критерии 1–8 и 10 PASS, критерий 9 (деплой на NAS) — за пользователем; security S1–S4 чисто (System.out только стартовая строка App; java.util.Random/currentTimeMillis — 0; внешних URL нет; tar/логи в git не попадали). Находки: Major — офлайн-tar без RepoTags (после docker load нет тега genpass:1.0, compose сломался бы) → все три tar пересобраны с -t genpass:1.0 (RepoTags подтверждён, контрольный docker load + run = healthy, внутри приложения 1.0.0); Minor — pom 1.0-SNAPSHOT → 1.0.0 (+fallback App.VERSION); Nit — опечатка README. Итог: релиз 1.0.0, 94 теста зелёные, 8 коммитов 525b7e3→15bc137. ОСТАЁТСЯ ЗА ПОЛЬЗОВАТЕЛЕМ: деплой на NAS по README (критерий 9), подтверждение приёмки, решение по git-тегу v1.0.0.
