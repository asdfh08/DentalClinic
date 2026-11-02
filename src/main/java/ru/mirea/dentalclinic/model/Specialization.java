package ru.mirea.dentalclinic.model;

import ru.mirea.dentalclinic.exception.ValidationException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Специализация врача-стоматолога.
 * Перечисление сущности "Врач": вместо произвольной строки врач получает
 * одно из фиксированных значений, поэтому по специализации можно надёжно
 * фильтровать и сортировать.
 *
 * В базе данных (столбец dentists.specialization) хранится русское название.
 */
public enum Specialization {

    THERAPIST("Стоматолог-терапевт"),
    SURGEON("Стоматолог-хирург"),
    ORTHODONTIST("Ортодонт"),
    ORTHOPEDIST("Стоматолог-ортопед"),
    IMPLANTOLOGIST("Имплантолог"),
    PERIODONTIST("Пародонтолог"),
    PEDIATRIC("Детский стоматолог"),
    HYGIENIST("Гигиенист"),
    GENERAL("Стоматолог общей практики");

    private final String title;

    Specialization(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Строгий разбор значения, введённого пользователем: подходит имя константы
     * (THERAPIST) или русское название (Стоматолог-терапевт), регистр не важен.
     */
    public static Specialization parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("специализация врача не может быть пустой");
        }
        String value = raw.trim();
        return Arrays.stream(values())
                .filter(item -> item.name().equalsIgnoreCase(value) || item.title.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "недопустимая специализация: " + raw + ". Допустимые значения: " + titles()));
    }

    /**
     * Мягкий разбор значения, прочитанного из базы данных.
     * Раньше специализация вводилась свободным текстом, поэтому в таблице могли
     * остаться значения вроде "терапевт" или "хирург" — они сопоставляются с
     * подходящей константой, а совсем незнакомый текст превращается в {@link #GENERAL}.
     */
    public static Specialization fromDatabase(String raw) {
        if (raw == null || raw.isBlank()) {
            return GENERAL;
        }
        String value = raw.trim().toLowerCase();
        for (Specialization item : values()) {
            String title = item.title.toLowerCase();
            if (item.name().equalsIgnoreCase(value) || title.equals(value)) {
                return item;
            }
        }
        for (Specialization item : values()) {
            String title = item.title.toLowerCase();
            if (item != GENERAL && (title.contains(value) || value.contains(title))) {
                return item;
            }
        }
        return GENERAL;
    }

    public static String titles() {
        return Arrays.stream(values())
                .map(Specialization::getTitle)
                .collect(Collectors.joining(", "));
    }
}
