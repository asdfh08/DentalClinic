package ru.mirea.dentalclinic.service;

import ru.mirea.dentalclinic.model.Dentist;

import java.util.Comparator;

/**
 * Способы сортировки врачей: по каждому ключевому параметру —
 * прямая и обратная сортировка.
 */
public enum DentistSort {

    NAME_ASC("По ФИО: А-Я",
            Comparator.comparing((Dentist dentist) -> dentist.getFullName(), String.CASE_INSENSITIVE_ORDER)),

    NAME_DESC("По ФИО: Я-А",
            Comparator.comparing((Dentist dentist) -> dentist.getFullName(), String.CASE_INSENSITIVE_ORDER)
                    .reversed()),

    CABINET_ASC("По номеру кабинета: по возрастанию",
            Comparator.comparingInt((Dentist dentist) -> dentist.getCabinet())),

    CABINET_DESC("По номеру кабинета: по убыванию",
            Comparator.comparingInt((Dentist dentist) -> dentist.getCabinet()).reversed()),

    SPECIALIZATION_ASC("По специализации: А-Я",
            Comparator.comparing((Dentist dentist) -> dentist.getSpecialization().getTitle(),
                            String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(dentist -> dentist.getFullName(), String.CASE_INSENSITIVE_ORDER)),

    SPECIALIZATION_DESC("По специализации: Я-А",
            Comparator.comparing((Dentist dentist) -> dentist.getSpecialization().getTitle(),
                            String.CASE_INSENSITIVE_ORDER)
                    .reversed()
                    .thenComparing(dentist -> dentist.getFullName(), String.CASE_INSENSITIVE_ORDER));

    private final String title;
    private final Comparator<Dentist> comparator;

    DentistSort(String title, Comparator<Dentist> comparator) {
        this.title = title;
        this.comparator = comparator;
    }

    public String getTitle() {
        return title;
    }

    public Comparator<Dentist> getComparator() {
        return comparator;
    }
}
