# Palma backend

Python 3.12, FastAPI, SQLite, uvicorn. Без Docker и без Postgres.

## Установка

```sh
cd backend
uv venv -p 3.12 .venv
uv pip install -p .venv/bin/python -r requirements.txt
```

Без `uv`: `python3.12 -m venv .venv && .venv/bin/pip install -r requirements.txt`.

## Настройка

```sh
cp .env.example .env
.venv/bin/python -m app.keygen
```

Вывод `keygen` (`PALMA_SIGNING_KEY=...`) вставить в `.env`. В `PALMA_ADMIN_TOKEN` записать длинную
случайную строку, например из `openssl rand -hex 32`. Если `PALMA_ADMIN_TOKEN` пустой, админские
маршруты отвечают `403` всем.

| Переменная | Назначение |
|---|---|
| `PALMA_ADMIN_TOKEN` | значение заголовка `X-Admin-Token` |
| `PALMA_SIGNING_KEY` | seed ed25519, 32 байта в base64. Без него сервер не стартует |
| `RUSTORE_STUB` | `1` — проверка покупок заглушкой. Иначе `/api/sub/verify` отвечает `503 store_unavailable` |
| `PALMA_DATA_DIR` | где лежат `palma.sqlite3`, `images/`, `home/` |
| `PALMA_HOST`, `PALMA_PORT` | адрес uvicorn |
| `PALMA_PUBLIC_URL` | префикс для `url` в манифесте, по умолчанию путь относительный |
| `PALMA_PRODUCT_PRO`, `PALMA_PRODUCT_LIFETIME` | `product_id` в RuStore для Pro и Lifetime |
| `PALMA_PRO_PERIOD_DAYS` | оплаченный период, который выдаёт заглушка |
| `PALMA_HOME_MAX_BYTES`, `PALMA_IMAGE_MAX_BYTES` | лимиты загрузки, сверх них `413` |

## Запуск

```sh
cd backend
set -a; . ./.env; set +a; .venv/bin/python -m app
```

Админский экран: `http://127.0.0.1:8000/admin`.

## Проверка

```sh
cd backend
.venv/bin/python -m pytest -q
.venv/bin/ruff check . --exclude .venv
.venv/bin/ruff format --check . --exclude .venv
```

Ручная проверка на запущенном сервере:

```sh
curl -s localhost:8000/api/health
curl -si localhost:8000/api/manifest | head -n 12
curl -s -H "X-Admin-Token: $PALMA_ADMIN_TOKEN" \
  -F id=alpine -F name=Alpine -F distro=alpine -F desktop=none -F min_ram_mb=1024 -F pro=false \
  -F file=@alpine.tar.xz localhost:8000/api/admin/images
curl -s -C - -o alpine.tar.xz localhost:8000/api/images/alpine
curl -s -H 'content-type: application/json' -d '{"device_id":"phone-1","name":null}' \
  localhost:8000/api/devices/register
curl -s -H "X-Device-Key: $DEVICE_KEY" 'localhost:8000/api/sub/status?device_id=phone-1'
```

## Контракт

- Все ответы API — JSON `application/json; charset=utf-8`, ошибки — `{"error": "snake_case"}`.
  Файлы образов и бэкапов отдаются как `application/octet-stream`, `/admin` — HTML.
- Манифест подписан: заголовок `X-Palma-Signature` — base64 подписи ed25519 над точными байтами
  тела ответа. Публичный ключ: `GET /api/pubkey` → `{"algorithm": "ed25519", "public_key": base64}`.
  Клиент кэширует тело вместе с подписью; на `304` подпись повторяется в заголовке.
- `generated_at` и `version` меняются только при публикации или снятии образа, поэтому `ETag`
  стабилен между запросами.
- Скачивание: `ETag` — sha256 файла в кавычках, `Range` (`a-b`, `a-`, `-n`), `If-Range` для
  безопасной докачки, `416` с `Content-Range: bytes */size` за пределами файла. Несколько диапазонов
  в одном запросе не поддерживаются — отдаётся весь файл `200`.
- Pro-образ: `GET /api/images/{id}?device_id=...`; без действующей подписки `402 subscription_required`.
  Порядок проверок: `404 image_not_found` → `402` → `503 file_unavailable` / `503 file_corrupted`.
- Снятый образ пропадает из манифеста и отвечает `404`, файл остаётся на диске.
- `/api/sub/verify`: `ok*` → `ACTIVE`, `pause*` → `PAUSED` (срок не сдвигается, доступ до конца
  оплаченного периода), `stop*` → `TERMINATED` (tier `free`), иначе `404 purchase_not_found`.
  Покупка, привязанная к другому устройству, — `409 purchase_bound_to_other_device`.
  Неизвестный `product_id` — `422 unknown_product`.
- `/api/devices/register`: `{"device_id", "name"}` → `201 {"device_id", "device_key", "tier", "expires_at"}`.
  Ключ показывается один раз, в базе лежит его sha256. Повтор без верного `X-Device-Key` —
  `409 device_exists`, с верным — `200` без ключа.
- `X-Device-Key` обязателен для `/api/sub/status`, `/api/sub/verify`, `/api/home/{device_id}`.
  Нет ключа, неверный, мусорный или от другого устройства — `403 forbidden`. Наличие чужого бэкапа
  не раскрывается. Порядок: `422 invalid_device_id` → `403` → остальное.
- `/api/home/{device_id}`: `POST` multipart с полем `archive`, нужен Pro или Lifetime (`402`
  иначе), ответ `201 {"sha256", "size_bytes", "created_at"}`. `GET` отдаёт последний архив с `Range`,
  в том числе после окончания подписки.
- `/api/admin/subscribers`: `total` — устройства с хотя бы одной покупкой, `pro`, `lifetime`,
  `expiring_7d` — Pro, у которых срок кончается в ближайшие 7 дней.
- Лог: одна строка на запрос — метод, путь без query, код, время. Тела и заголовки не пишутся.

## Устройство

```
app/
  __main__.py      запуск uvicorn
  main.py          сборка приложения, /admin
  config.py        env → Settings
  db.py            схема SQLite
  files.py         Range, потоковая отдача, запись по sha256
  signing.py       ed25519
  rustore.py       заглушка RuStore
  subscriptions.py tier, привязка покупки, сводка
  devices.py       ключ устройства, проверка X-Device-Key
  routes/          catalog, devices, subs, home, admin
admin/index.html   админский экран, шрифт Inter в admin/fonts (OFL)
tests/             pytest
```
