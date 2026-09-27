package ru.mirea.dentalclinic.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

/**
 * Пациент стоматологической клиники — сущность "пользователь системы".
 * Таблица базы данных: patients.
 */
public class Patient extends Person {

    private String email;
    private LocalDate birthDate;
    private LocalDateTime createdAt;

    /** Полный конструктор — используется при чтении строки из ResultSet. */
    public Patient(int id, String fullName, String phone, String email,
                   LocalDate birthDate, LocalDateTime createdAt) {
        super(id, fullName, phone);
        this.email = email;
        this.birthDate = birthDate;
        this.createdAt = createdAt;
    }

    /** Конструктор для создания нового пациента (ID выдаёт база данных). */
    public Patient(String fullName, String phone, String email, LocalDate birthDate) {
        this(0, fullName, phone, email, birthDate, null);
    }

    @Override
    public String role() {
        return "Пациент";
    }

    @Override
    public String describe() {
        return super.describe() + ", возраст " + getAge() + " г.";
    }

    /** Возраст пациента вычисляется, а не хранится в базе данных. */
    public int getAge() {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
