package ru.mirea.dentalclinic.model;

/**
 * Врач-стоматолог. Таблица базы данных: dentists.
 * Второй наследник {@link Person} — на нём демонстрируется полиморфизм
 * (один и тот же вызов describe() даёт разный результат для пациента и врача).
 */
public class Dentist extends Person {

    private String specialization;
    private int cabinet;

    public Dentist(int id, String fullName, String phone, String specialization, int cabinet) {
        super(id, fullName, phone);
        this.specialization = specialization;
        this.cabinet = cabinet;
    }

    public Dentist(String fullName, String phone, String specialization, int cabinet) {
        this(0, fullName, phone, specialization, cabinet);
    }

    @Override
    public String role() {
        return "Врач-стоматолог";
    }

    @Override
    public String describe() {
        return super.describe() + ", " + specialization + ", кабинет " + cabinet;
    }

    public String getSpecialization() {
        return specialization;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public int getCabinet() {
        return cabinet;
    }

    public void setCabinet(int cabinet) {
        this.cabinet = cabinet;
    }
}
