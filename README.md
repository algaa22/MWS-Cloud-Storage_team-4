# MWS Cloud Storage ☁️

**MWS Cloud Storage** — высокопроизводительное распределенное хранилище, построенное на кастомном Netty-ядре. Проект реализует сложную логику управления состояниями файлов, эшелонированную проверку безопасности и отказоустойчивую чанковую загрузку.

## 🚀 Быстрый старт

### 1. Подготовка репозитория

Склонируйте основной проект и все зависимые микросервисы (антивирус и уведомления) одной командой:

```bash
git clone --recursive https://github.com/algaa22/MWS-Cloud-Storage_team-4.git
cd MWS-Cloud-Storage_team-4

```

### 2. Конфигурация окружения

1. **SSL:** Поместите `server.crt`, `server.key`, `server.p12` в `src/main/resources/ssl`.
2. **S3:** Создайте `/etc/seaweedfs/s3.json` (см. `s3.json.example`).
3. **Environment:** Создайте `.env` в корне:

```env
# Database
DB_URL=jdbc:postgresql://postgres:5432/cloud_storage_db
DB_USERNAME=postgres
DB_NAME=cloud_storage_db
DB_PASSWORD=super_secret_password_123

# Storage (SeaweedFS)
S3_URL=https://cloud-storage-seaweed:8333
S3_ACCESS_KEY=seaweedfs
S3_SECRET_KEY=seaweedfs
S3_REGION=eu-central-1
S3_BUCKET_NAME=user-data

# JWT
JWT_SECRET_KEY=d8f4a9c3e7b2f6a1d9e4c8b3f7a2e5d1f9c6b4e8a3d7f2c9e1b5f8d3a6c9e4b7

# Notifications Service
NOTIFICATION_MAIL=your_email@gmail.com
NOTIFICATION_PASSWORD=your_password

# RabbitMQ
RABBITMQ_HOST=cloud-storage-mq
RABBITMQ_PORT=5672
RABBITMQ_USER=guest
RABBITMQ_PASS=guest

```

### 3. Запуск

```bash
docker-compose up --build -d

```

После запуска инфраструктура будет доступна по адресу `https://localhost:8080`.

## 🛠 Техническая архитектура

### 🚀 Архитектура на Virtual Threads (Project Loom)
Вместо классической реактивщины мы используем виртуальные потоки. Это позволяет писать императивный, легко читаемый код, который при этом масштабируется так же эффективно, как асинхронный. Все блокирующие операции (запросы к PostgreSQL через JPA, стриминг данных в SeaweedFS) выполняются в легких потоках, не блокируя системные ресурсы Netty.

### 🧩 Инфраструктура маппинга
Чтобы связать сетевое ядро Netty с бизнес-логикой на виртуальных потоках, мы разработали собственную систему декларативного маппинга. 

* **RequestToDtoDecoder & DtoAssembler:** Обеспечивают автоматическую сборку DTO из `HttpRequest`. Поддерживают внедрение `@UserId` из атрибутов канала, извлечение `@QueryParam` и маппинг `@RequestHeader`.
* **Zero-Reflection Invoke:** `RouteRegistry` и `DtoMetadataCache` сканируют и кэшируют метаданные рекордов (DTO) при старте. Это позволяет находить обработчик и инстанцировать объекты без использования рефлексии на каждый запрос.
* **DtoToResponseEncoder:** Выполняет обратную трансформацию Java-рекордов в `FullHttpResponse`.
* **Авто-заголовки:** Поля DTO автоматически конвертируются в HTTP-заголовки (например, `fileSize` $\to$ `X-File-Size`).
* **Декларативная конфигурация:** Логика обработки запросов и ответов полностью управляется через кастомный набор аннотаций в пакете `netty.mapping.annotations`:
  * **Request:** `@RequestMapping`, `@UserId`, `@RequestBody`, `@RequestBodyParam`, `@QueryParam`, `@RequestHeader`, `@NestedDto`.
  * **Response:** `@ResponseStatus`, `@ResponseHeader`, `@ResponseBodyParam`.



