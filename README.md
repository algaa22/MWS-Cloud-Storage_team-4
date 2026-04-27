# MWS Cloud Storage ☁️

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

**MWS Cloud Storage** — это полнофункциональное облачное хранилище файлов, разработанное на Java.
Проект представляет собой альтернативу таким сервисам, как Google Drive или Яндекс.Диск,
предлагающее разработчикам простой API, а пользователям — гибкую организацию файлов через
теги и надёжное хранилище.

![Главный интерфейс MWS Cloud Storage](screenshots/main-interface.png)
*Главная страница веб-интерфейса*

---

## 🚀 Установка и запуск

Проект состоит из двух частей: **backend** (Java) и **frontend** (React)

1. **Клонируйте репозиторий**

``` bash
git clone https://github.com/algaa22/MWS-Cloud-Storage_team-4.git
cd MWS-Cloud-Storage_team-4
```

2. **Создайте файл с переменными окружения `.env`**

Для запуска через `docker-compose`: в корневой папке проекта создайте `.env`.

Например:

```bash
DB_URL=jdbc:postgresql://postgres:5432/cloud_storage_db
DB_USERNAME=postgres
DB_PASSWORD=super_secret_password_123

POSTGRES_DB=cloud_storage_db
POSTGRES_USER=postgres
POSTGRES_PASSWORD=super_secret_password_123

S3_URL=http://seaweedfs:8333
S3_ACCESS_KEY=admin_user
S3_SECRET_KEY=super_secret_password_123
S3_REGION=eu-central-1
S3_BUCKET_NAME=user-data

JWT_SECRET_KEY=d8f4a9c3e7b2f6a1d9e4c8b3f7a2e5d1f9c6b4e8a3d7f2c9e1b5f8d3a6c9e4b7
```

3. **Настройте конфигурационный файл `src/main/resources/config.yml`**
4. **Соберите docker-образ**

В корневой папке проекта запустите:

``` bash
docker build -t cloud-storage-app .
```

5. **Запустите всю инфраструктуру через `docker-compose`**

```bash
docker-compose up -d
```

6. (Дополнительно) **Запустите `frontend` вручную** (для разработки)

В папке `cloud-storage-frontend` введите:

``` bash
npm run dev
```

---

## 🔐 Настройка HTTPS

Если вы планируете запустить сервер на HTTPS, то перед сборкой добавьте ваши SSL-сертификаты `server.crt`, `server.csr`,
`server.key`, `server.p12` в папке проекта `src/main/resources/ssl`

---

## ✨ Возможности

#### 📁 Работа с файлами

- **Загрузка и скачивание любого размера** — система позволяет использовать чанковую передачу для файлов больших
  размеров

- **Полноценная файловая система** — создание, перемещение, переименование и удаление папок и файлов через удобный
  веб-интерфейс

- **Гибкая организация** — поддержка системы тегов для категоризации файлов помимо классической структуры папок

- **Контроль доступа** — настраиваемая видимость файлов (приватные, доступные по ссылке, публичные)

*Техническая деталь:* для соответствия стандартам S3-совместимых хранилищ чанковая передача используется для файлов с
размером от `5 МБ`

#### 🔐 Безопасность и учётные записи

- **Полный цикл аутентификации** — регистрация, вход и выход

- **Надёжные сессии** — JWT-токены с автоматической ротацией через refresh-токены

- **Управление профилем** — изменение имени пользователя и пароля

**🌐 Веб-интерфейс**

- **Удобный веб-клиент** — интуитивный интерфейс для управления файлами без необходимости использовать API напрямую

---

## 🏗️ Архитектура

Система состоит из **независимых модулей**, каждый из которых отвечает за свою задачу. Пользователь может работать как
через **веб-интерфейс**, так и напрямую через **API**.

```mermaid
graph TD
    A[Веб-интерфейс React] --> B[REST API]
    C[Сторонние клиенты] --> B
    B --> D[Backend на Java]
    D --> E[(PostgreSQL<br/>Метаданные)]
    D --> F[(S3<br/>Файлы-объекты)]
```

#### Ключевые архитектурные решения:

1. **Разделение логики и данных**

    - **PostgreSQL** хранит всю **логическую структуру**: пользователей, виртуальные пути файлов, теги, права доступа

    - **SeaweedFS (S3-совместимое)** хранит **сами файлы** как объекты. Это обеспечивает отказоустойчивость и эффективность
      для
      больших бинарных данных

2. **Двухуровневая система путей**

    - **Для пользователя**: привычные пути (`documents/report.pdf`)

    - **Для системы**: в SeaweedFS файл хранится по ключу`user_id/file_id`, что идеально соответствует объектной модели S3 и
      обеспечивает высокую производительность

3. **Единая точка входа**

    - Весь доступ к системе происходит **только через REST API** нашего backend

4. **Контейнеризация**

    - Каждый компонент (Backend, PostgreSQL, SeaweedFS) работает в **отдельном Docker-контейнере**. Это гарантирует
      идентичность окружений на всех машинах и упрощает развёртывание одной командой (`docker-compose up`)

---

