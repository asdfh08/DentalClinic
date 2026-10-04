-- =====================================================================
--  КР 1. Информационная система "Запись пациента на стоматологический приём"
--  СУБД: PostgreSQL 13+
--
--  Порядок запуска:
--    1) createdb -U postgres dental_clinic
--       (или в psql: CREATE DATABASE dental_clinic ENCODING 'UTF8';)
--    2) psql -U postgres -d dental_clinic -f db/schema_postgresql.sql
-- =====================================================================

DROP TABLE IF EXISTS appointments CASCADE;
DROP TABLE IF EXISTS patients CASCADE;
DROP TABLE IF EXISTS dentists CASCADE;

-- ---------------------------------------------------------------------
-- Таблица 1: пациенты (участник предметной области)
-- ---------------------------------------------------------------------
CREATE TABLE patients
(
    id         SERIAL PRIMARY KEY,
    full_name  VARCHAR(120) NOT NULL,
    phone      VARCHAR(20)  NOT NULL UNIQUE,
    email      VARCHAR(120) UNIQUE,
    birth_date DATE         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_patients_name_length CHECK (char_length(trim(full_name)) >= 3),
    CONSTRAINT chk_patients_phone_length CHECK (char_length(phone) >= 10),
    CONSTRAINT chk_patients_birth_date CHECK (birth_date <= CURRENT_DATE)
);

-- ---------------------------------------------------------------------
-- Таблица 2: врачи-стоматологи
-- ---------------------------------------------------------------------
CREATE TABLE dentists
(
    id             SERIAL PRIMARY KEY,
    full_name      VARCHAR(120) NOT NULL,
    phone          VARCHAR(20)  NOT NULL UNIQUE,
    specialization VARCHAR(60)  NOT NULL,
    cabinet        INTEGER      NOT NULL,

    CONSTRAINT chk_dentists_name_length CHECK (char_length(trim(full_name)) >= 3),
    CONSTRAINT chk_dentists_cabinet CHECK (cabinet BETWEEN 1 AND 500)
);

