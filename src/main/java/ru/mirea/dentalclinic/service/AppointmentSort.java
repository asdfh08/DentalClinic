package ru.mirea.dentalclinic.service;

import ru.mirea.dentalclinic.model.Appointment;

import java.util.Comparator;

/**
 * Способы сортировки записей на приём.
 * Второе перечисление проекта: каждая константа хранит готовый {@link Comparator},
 * поэтому сервису не нужен switch — он просто вызывает getComparator().
 */
public enum AppointmentSort {

    TIME_ASC("По дате приёма: сначала ранние",
            Comparator.comparing((Appointment appointment) -> appointment.getAppointmentTime())),

    TIME_DESC("По дате приёма: сначала поздние",
            Comparator.comparing((Appointment appointment) -> appointment.getAppointmentTime()).reversed()),

    PRICE_DESC("По стоимости: сначала дорогие",
            Comparator.comparing((Appointment appointment) -> appointment.getPrice()).reversed()),

    PRICE_ASC("По стоимости: сначала дешёвые",
            Comparator.comparing((Appointment appointment) -> appointment.getPrice())),

    PATIENT_NAME("По ФИО пациента (А-Я)",
            Comparator.comparing((Appointment appointment) -> appointment.getPatientName(),
                    String.CASE_INSENSITIVE_ORDER)),

    STATUS("По статусу, затем по дате приёма",
            Comparator.comparing((Appointment appointment) -> appointment.getStatus())
                    .thenComparing((Appointment appointment) -> appointment.getAppointmentTime()));

    private final String title;
    private final Comparator<Appointment> comparator;

    AppointmentSort(String title, Comparator<Appointment> comparator) {
        this.title = title;
        this.comparator = comparator;
    }

    public String getTitle() {
        return title;
    }

    public Comparator<Appointment> getComparator() {
        return comparator;
    }
}
