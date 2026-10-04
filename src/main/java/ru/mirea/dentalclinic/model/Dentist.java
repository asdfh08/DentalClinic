package ru.mirea.dentalclinic.model;

/**
 * Врач-стоматолог. Таблица базы данных: dentists.
 * Второй наследник {@link Person} — на нём демонстрируется полиморфизм
 * (один и тот же вызов describe() даёт разный результат для пациента и врача).
 */
public class Dentist extends Person {

    private Specialization specialization;
    private int cabinet;

    public Dentist(int id, String fullName, String phone, Specialization specialization, int cabinet) {
        super(id, fullName, phone);
        this.specialization = specialization;
        this.cabinet = cabinet;
    }

    public Dentist(String fullName, String phone, Specialization specialization, int cabinet) {
        this(0, fullName, phone, specialization, cabinet);
    }

    /** Может ли врач выполнять процедуру — определяется его специализацией. */
    public boolean canPerform(ProcedureType procedureType) {
        return specialization.canPerform(procedureType);
    }

    @Override
    public String role() {
        return "Врач-стоматолог";
    }

    @Override
    public String describe() {
        return super.describe() + ", " + specialization.getTitle() + ", кабинет " + cabinet;
    }

    public Specialization getSpecialization() {
        return specialization;
    }

    public void setSpecialization(Specialization specialization) {
        this.specialization = specialization;
    }

    public int getCabinet() {
        return cabinet;
    }

    public void setCabinet(int cabinet) {
        this.cabinet = cabinet;
    }
}
