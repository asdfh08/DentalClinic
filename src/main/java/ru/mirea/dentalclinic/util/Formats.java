package ru.mirea.dentalclinic.util;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Единые форматы вывода даты, времени и денежных сумм.
 * Утилитный класс: приватный конструктор, только статические методы.
 */
public final class Formats {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private Formats() {
    }

    public static String date(LocalDate value) {
        return value == null ? "-" : value.format(DATE);
    }

    public static String dateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(DATE_TIME);
    }

    public static String time(LocalDateTime value) {
        return value == null ? "-" : value.format(TIME);
    }

    public static String money(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        return String.format(Locale.ROOT, "%,.2f руб.", value);
    }

    /** Обрезает длинную строку, чтобы таблица в консоли не "разъезжалась". */
    public static String cut(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)) + "...";
    }
}
