package ru.mirea.dentalclinic.model;

import java.util.Objects;

/**
 * Базовый абстрактный класс участника предметной области (человека клиники).
 *
 * Демонстрирует:
 * - инкапсуляцию: все поля private, доступ только через методы;
 * - наследование: {@link Patient} и {@link Dentist};
 * - полиморфизм: абстрактный метод {@link #role()} и переопределяемый {@link #describe()}.
 */
public abstract class Person {

    private int id;
    private String fullName;
    private String phone;

    protected Person(int id, String fullName, String phone) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
    }

    /** Роль участника в предметной области. Реализуется в классах-наследниках. */
    public abstract String role();

    /** Текстовое описание участника. Наследники дополняют его своими данными. */
    public String describe() {
        return role() + " #" + id + ": " + fullName + ", тел. " + phone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        return describe();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return id == ((Person) other).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass().getName(), id);
    }
}