-- ---------------------------------------------------------------------
-- Таблица 3: ОСНОВНАЯ СУЩНОСТЬ — записи пациентов на приём
--   appointments.patient_id -> patients.id   (ON DELETE CASCADE)
--   appointments.dentist_id -> dentists.id   (ON DELETE RESTRICT)
-- ---------------------------------------------------------------------
CREATE TABLE appointments
(
    id               SERIAL PRIMARY KEY,
    patient_id       INTEGER        NOT NULL,
    dentist_id       INTEGER        NOT NULL,
    appointment_time TIMESTAMP      NOT NULL,
    procedure_type   VARCHAR(30)    NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'CREATED',
    price            NUMERIC(10, 2) NOT NULL,
    complaint        VARCHAR(255),
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_appointments_dentist FOREIGN KEY (dentist_id) REFERENCES dentists (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- один врач не может быть занят двумя приёмами в одну и ту же минуту
    CONSTRAINT uq_appointments_dentist_slot UNIQUE (dentist_id, appointment_time),

    -- ограничения допустимых значений: списки совпадают с enum в Java-коде
    CONSTRAINT chk_appointments_status CHECK (status IN
                                              ('CREATED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    CONSTRAINT chk_appointments_procedure CHECK (procedure_type IN
                                                 ('CONSULTATION', 'HYGIENE', 'CARIES_TREATMENT', 'ROOT_CANAL',
                                                  'EXTRACTION', 'IMPLANTATION', 'BRACES')),
    CONSTRAINT chk_appointments_price CHECK (price >= 0 AND price <= 500000)
);

CREATE INDEX idx_appointments_time ON appointments (appointment_time);
CREATE INDEX idx_appointments_status ON appointments (status);
CREATE INDEX idx_appointments_patient ON appointments (patient_id);

COMMENT ON TABLE patients IS 'Пациенты стоматологической клиники';
COMMENT ON TABLE dentists IS 'Врачи-стоматологи';
COMMENT ON TABLE appointments IS 'Записи пациентов на стоматологический приём (основная сущность)';

-- =====================================================================
--  НАЧАЛЬНЫЕ ТЕСТОВЫЕ ДАННЫЕ
--  6 пациентов, 4 врача, 15 записей, все 5 статусов, 7 типов процедур.
--
--  Даты записей заданы относительно текущей даты (CURRENT_DATE), поэтому
--  тестовые данные всегда остаются актуальными: часть приёмов в прошлом
--  (завершённые, отменённый, неявка), часть — в будущем.
--  Примечание: при некоторых датах запуска отдельные записи могут попасть
--  на воскресенье. Правило "воскресенье — выходной" проверяется приложением
--  при создании новых записей, к уже загруженным тестовым данным оно не применяется.
-- =====================================================================

INSERT INTO patients (full_name, phone, email, birth_date)
VALUES ('Иванов Алексей Сергеевич', '+79001234501', 'alexey.ivanov@mail.ru', DATE '1990-03-15'),
       ('Петрова Мария Ивановна', '+79001234502', 'maria.petrova@mail.ru', DATE '1985-07-22'),
       ('Сидоров Дмитрий Павлович', '+79001234503', 'd.sidorov@yandex.ru', DATE '1998-11-05'),
       ('Кузнецова Анна Викторовна', '+79001234504', 'anna.kuznetsova@gmail.com', DATE '2001-01-30'),
       ('Смирнов Игорь Олегович', '+79001234505', NULL, DATE '1975-09-12'),
       ('Фёдорова Ольга Николаевна', '+79001234506', 'olga.fedorova@mail.ru', DATE '1993-05-19');

INSERT INTO dentists (full_name, phone, specialization, cabinet)
VALUES ('Морозов Пётр Андреевич', '+79007654321', 'Стоматолог-терапевт', 101),
       ('Волкова Елена Дмитриевна', '+79007654322', 'Ортодонт', 102),
       ('Зайцев Никита Романович', '+79007654323', 'Стоматолог-хирург', 103),
       ('Соколова Ирина Павловна', '+79007654324', 'Имплантолог', 104);

INSERT INTO appointments (patient_id, dentist_id, appointment_time, procedure_type, status, price, complaint)
VALUES
-- прошедшие приёмы: завершённые, отменённый и неявка
(1, 1, (CURRENT_DATE - 14) + TIME '10:00', 'CARIES_TREATMENT', 'COMPLETED', 7500.00,
 'Боль при накусывании на верхний левый зуб'),
(2, 1, (CURRENT_DATE - 12) + TIME '11:30', 'HYGIENE', 'COMPLETED', 5000.00,
 'Плановая чистка, налёт и кровоточивость дёсен'),
(3, 2, (CURRENT_DATE - 10) + TIME '09:30', 'ROOT_CANAL', 'COMPLETED', 14000.00,
 'Острая пульсирующая боль, отёк'),
(4, 3, (CURRENT_DATE - 8) + TIME '15:00', 'EXTRACTION', 'COMPLETED', 6000.00,
 'Разрушенный зуб мудрости'),
(5, 2, (CURRENT_DATE - 6) + TIME '12:00', 'CONSULTATION', 'CANCELLED', 1500.00,
 'Пациент отменил визит'),
(6, 4, (CURRENT_DATE - 5) + TIME '16:00', 'BRACES', 'NO_SHOW', 60000.00,
 'Консультация по установке брекет-системы'),
(1, 3, (CURRENT_DATE - 3) + TIME '14:00', 'CONSULTATION', 'COMPLETED', 1500.00,
 'Повторный осмотр после лечения'),

-- будущие приёмы: созданные и подтверждённые
(2, 2, (CURRENT_DATE + 1) + TIME '09:00', 'CONSULTATION', 'CONFIRMED', 1500.00,
 'Консультация по выравниванию зубов'),
(3, 1, (CURRENT_DATE + 1) + TIME '10:30', 'CARIES_TREATMENT', 'CREATED', 7500.00,
 'Тёмное пятно на переднем зубе'),
(4, 4, (CURRENT_DATE + 2) + TIME '11:00', 'IMPLANTATION', 'CONFIRMED', 45000.00,
 'Установка импланта вместо удалённого зуба'),
(5, 3, (CURRENT_DATE + 2) + TIME '13:00', 'HYGIENE', 'CREATED', 5000.00,
 'Профилактическая гигиена'),
(6, 1, (CURRENT_DATE + 3) + TIME '09:30', 'ROOT_CANAL', 'CREATED', 14500.00,
 'Ноющая боль в нижнем правом зубе'),
(1, 2, (CURRENT_DATE + 4) + TIME '15:00', 'EXTRACTION', 'CONFIRMED', 6000.00,
 'Удаление ретинированного зуба'),
(2, 4, (CURRENT_DATE + 6) + TIME '10:00', 'BRACES', 'CREATED', 60000.00,
 'Установка брекет-системы, первый этап'),
(3, 3, (CURRENT_DATE + 7) + TIME '12:30', 'CONSULTATION', 'CREATED', 2000.00,
 'Осмотр перед протезированием');

-- =====================================================================
--  ПРОВЕРКА ЗАГРУЗКИ
-- =====================================================================
SELECT 'Пациентов' AS entity, COUNT(*) AS total FROM patients
UNION ALL
SELECT 'Врачей', COUNT(*) FROM dentists
UNION ALL
SELECT 'Записей на приём', COUNT(*) FROM appointments;

SELECT a.id,
       p.full_name AS patient,
       d.full_name AS dentist,
       a.appointment_time,
       a.procedure_type,
       a.status,
       a.price
FROM appointments a
         JOIN patients p ON p.id = a.patient_id
         JOIN dentists d ON d.id = a.dentist_id
ORDER BY a.appointment_time;
