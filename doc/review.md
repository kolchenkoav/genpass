## REVIEW SUMMARY:
Issue 1:
- **Title:** Добавить проверку на пустой набор после filterAmbiguous
- **Type:** Bug
- **Severity:** Weak Warning
- **Location:** src/main/java/org/example/genpass/core/PasswordGenerator.java:47-62
- **Description:** addIf/filterAmbiguous добавляют отфильтрованный набор без проверки на пустоту. С текущими CharGroups это невозможно (нижн. 25, верхн. 24, цифры 8, спец без изменений), но стоит добавить guard с понятным сообщением, иначе будущая правка SPECIAL (например добавление '|') превратит 500-ю в необъяснимую 'bound must be positive'.

Issue 2:
- **Title:** HealthCheck хардкодит localhost при настраиваемом HOST
- **Type:** Bug
- **Severity:** Weak Warning
- **Location:** src/main/java/org/example/genpass/web/HealthCheck.java:19-24
- **Description:** Сервер биндится на HOST из env (по умолчанию 0.0.0.0), а healthcheck всегда ходит на localhost. Если HOST задать конкретным не-loopback адресом, Docker HEALTHCHECK будет всегда failing. Нужно учитывать HOST (для 0.0.0.0 заменять на 127.0.0.1) либо задокументировать ограничение.

Issue 3:
- **Title:** crackTime: насыщение Math.round для больших энтропий
- **Type:** Bug
- **Severity:** Weak Warning
- **Location:** src/main/java/org/example/genpass/core/StrengthEstimator.java:49-66
- **Description:** Для длинных паролей (например 128 симв. ≈ 837 бит) years ~1e244, Math.round(double) насыщается до Long.MAX_VALUE и UI показывает '~9223372036854775807 лет'. Стоит ввести потолок вида '>10^N лет' при переполнении.

Issue 4:
- **Title:** Deprecated disable(MapperFeature) в ObjectMapper
- **Type:** Best Practice
- **Severity:** Weak Warning
- **Location:** src/main/java/org/example/genpass/web/ApiHandlers.java:27-29
- **Description:** IDE фиксирует deprecation: конфигурирование MapperFeature через ObjectMapper.disable() устарело и в будущем может игнорироваться. Перейти на JsonMapper.builder().disable(...).build(). Сейчас поведение верное (stringLengthRejected/floatLengthRejected зелёные).

Issue 5:
- **Title:** health() не обрабатывает RuntimeException в 500
- **Type:** Best Practice
- **Severity:** Weak Warning
- **Location:** src/main/java/org/example/genpass/web/ApiHandlers.java:34-50
- **Description:** handlePost на RuntimeException отвечает 500 JSON, а health() при RuntimeException от writeJson просто роняет соединение без ответа. Стоит добавить тот же catch (RuntimeException e) → 500 для симметрии.

Issue 6:
- **Title:** WebServer.stop: нет awaitTermination перед выходом
- **Type:** Concurrency
- **Severity:** Weak Warning
- **Location:** src/main/java/org/example/genpass/web/WebServer.java:35-38
- **Description:** server.stop(1) ждёт обмены до 1 сек, но executor.shutdown() не дожидается задач: в shutdown-hook JVM завершится и убьёт работающие потоки, обрывая ответы на середине. Добавить executor.awaitTermination(1, SECONDS) после shutdown().

Issue 7:
- **Title:** Дублирующий throws в readBody
- **Type:** Maintainability
- **Severity:** Info
- **Location:** src/main/java/org/example/genpass/web/ApiHandlers.java:118
- **Description:** RequestBodyTooLargeException extends IOException, поэтому в 'throws IOException, RequestBodyTooLargeException' подтип избыточен (IDE: DuplicateThrows). Оставить только IOException — catch-порядок корректен и не изменится.

Issue 8:
- **Title:** Неиспользуемая зависимость Lombok
- **Type:** Maintainability
- **Severity:** Info
- **Location:** pom.xml:63-68
- **Description:** DTO реализованы record-ами, Lombok нигде не используется. Мёртвая provided-зависимость увеличивает шум аудита — убрать из pom.xml.

## SUMMARY
Ревью пакета org.example.genpass (18 Java-файлов: core — генераторы/энтропия, web — HttpServer/JSON API/статика). Критических ошибок не найдено: криптография корректна (SecureRandom, без modulo bias, Фишер–Йетс), заголовки безопасности (no-store/CSP/nosniff) на всех ответах, строгая Jackson-коэрсия, whitelist статики закрывает path traversal, лимит тела 64 КБ. В сессии прогнаны тесты: ApiIntegrationTest 19/19, PasswordGeneratorTest 5/5, PassphraseGeneratorTest 6/6, StrengthEstimatorTest 5/5, OptionsValidationTest 13/13 — все зелёные (AppTest, PasswordStatisticsTest, PinGeneratorTest, WordlistTest не запускались, их логика проверена чтением кода и интеграционными тестами). Найдено 6 WeakWarning и 2 Info: латентный крэш при пустом наборе после фильтра AMBIGUOUS, healthcheck на hardcoded localhost, насыщение Math.round в crackTime, deprecated-API Jackson, асимметрия обработки ошибок в health(), отсутствие awaitTermination в stop(), дубликат throws и мёртвая зависимость Lombok. Все требования PLAN.md (2.1–2.4, 3.1–3.8, 5.3) выполнены.