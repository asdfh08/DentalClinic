# ER-диаграмма базы данных `dental_clinic`

Информационная система «Запись пациента на стоматологический приём» (КР 1).
СУБД: PostgreSQL 13+. Готовая картинка для вставки в отчёт — `er-diagram.svg`.

## Диаграмма (Mermaid)

```mermaid
erDiagram
    PATIENTS ||--o{ APPOINTMENTS : "записывается на приём"
    DENTISTS ||--o{ APPOINTMENTS : "проводит приём"

    PATIENTS {
        SERIAL id PK "первичный ключ"
        VARCHAR(120) full_name "NOT NULL, CHECK длина >= 3"
        VARCHAR(20) phone "NOT NULL, UNIQUE"
        VARCHAR(120) email "UNIQUE, может быть NULL"
        DATE birth_date "NOT NULL, CHECK <= CURRENT_DATE"
        TIMESTAMP created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }

    DENTISTS {
        SERIAL id PK "первичный ключ"
        VARCHAR(120) full_name "NOT NULL, CHECK длина >= 3"
        VARCHAR(20) phone "NOT NULL, UNIQUE"
        VARCHAR(60) specialization "NOT NULL"
        INTEGER cabinet "NOT NULL, CHECK 1..500"
    }

    APPOINTMENTS {
        SERIAL id PK "первичный ключ"
        INTEGER patient_id FK "NOT NULL, ON DELETE CASCADE"
        INTEGER dentist_id FK "NOT NULL, ON DELETE RESTRICT"
        TIMESTAMP appointment_time "NOT NULL, UNIQUE вместе с dentist_id"
        VARCHAR(30) procedure_type "NOT NULL, CHECK - 7 значений enum"
        VARCHAR(20) status "NOT NULL, CHECK - 5 значений enum"
        NUMERIC price "NOT NULL, CHECK 0..500000"
        VARCHAR(255) complaint "может быть NULL"
        TIMESTAMP created_at "NOT NULL, DEFAULT CURRENT_TIMESTAMP"
    }
```

## Связи

| Связь | Тип | Реализация | Поведение при удалении |
|---|---|---|---|
| `appointments.patient_id` → `patients.id` | один-ко-многим (1:N) | FOREIGN KEY `fk_appointments_patient` | `ON DELETE CASCADE` — вместе с пациентом удаляется история его приёмов |
| `appointments.dentist_id` → `dentists.id` | один-ко-многим (1:N) | FOREIGN KEY `fk_appointments_dentist` | `ON DELETE RESTRICT` — врача с записями удалить нельзя |

Один пациент может иметь много записей на приём; одна запись относится
ровно к одному пациенту и одному врачу. Один врач проводит много приёмов.

## Ограничения целостности

- **PRIMARY KEY** — `patients.id`, `dentists.id`, `appointments.id` (`SERIAL`).
- **FOREIGN KEY** — два внешних ключа таблицы `appointments`.
- **NOT NULL** — все обязательные поля (ФИО, телефон, дата рождения, время приёма, процедура, статус, стоимость).
- **UNIQUE** — `patients.phone`, `patients.email`, `dentists.phone`, а также составное
  `uq_appointments_dentist_slot (dentist_id, appointment_time)`: врач не может быть занят
  двумя приёмами в одну минуту.
- **CHECK** — допустимые значения `status` и `procedure_type` (совпадают с константами
  Java-перечислений `AppointmentStatus` и `ProcedureType`), диапазон стоимости,
  диапазон номера кабинета, минимальная длина ФИО и телефона.
- **Индексы** — по времени приёма, статусу и пациенту для ускорения фильтрации и поиска.

## Соответствие таблиц и классов Java

| Таблица | Класс модели | Репозиторий | Сервис |
|---|---|---|---|
| `patients` | `model/Patient` (наследник `Person`) | `repository/PatientRepository` | `service/PatientService` |
| `dentists` | `model/Dentist` (наследник `Person`) | `repository/DentistRepository` | `service/DentistService` |
| `appointments` | `model/Appointment` | `repository/AppointmentRepository` | `service/AppointmentService` |
| колонка `status` | `model/AppointmentStatus` (enum) | — | — |
| колонка `procedure_type` | `model/ProcedureType` (enum) | — | — |