### 🎛 Стейт-машина (FSM) и Retry-логика

Жизненный цикл любого файла в системе жестко контролируется через состояния в PostgreSQL, что гарантирует консистентность в распределенной среде:

**Система статусов:**
* **PENDING:** Начало операции, файл заблокирован для других процессов записи.
* **READY:** Файл успешно прошел все проверки и доступен для чтения.
* **ERROR:** Временный сбой, допускающий повторную попытку.
* **FATAL:** Критическая ошибка или превышение лимита `retry_count`.
* **DANGEROUS:** Вердикт антивируса о наличии угроз.


* **Отказоустойчивость (Failsafe):** Логика ретраев инкапсулирована в `StorageRepositoryWrapper`. Для `UPLOAD` и `CHANGE_METADATA` применяется **Client-side retry**, а для `DELETE` — автоматический **Server-side retry** через фоновые задачи.
* **Resumable Upload:** Механизм докачки реализован в виртуальных потоках. При обрыве сессии сервер сохраняет состояние и позволяет клиенту продолжить загрузку с $n$-го чанка, используя `sessionId` и инкрементальное вычисление хеша SHA-256.
* **Background Cleanup:** Шедулер очистки автоматически обрабатывает "зависшие" в `PENDING/ERROR/FATAL` операции, выполняя либо откат (для загрузок), либо принудительное завершение (для удалений).

---

### 🔄 Чанковая передача данных

Система использует динамическое переключение стратегии обработки трафика в `ChannelPipeline` через `HttpTrafficStrategySelector`.

#### Стратегия выбора (Pipeline Selection)

Сервер анализирует входящий `HttpRequest` и на лету перестраивает пайплайн:

* **CHUNKED:** Активируется для эндпоинтов загрузки частей (`/api/files/upload/chunked/part`) и скачивания (`/api/files/download`). В пайплайн добавляется `ChunkedWriteHandler`, а `HttpObjectAggregator` **исключается**, чтобы не перегружать RAM.
* **AGGREGATED:** Используется для всех остальных API-запросов. Весь запрос собирается в `FullHttpRequest` с лимитом **5 МБ** (настраивается в `maxAggregatedContentLength`).

#### Загрузка (Upload & Retry)

Процесс загрузки частей контролируется контроллером `ChunkedUploadController`. В случае возникновения `UploadRetriableException` (например, временный сбой S3 или сети), сервер возвращает специальные Retry-ответы:

* **RETRY_PART (`409 Conflict`):** Ошибка при загрузке конкретного чанка. Клиенту необходимо повторно отправить текущую часть. В ответе передается `partNum`.
* **RETRY_COMPLETE (`409 Conflict`):** Ошибка на этапе финализации (сборки) файла в S3. Клиенту нужно повторно вызвать метод `complete`.

#### Скачивание (Download)

Запросы на скачивание всегда переводят пайплайн в режим **CHUNKED**.

* Используется кастомный `ChunkedDownloadInput` (на базе Project Loom), который читает `InputStream` из хранилища частями (`fileDownloadChunkSize`) и оборачивает их в `DefaultHttpContent`.
* Поддерживаются **Range-запросы** (`206 Partial Content`): сервер отправляет только запрашиваемый диапазон байт, что критично для докачки и стриминга видео.

---

## 📊 Мониторинг и инструменты (Observability)

Система глубоко инструментирована для отслеживания состояния Data Plane и Control Plane.

### 1. Инфраструктурный слой (System Health)

| **Метрика** | **Тип** | **Теги** | **Описание** |
| --- | --- | --- | --- |
| `jvm.memory.used` | Gauge | `area`, `id` | Потребление памяти. Мониторинг Heap и Non-heap. |
| `system.cpu.usage` | Gauge | — | Общая загрузка CPU процессом приложения. |
| `hikaricp.connections` | Gauge | `pool`, `state` | Состояние пула БД: `active` (в работе), `pending` (очередь). |
| `rabbitmq.queue.messages` | Gauge | `queue` | Глубина очередей (антивирус, уведомления, cleanup). |

