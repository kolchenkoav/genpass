# Task 03: M2 — Веб-слой (JDK HttpServer + UI) и интеграционные тесты

**Type:** Code Modification

## Goal

Поднять минимальный HTTP-сервер на `com.sun.net.httpserver` с JSON API, статическим
UI и заголовками безопасности; покрыть интеграционными тестами rest-assured (раздел 7.2).

## What to Do

- `web.WebServer` — HttpServer + `ExecutorService.newFixedThreadPool`, контекст-маршруты:
  `/api/health`, `/api/password`, `/api/passphrase`, `/api/pin`, `/` + статика;
  graceful shutdown (по SIGTERM/hook). Конфиг из env: `PORT` (default 8080), `HOST` (default 0.0.0.0).
- `web.ApiHandlers` — разбор JSON-тела (Jackson DTO) → вызов ядра → ответ
  `{"result": "...", "entropyBits": N, "strength": "..."}`; ошибки валидации → 400 `{"error": ...}`;
  неизвестный путь → 404; некорректный JSON → 400.
- `web.SecurityHeadersFilter` (или хелпер в каждом handler): на ВСЕХ ответах —
  `Cache-Control: no-store` (API) / `no-cache` (статика), CSP
  `default-src 'none'; script-src 'self'; style-src 'self'; connect-src 'self'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'`,
  `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`, `X-Frame-Options: DENY`.
- `web.StaticHandler` — отдаёт из classpath `web/index.html`, `web/app.js`, `web/style.css`
  с правильными Content-Type; без листинга каталогов.
- `web.HealthCheck` — отдельный `main()`: `java.net.http.HttpClient` GET
  `http://localhost:${PORT:-8080}/api/health`, exit 0 при 200, иначе 1 (для Docker HEALTHCHECK).
- UI: `src/main/resources/web/{index.html,app.js,style.css}` — ванильные HTML/JS/CSS,
  русскоязычный; три режима (Пароль / Passphrase / PIN) с контролами из п.2.1–2.3 PLAN.md;
  кнопка «Копировать»: `navigator.clipboard.writeText` с fallback `document.execCommand('copy')`;
  инлайн-скрипты/CSS запрещены (CSP) — только отдельные файлы; никаких внешних URL.
- Интеграционные тесты rest-assured в `src/test/java/org/example/jenpass/web/`:
  `@BeforeSuite` старт WebServer на порту `ServerSocket(0)`; проверки из раздела 7.2 PLAN.md
  (health 200; три POST-эндпоинта 200 + схема ответа; заголовки no-store/CSP/nosniff;
  400-сценарии; 404; `GET /` отдаёт HTML). Добавить классы в `src/test/resources/testNG.xml`.
- Обновить `App.main()`: запуск `WebServer`, печать в stdout только порт/версию.

## Files/Areas

- `src/main/java/org/example/jenpass/web/*` — WebServer, ApiHandlers, StaticHandler, SecurityHeaders, HealthCheck, DTO
- `src/main/java/org/example/jenpass/App.java` — запуск сервера
- `src/main/resources/web/*` — index.html, app.js, style.css
- `src/test/java/org/example/jenpass/web/*` — интеграционные тесты
- `src/test/resources/testNG.xml` — добавить suite-классы

## Key Points

- Зависимости: только `web → core`; ядро не трогаем и не меняем.
- Результат генерации НИКОГДА не попадает в лог/stdout — проверьте после реализации.
- HttpServer: ответ обязан закрываться (`exchange.close()` в finally), иначе утечки дескрипторов.
- Форма UI шлёт POST с JSON — убедитесь, что fetch использует same-origin и `cache: 'no-store'`.

## Done When

- [ ] `mvn clean verify` зелёный (unit + интеграционные)
- [ ] `java -jar target/genpass.jar`: `GET /api/health` → 200; `GET /` отдаёт UI
- [ ] POST /api/password|passphrase|pin возвращают корректные JSON-ответы (проверено тестами)
- [ ] Заголовки `Cache-Control: no-store`, CSP, `X-Content-Type-Options: nosniff` — на всех API-ответах (тесты)
- [ ] UI работает в браузере: генерация трёх типов, копирование в буфер (localhost)
- [ ] 400 на некорректные параметры, 404 на неизвестный путь (тесты)
