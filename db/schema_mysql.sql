-- =====================================================================
--  КР 1. Информационная система "Запись пациента на стоматологический приём"
--  СУБД: MySQL 8.0+
--  Запуск:  mysql -u root -p < db/schema_mysql.sql
-- =====================================================================

DROP DATABASE IF EXISTS dental_clinic;
CREATE DATABASE dental_clinic
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
USE dental_clinic;

-- ---------------------------------------------------------------------
-- Таблица 1: пациенты (участник предметной области)
-- ---------------------------------------------------------------------
CREATE TABLE patients
(
    id         INT AUTO_INCREMENT PRIMARY KEY,
    full_name  VARCHAR(120) NOT NULL,
    phone      VARCHAR(20)  NOT NULL UNIQUE,
    email      VARCHAR(120) UNIQUE,
    birth_date DATE         NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_patients_name_length CHECK (CHAR_LENGTH(TRIM(full_name)) >= 3),
    CONSTRAINT chk_patients_phone_length CHECK (CHAR_LENGTH(phone) >= 10)
);

-- ---------------------------------------------------------------------
-- Таблица 2: врачи-стоматологи
-- ---------------------------------------------------------------------
CREATE TABLE dentists
(
    id             INT AUTO_INCREMENT PRIMARY KEY,
    full_name      VARCHAR(120) NOT NULL,
    phone          VARCHAR(20)  NOT NULL UNIQUE,
    specialization VARCHAR(60)  NOT NULL,
    cabinet        INT          NOT NULL,

    CONSTRAINT chk_dentists_name_length CHECK (CHAR_LENGTH(TRIM(full_name)) >= 3),
    CONSTRAINT chk_dentists_cabinet CHECK (cabinet BETWEEN 1 AND 500)
);

-- ---------------------------------------------------------------------
-- Таблица 3: ОСНОВНАЯ СУЩНОСТЬ — записи на приём
--   appointments.patient_id -> patients.id
--   appointments.dentist_id -> dentists.id
-- ---------------------------------------------------------------------
CREATE TABLE appointments
(
    id               INT AUTO_INCREMENT PRIMARY KEY,
    patient_id       INT            NOT NULL,
    dentist_id       INT            NOT NULL,
    appointment_time DATETIME       NOT NULL,
    procedure_type   VARCHAR(30)    NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'CREATED',
    price            DECIMAL(10, 2) NOT NULL,
    complaint        VARCHAR(255),
    created_at       TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_appointments_patient FOREIGN KEY (patient_id) REFERENCES patients (id)
        ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_appointments_dentist FOREIGN KEY (dentist_id) REFERENCES dentists (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,

    -- один врач не может быть занят двумя приёмами в одну и ту же минуту
    CONSTRAINT uq_appointments_dentist_slot UNIQUE (dentist_id, appointment_time),

    -- ограничения допустимых значений: значения должны совпадать с enum в Java
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

-- =====================================================================
--  НАЧАЛЬНЫЕ ТЕСТОВЫЕ ДАННЫЕ
--  6 пациентов, 4 врача, 15 записей, 5 различных статусов, 7 процедур.
--  Даты заданы относительно текущей даты, поэтому данные всегда актуальны.
-- =====================================================================

INSERT INTO patients (full_name, phone, email, birth_date)
VALUES ('Иванов Алексей Сергеевич', '+79001234501', 'alexey.ivanov@mail.ru', '1990-03-15'),
       ('Петрова Мария Ивановна', '+79001234502', 'maria.petrova@mail.ru', '1985-07-22'),
       ('Сидоров Дмитрий Павлович', '+79001234503', 'd.sidorov@yandex.ru', '1998-11-05'),
       ('Кузнецова Анна Викторовна', '+79001234504', 'anna.kuznetsova@gmail.com', '2001-01-30'),
       ('Смирнов Игорь Олегович', '+79001234505', NULL, '1975-09-12'),
       ('Фёдорова Ольга Николаевна', '+79001234506', 'olga.fedorova@mail.ru', '1993-05-19');

INSERT INTO dentists (full_name, phone, specialization, cabinet)
VALUES ('Морозов Пётр Андреевич', '+79007654321', 'Стоматолог-терапевт', 101),
       ('Волкова Елена Дмитриевна', '+79007654322', 'Ортодонт', 102),
       ('Зайцев Никита Романович', '+79007654323', 'Стоматолог-хирург', 103),
       ('Соколова Ирина Павловна', '+79007654324', 'Имплантолог', 104);

INSERT INTO appointments (patient_id, dentist_id, appointment_time, procedure_type, status, price, complaint)
VALUES
-- прошедшие приёмы: завершённые, отменённый и неявка
(1, 1, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -14 DAY), '10:00:00'), 'CARIES_TREATMENT', 'COMPLETED', 7500.00,
 'Боль при накусывании на верхний левый зуб'),
(2, 1, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -12 DAY), '11:30:00'), 'HYGIENE', 'COMPLETED', 5000.00,
 'Плановая чистка, налёт и кровоточивость дёсен'),
(3, 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -10 DAY), '09:30:00'), 'ROOT_CANAL', 'COMPLETED', 14000.00,
 'Острая пульсирующая боль, отёк'),
(4, 3, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -8 DAY), '15:00:00'), 'EXTRACTION', 'COMPLETED', 6000.00,
 'Разрушенный зуб мудрости'),
(5, 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -6 DAY), '12:00:00'), 'CONSULTATION', 'CANCELLED', 1500.00,
 'Пациент отменил визит'),
(6, 4, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -5 DAY), '16:00:00'), 'BRACES', 'NO_SHOW', 60000.00,
 'Консультация по установке брекет-системы'),
(1, 3, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL -3 DAY), '14:00:00'), 'CONSULTATION', 'COMPLETED', 1500.00,
 'Повторный осмотр после лечения'),

-- будущие приёмы: созданные и подтверждённые
(2, 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '09:00:00'), 'CONSULTATION', 'CONFIRMED', 1500.00,
 'Консультация по выравниванию зубов'),
(3, 1, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 1 DAY), '10:30:00'), 'CARIES_TREATMENT', 'CREATED', 7500.00,
 'Тёмное пятно на переднем зубе'),
(4, 4, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '11:00:00'), 'IMPLANTATION', 'CONFIRMED', 45000.00,
 'Установка импланта вместо удалённого зуба'),
(5, 3, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 2 DAY), '13:00:00'), 'HYGIENE', 'CREATED', 5000.00,
 'Профилактическая гигиена'),
(6, 1, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 3 DAY), '09:30:00'), 'ROOT_CANAL', 'CREATED', 14500.00,
 'Ноющая боль в нижнем правом зубе'),
(1, 2, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 4 DAY), '15:00:00'), 'EXTRACTION', 'CONFIRMED', 6000.00,
 'Удаление ретинированного зуба'),
(2, 4, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 6 DAY), '10:00:00'), 'BRACES', 'CREATED', 60000.00,
 'Установка брекет-системы, первый этап'),
(3, 3, TIMESTAMP(DATE_ADD(CURDATE(), INTERVAL 7 DAY), '12:30:00'), 'CONSULTATION', 'CREATED', 2000.00,
 'Осмотр перед протезированием');

-- =====================================================================
--  ПРОВЕРКА
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