### 2. Слой Netty-ядра (Network & I/O)

| **Метрика** | **Тип** | **Описание** |
| --- | --- | --- |
| `http.server.requests` | Timer | **RPS** и **Latency** (P95, P99) кастомных контроллеров. |
| `netty.connections.active` | Gauge | Количество активных TCP-сессий (Concurrent Users). |
| `netty.eventloop.executor.tasks.pending` | Gauge | Очередь задач в EventLoop. Индикатор блокирующих вызовов. |
| `netty.allocator.memory.used` | Gauge | Выделенная **Direct Memory**. Рост без падений — признак утечки. |
| `netty.allocator.memory.pinned` | Gauge | Количество "залоченных" буферов (`refCnt > 0`). Индикатор забытых `release()`. |
| `netty.allocator.pools.chunks/pages` | Gauge | Уровень фрагментации пулов памяти Netty. |

### 3. Бизнес-слой (Critical Path & FSM)

| **Метрика** | **Тип** | **Описание**                                                      |
| --- | --- |-------------------------------------------------------------------|
| `storage.operation.duration` | Timer | Чистое время бизнес-операций (UPLOAD, DOWNLOAD, DELETE).          |
| `storage.retry.total` | Counter | Количество ретраев. Рост = нестабильность S3/сети.                |
| `storage.files.status.count` | Gauge | Количественный срез FSM: `PENDING`, `READY`, `FATAL`, `DANGEROUS`. |
| `storage.payload.size.bytes` | Distribution | Гистограмма размеров файлов для оптимизации чанков.               |
| `storage.upload.chunks.active` | Gauge | Активные сессии многопоточной загрузки (Multipart).               |
| `storage.antivirus.scan.count` | Counter | Результаты сканирования: `CLEAN`, `INFECTED`, `ERROR`.            |

---

## 🏗 Технологический стек

### 1. Cloud Storage Backend (Core)

* **Runtime:** Java 21 (Virtual Threads), Spring Boot 3.5.11.
* **Network:** Netty 4.2.
* **Data:** PostgreSQL, Flyway, Spring Data JPA.
* **Storage:** AWS SDK (S3) $\to$ SeaweedFS.
* **Reliability:** Failsafe, RabbitMQ, Redis.

### 2. Antivirus Scanner Service

* **Logic:** ClamAV Client, Apache Tika (MIME-validation).
* **Performance:** Redis (Кэширование вердиктов), S3 SDK.
* **Events:** RabbitMQ.

### 3. Notification Service

* **Engine:** Spring Boot 3.2.4, Jakarta Mail.
* **UI/UX:** Thymeleaf (HTML-шаблоны писем).
* **API:** Spring Web.

---

## 📑 API Endpoints

Все запросы к API начинаются с префикса `/api`. Большинство операций требуют авторизации через JWT-токен в заголовке.

### 📂 Работа с файлами (`/api/files`)

| Метод | Эндпоинт | Описание |
| --- | --- | --- |
| `POST` | `/files/upload` | Синхронная загрузка файлов (до 5 МБ). |
| `GET` | `/files/download` | Скачивание файла в поточном режиме. |
| `GET` | `/files/list` | Получение списка объектов в директории. |
| `GET` | `/files/info` | Метаданные конкретного файла. |
| `GET` | `/files/trash` | Список удаленных объектов в корзине. |
| `POST` | `/files/restore` | Восстановление файла из корзины. |
| `GET` | `/files/preview/content` | Запрос временной ссылки для предпросмотра медиа. |

### 📦 Чанковая загрузка (Resumable Upload)

