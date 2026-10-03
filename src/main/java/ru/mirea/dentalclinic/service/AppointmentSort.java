package ru.mirea.dentalclinic.service;

import ru.mirea.dentalclinic.model.Appointment;

import java.util.Comparator;

/** Способы сортировки записей на приём. Каждый вариант хранит свой компаратор. */
public enum AppointmentSort {

    TIME_ASC("По времени (сначала ранние)",
            Comparator.comparing(Appointment::getAppointmentTime)),
    TIME_DESC("По времени (сначала поздние)",
            Comparator.comparing(Appointment::getAppointmentTime).reversed()),
    PRICE_ASC("По стоимости (сначала дешёвые)",
            Comparator.comparing(Appointment::getPrice)),
    PRICE_DESC("По стоимости (сначала дорогие)",
            Comparator.comparing(Appointment::getPrice).reversed()),
    PATIENT_NAME("По ФИО пациента",
            Comparator.comparing(Appointment::getPatientName, String.CASE_INSENSITIVE_ORDER));

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
