# Task 02: M1 — Ядро генерации + unit-тесты TestNG

**Type:** Code Modification

## Goal

Реализовать зависимое только от JDK ядро генерации (пароль, passphrase, PIN, оценка
стойкости) и покрыть его unit-тестами TestNG согласно разделу 7.1 плана PLAN.md.

## What to Do

- `core.CryptoRandom` — обёртка над `java.security.SecureRandom`: `nextInt(bound)`, `shuffle(char[])`.
- `core.CharGroups` — константы: LOWER `a-z`, UPPER `A-Z`, DIGITS `0-9`,
  SPECIAL `!@#$%^&*()-_=+[]{};:,.<>?/~`, AMBIGUOUS `{I,l,1,O,0,|}`.
- `core.PasswordOptions` / `core.PassphraseOptions` — records с валидацией
  (границы: длина пароля 4–128, слов 3–12, PIN 3–12; хотя бы один набор включён;
  иначе `IllegalArgumentException`).
- `core.PasswordGenerator` — гарантия ≥1 символа каждого включённого набора
  (заполнение → добор → перемешивание Фишера–Йетса через CryptoRandom), `excludeAmbiguous`.
- `core.PinGenerator` — цифры, опция «без ведущих нулей».
- `core.Wordlist` — загрузка `src/main/resources/wordlists/eff-short.txt` (EFF short
  wordlist ~1296 слов, public domain; скачать и положить в ресурсы, UTF-8).
- `core.PassphraseGenerator` — количество слов, разделитель (`-`/`_`/пробел/пусто),
  капитализация, добавление цифры.
- `core.StrengthEstimator` — энтропия `length × log2(alphabet)` (для passphrase
  `words × log2(dictSize)` + 3.32 бита за цифру), метки: <40 слабый / 40–59 средний /
  60–79 сильный / ≥80 очень сильный; время перебора при 10^10 попыток/сек.
- Unit-тесты TestNG в `src/test/java/org/example/jenpass/core/` по разделу 7.1 PLAN.md:
  границы длин, принадлежность алфавиту, исключение неоднозначных, гарантия покрытия,
  статистическая равномерность (100k генераций, допуск ±20% или хи-квадрат),
  энтропия точечными значениями (8 строчных ≈ 37.6 бит; 5 слов из 1296 ≈ 51.65 бит),
  UTF-8 словарь.
- Зарегистрировать тестовые классы в `src/test/resources/testNG.xml`.

## Files/Areas

- `src/main/java/org/example/jenpass/core/*` — 9 классов
- `src/main/resources/wordlists/eff-short.txt` — словарь
- `src/test/java/org/example/jenpass/core/*` — тесты
- `src/test/resources/testNG.xml` — добавить классы в suite

## Key Points

- Ядро НЕ импортирует `com.sun.net.httpserver`, Jackson, классы `web` — только JDK
  (и, при необходимости, Lombok для records-бойлерплейта).
- Случайность ТОЛЬКО `SecureRandom` через `CryptoRandom`; никакой `Random`/`Math.random()`.
- Для статистических тестов НЕ подменяйте SecureRandom сидом — используйте допуски,
  устойчивые к случайным флагам (chi-square с p>0.001 или ±20% от равномерного).

## Done When

- [ ] Все классы ядра реализованы, валидация границ работает
- [ ] `mvn test` зелёный, все тесты из раздела 7.1 PLAN.md написаны и проходят
- [ ] В коде `core` нет импортов веб-слоя/Jackson (проверено ревью/поиском)
- [ ] Словарь загружается из classpath (тест подтверждает ≈1296 слов, UTF-8)