## 🛠 Технологии

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Netty](https://img.shields.io/badge/Netty-6DB33F?style=for-the-badge&logo=netty&logoColor=white)
![JWT](https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=JSON%20web%20tokens&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white)
![SeaweedFS](https://img.shields.io/badge/SeaweedFS-41B883?style=for-the-badge&logo=files&logoColor=white)
![React](https://img.shields.io/badge/React-20232A?style=for-the-badge&logo=react&logoColor=61DAFB)
![JavaScript](https://img.shields.io/badge/JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)
![CSS](https://img.shields.io/badge/CSS-1572B6?style=for-the-badge&logo=css&logoColor=white)

---

## 📖 Документация API

Все запросы к API начинаются с префикса `/api`. Почти все операции (кроме register/login/refresh) требуют заголовок:
`X-Auth-Token: <access_token>`

Опциональные параметры или заголовки обозначены через `*` 

#### 1. Пользователи и Аутентификация (`/api/users`)

| Endpoint | Method | Headers                                              | Description |
| --- | --- |------------------------------------------------------| --- |
| `/auth/register` | `POST` | `X-Auth-Email`, `X-Auth-Password`, `X-Auth-Username` | Регистрация нового аккаунта |
| `/auth/login` | `POST` | `X-Auth-Email`, `X-Auth-Password`                    | Вход и получение пары токенов |
| `/auth/logout` | `POST` | `X-Auth-Token`                                       | Инвалидация текущей сессии |
| `/auth/refresh` | `POST` | `X-Refresh-Token`                                    | Обновление Access Token |
| `/info` | `GET` | `X-Auth-Token`                                       | Получение данных текущего профиля |
| `/update` | `POST` | `X-Auth-Token`, `X-New-Username*`, `X-New-Password*` | Смена данных пользователя |

#### 2. Работа с файлами (`/api/files`)

| Endpoint                | Method | Query Params                                              | Headers                  | Description                                |
|-------------------------| --- |-----------------------------------------------------------|--------------------------|--------------------------------------------|
| `/list`                 | `GET` | `parentId*`, `recursive*`, `includeDirectories*`, `tags*` | —                        | Список объектов в директории               |
| `/info`                 | `GET` | `id`                                                      | —                        | Детальные метаданные файла                 |
| `/`                     | `PUT` | `id`, `newName*`, `newParentId*`                          | `X-File-New-Visibility*` | Изменение метаданных                       |
| `/`                     | `DELETE` | `id`, `permanent*`                                         | —                        | Удаление файла (soft/hard)                 |
| `/download`             | `GET` | `id`                                                      | —                        | Чанковое скачивание                        |
| `/upload`               | `POST` | `name`, `parentId*`                                       | `X-File-Tags`            | Обычная загрузка                           |
| `/upload/chunked`       | `POST` | `name`, `parentId*`                                       | `X-File-Tags`, `X-File-Size` | Чанковая загрузка                          |
| `/upload/chunked/resume` | `POST` | `name`, `parentId*`                                       |                          | Возобновление прерванной чанковой загрузки |

#### 3. Работа с директориями (`/api/directories`)

| Endpoint | Method | Query Params                     | Description |
| --- | --- |----------------------------------| --- |
| `/` | `PUT` | `name`, `parentId*`              | Создать новую папку |
| `/` | `POST` | `id`, `newName*`, `newParentId*` | Переименовать или переместить папку |
| `/` | `DELETE` | `id`                             | Рекурсивное удаление папки и всего содержимого |

### 📦 Формат ответов

Большинство успешных операций возвращают JSON следующего вида:

```json
{
  "success": true,
  "message": "Операция выполнена",
  "data": { ... } 
}

```

### 🔄 Чанковая передача данных

Для работы с файлами сервер использует динамическое переключение стратегии обработки трафика в `ChannelPipeline`.

#### Загрузка (Upload)

Если **X-File-Size** во входящем запросе превышает лимит **5 МБ** (`maxAggregatedContentLength`), система переключается с агрегированной обработки в памяти на чанковую.

Если возникла временная ошибка записи, сервер вернет статус `409 Conflict` с полем `action` в теле:

* `RETRY_PART`: Ошибка записи чанка. Повторите отправку с той части, что указана в полях `currentFileSize` и `partNum`.
* `RETRY_COMPLETE`: Ошибка при сборке файла. Повторите финальный `LastHttpContent`.

#### Скачивание (Download)

Запросы на `GET /api/files/download` **всегда** обрабатываются в потоковом режиме (`CHUNKED`), независимо от размера файла.

---

## 🗺️ Roadmap

Мы активно развиваем проект и планируем реализовать следующие функции и улучшения:

- **Система уровней доступа** к файлам и папкам с гибкими правилами для совместной работы

- **Расширенный поиск по файлам** для полнотекстового поиска и фильтрации

- **Усиление безопасности**: защита от инъекций, улучшение валидации и прочее

- **Готовый фронтенд** с полировкой UI/UX, адаптивным дизайном и улучшенной производительностью

---

## 👥 Команда / Контакты

**Команда:**

* **Сергей** @seregawallapop — Архитектура & Сетевой слой & API

* **Анастасия** @alg_aaa — Бизнес логика & Веб-интерфейс

* **Павел** @PavelStar899 — Слой данных

**Обратная связь:**  
По всем вопросам, связанным с проектом, пожалуйста, создавайте **Issue** в репозитории проекта на GitHub.

_© 2025-2026 Команда MWS Cloud Storage. Все права защищены._