| Метод | Эндпоинт | Описание |
| --- | --- | --- |
| `POST` | `/files/upload/chunked/start` | Инициализация сессии загрузки. |
| `POST` | `/files/upload/chunked/part` | Передача конкретного фрагмента (чанка) данных. |
| `GET` | `/files/upload/chunked/status` | Опрос состояния: список полученных фрагментов. |
| `POST` | `/files/upload/chunked/complete` | Завершение сборки и запуск антивирусной проверки. |
| `POST` | `/files/upload/chunked/abort` | Принудительная отмена сессии и очистка S3. |

### 📁 Управление директориями (`/api/directories`)

| Метод | Эндпоинт | Описание |
| --- | --- | --- |
| `PUT` | `/directories` | Создание новой виртуальной папки. |

### 🔗 Публичный доступ (Sharing)

| Метод | Эндпоинт | Описание |
| --- | --- | --- |
| `POST` | `/shares` | Создание публичной ссылки на файл/папку. |
| `GET` | `/shares/info` | Получение метаданных расшаренного объекта. |
| `GET` | `/shares/download` | Прямое скачивание объекта по ссылке. |
| `POST` | `/shares/validate` | Валидация пароля для защищенных ссылок. |
| `GET` | `/shares/user` | Список всех активных шар текущего пользователя. |
| `GET` | `/shares/file` | Информация о доступе к конкретному файлу. |

### 👤 Пользователи и Авторизация (`/api/users`)

| Метод | Эндпоинт | Описание |
| --- | --- | --- |
| `POST` | `/users/auth/login` | Аутентификация и получение токенов. |
| `POST` | `/users/auth/register` | Регистрация нового аккаунта. |
| `POST` | `/users/auth/logout` | Деавторизация и инвалидация сессии. |
| `POST` | `/users/auth/refresh` | Обновление пары access/refresh токенов. |
| `GET` | `/users/info` | Данные текущего профиля. |
| `POST` | `/users/update` | Обновление данных (пароль, никнейм). |

### 💳 Биллинг и Платежи (`/api/users/tariff`, `/api/payments`)

| Метод | Эндпоинт | Описание |
| --- | --- | --- |
| `GET` | `/users/tariff/plans` | Просмотр доступных тарифных планов. |
| `GET` | `/users/tariff/info` | Текущий объем хранилища и статус квоты. |
| `POST` | `/users/tariff/purchase` | Покупка подписки. |
| `POST` | `/users/tariff/set-auto-renew` | Управление автопродлением тарифа. |
| `POST` | `/users/tariff/update-payment` | Привязка/обновление платежных данных. |
| `GET` | `/api/payments/history` | История транзакций пользователя. |

---

## 👥 Команда / Контакты

### **Сергей** (@seregawallapop) — **Lead / System Architect**

* **Сетевой стек:** Netty-ядро на виртуальных потоках, кастомный DI/Mapping и докачка файлов.
* **Security & Reliability:** Микросервис антивируса (ClamAV), State-машина транзакций, SSL/BoringSSL и Failsafe-логика.
* **Infrastructure:** Настройка Docker-окружения, CI/CD, мониторинг (Prometheus/Micrometer) и «умный» поиск.

### **Анастасия** (@alg_aaa) — **Business Logic & Frontend**

* **Product:** Тарифные планы, квоты, система уведомлений (SMTP) и безопасный шеринг (ссылки + пароли).
* **Frontend:** React-клиент с пагинацией, превью медиа и управлением чанковыми загрузками.
* **Auth:** Регистрация, авторизация и механизмы ротации JWT-токенов.

### **Павел** (@PavelStar899) — **Data Infrastructure**

* **Storage:** Интеграция SeaweedFS (S3), реализация древовидной структуры и миграции Flyway.
* **Persistence:** Проектирование БД (PostgreSQL), Spring Data JPA и логика Soft/Hard delete.
* **DevOps:** Health Checks системы и автоматическая очистка корзины.

### **Обратная связь:**
По всем вопросам, связанным с проектом, пожалуйста, создавайте **Issue** в репозитории проекта на GitHub.

_© 2025-2026 Команда MWS Cloud Storage. Все права защищены._