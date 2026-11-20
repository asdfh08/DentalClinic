package ru.mirea.dentalclinic.service;

import ru.mirea.dentalclinic.model.Patient;

import java.time.LocalDateTime;
import java.util.Comparator;

/**
 * Способы сортировки пациентов: по каждому ключевому параметру —
 * прямая и обратная сортировка.
 */
public enum PatientSort {

    NAME_ASC("По ФИО: А-Я",
            Comparator.comparing((Patient patient) -> patient.getFullName(), String.CASE_INSENSITIVE_ORDER)),

    NAME_DESC("По ФИО: Я-А",
            Comparator.comparing((Patient patient) -> patient.getFullName(), String.CASE_INSENSITIVE_ORDER)
                    .reversed()),

    AGE_ASC("По возрасту: сначала младшие",
            Comparator.comparing((Patient patient) -> patient.getBirthDate()).reversed()),

    AGE_DESC("По возрасту: сначала старшие",
            Comparator.comparing((Patient patient) -> patient.getBirthDate())),

    REGISTERED_ASC("По дате регистрации: сначала давние",
            Comparator.comparing((Patient patient) -> registeredAt(patient))
                    .thenComparingInt(patient -> patient.getId())),

    REGISTERED_DESC("По дате регистрации: сначала новые",
            Comparator.comparing((Patient patient) -> registeredAt(patient))
                    .thenComparingInt(patient -> patient.getId())
                    .reversed());

    private final String title;
    private final Comparator<Patient> comparator;

    PatientSort(String title, Comparator<Patient> comparator) {
        this.title = title;
        this.comparator = comparator;
    }

    public String getTitle() {
        return title;
    }

    public Comparator<Patient> getComparator() {
        return comparator;
    }

    private static LocalDateTime registeredAt(Patient patient) {
        return patient.getCreatedAt() == null ? LocalDateTime.MIN : patient.getCreatedAt();
    }
}
