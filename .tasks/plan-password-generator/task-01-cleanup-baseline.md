# Task 01: M0 — Чистка pom.xml и починка тестового каркаса

**Type:** Code Modification

## Goal

Привести сборку в зелёное состояние: исправить scope-ошибки зависимостей, починить
TestNG-suite, подключить сборку fat-jar. Это базовая линия для всех последующих задач.

## What to Do

- В `pom.xml`: задать `<scope>test</scope>` для `org.testng:testng` и всех трёх артефактов
  `io.rest-assured` (`rest-assured`, `json-path`, `json-schema-validator`; у последнего
  убрать `<scope>compile</scope>`).
- Удалить зависимость `com.google.code.gson:gson` (дубль Jackson-databind).
- Добавить `maven-shade-plugin` (goal `shade` на `package`): `Main-Class: org.example.jenpass.App`,
  `finalName` → `genpass` (jar без `-1.0-SNAPSHOT`).
- Создать пакет `org.example.jenpass` с классом `App` (main печатает имя/версию и завершается),
  пустые подпакеты `core`, `web` (пакеты создать классами-заглушками или package-info).
- Удалить `src/test/java/resources/testNG.xml` (чужой «Booker Test Suite», ссылается на
  несуществующие классы) и создать валидный `src/test/resources/testNG.xml` — путь, который
  уже указан в конфигурации surefire.
- Добавить минимальный smoke-тест TestNG (например, `AppTest.versionIsDefined`).

## Files/Areas

- `pom.xml` — scope зависимостей, удаление gson, shade-плагин
- `src/test/java/resources/testNG.xml` — удалить
- `src/test/resources/testNG.xml` — создать (suite: AppTest)
- `src/main/java/org/example/jenpass/App.java` — создать
- `src/test/java/org/example/jenpass/AppTest.java` — создать

## Key Points

- Не добавляйте новых библиотек: всё нужное (testng, rest-assured, jackson, lombok, slf4j) уже в pom.
- После правок проверьте `mvn -B clean verify` и `mvn dependency:tree -Dscope=compile` —
  в compile-scope не должно остаться testng/rest-assured/gson.
- Shade-плагин: включите `Filters` на исключение сигнатур JAR (или `minimizeJar` не использовать,
  чтобы не выкинуть нужное).

## Done When

- [ ] `mvn clean verify` завершается SUCCESS (suite `src/test/resources/testNG.xml` находится и проходит)
- [ ] В `mvn dependency:tree` нет testng/rest-assured/gson в compile scope
- [ ] `target/genpass.jar` создаётся, `java -jar target/genpass.jar` печатает версию и выходит
- [ ] Файл `src/test/java/resources/testNG.xml` удалён
