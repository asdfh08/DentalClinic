package ru.mirea.dentalclinic.model;

import ru.mirea.dentalclinic.exception.ValidationException;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Тип стоматологической процедуры.
 * У каждой константы своя длительность и базовая стоимость —
 * они используются бизнес-правилами (проверка занятости слота врача и расчёт цены).
 */
public enum ProcedureType {

    CONSULTATION("Консультация", 30, "1500.00"),
    HYGIENE("Профессиональная гигиена", 60, "5000.00"),
    CARIES_TREATMENT("Лечение кариеса", 60, "7500.00"),
    ROOT_CANAL("Лечение каналов", 90, "14000.00"),
    EXTRACTION("Удаление зуба", 45, "6000.00"),
    IMPLANTATION("Имплантация", 120, "45000.00"),
    BRACES("Установка брекет-системы", 120, "60000.00");

    private final String title;
    private final int durationMinutes;
    private final BigDecimal basePrice;

    ProcedureType(String title, int durationMinutes, String basePrice) {
        this.title = title;
        this.durationMinutes = durationMinutes;
        this.basePrice = new BigDecimal(basePrice);
    }

    public String getTitle() {
        return title;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public BigDecimal getBasePrice() {
        return basePrice;
    }

    public static ProcedureType parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Тип процедуры не может быть пустым");
        }
        String value = raw.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(type -> type.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "Недопустимый тип процедуры: " + raw + ". Допустимые значения: " + names()));
    }

    public static String names() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
