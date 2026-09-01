# Task 05: M4 — docker-compose и деплой на Synology NAS

**Type:** Code Modification + Verification

## Goal

Подготовить docker-compose.yml и полную инструкцию деплоя на Synology NAS (Container
Manager, reverse-proxy/HTTPS), выполнить деплой на целевом NAS.

## What to Do

- Создать `docker-compose.yml` в корне репо по разделу 9.1 PLAN.md:
  `restart: unless-stopped`, `ports: "8088:8080"`, `environment: PORT=8080`,
  `container_name: genpass`, `healthcheck` (java HealthCheck).
  Без устаревшего ключа `version:`.
- Написать `README.md` (RU) с разделами:
  - О проекте (2–3 предложения, state-less, офлайн).
  - Локальная сборка и запуск (`mvn clean verify`, `java -jar`), переменные PORT/HOST.
  - Docker: сборка образа, multi-arch buildx (раздел 8.2 PLAN.md).
  - Деплой на Synology: определение архитектуры NAS (`uname -m` / DSM → Центр управления →
    Общие сведения), два пути доставки образа (pull из registry / импорт .tar через
    Container Manager → Образ → Импорт), создание проекта (Container Manager → Проект →
    Создать → путь `/volume1/docker/genpass` → вставить compose).
  - Reverse proxy / HTTPS: DSM → Панель управления → Порт входа → Дополнительно →
    Обратный прокси (`https://pass.<домен>` → `http://localhost:8088`), сертификат
    Let's Encrypt; заметка про navigator.clipboard и secure context.
- Выполнить деплой на целевом NAS (или, если недоступен, подготовить и согласовать
  с пользователем пошаговый чек-лист): проект в Container Manager, статус healthy,
  `http://<NAS_IP>:8088` открывается, генерация всех трёх типов работает.
- Проверить restart-политику: контейнер поднимается после остановки/перезагрузки.

## Files/Areas

- `docker-compose.yml` — создать в корне
- `README.md` — создать/дополнить

## Key Points

- Порт на хосте 8088 выбран, чтобы не конфликтовать с 8080 (часто занят на DSM).
- Не публикуйте в compose секретов — их здесь нет по архитектуре.
- Если NAS на armv7 (32-bit) — СТОП: вернуться к пользователю, отдельное решение (риски PLAN.md).
- Кнопка «Копировать» по HTTP работает через fallback; полный функционал — после HTTPS.

## Done When

- [ ] `docker-compose.yml` создан и соответствует разделу 9.1 PLAN.md
- [ ] README.md покрывает сборку, Docker, multi-arch, деплой на NAS, reverse proxy
- [ ] Контейнер развёрнут на NAS (или пользователь подтвердил готовность инструкции к деплою)
- [ ] `http://<NAS_IP>:8088` отвечает, health=healthy, restart-политика активна
