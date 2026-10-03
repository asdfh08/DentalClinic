package ru.mirea.dentalclinic.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/** Единые форматы вывода дат, времени и денег. */
public final class Formats {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private static final String EMPTY = "-";

    private Formats() {
    }

    public static String date(LocalDate date) {
        return date == null ? EMPTY : DATE.format(date);
    }

    public static String time(LocalTime time) {
        return time == null ? EMPTY : TIME.format(time);
    }

    public static String time(LocalDateTime dateTime) {
        return dateTime == null ? EMPTY : TIME.format(dateTime);
    }

    public static String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? EMPTY : DATE_TIME.format(dateTime);
    }

    /** 7500 -> "7 500,00 руб." */
    public static String money(BigDecimal amount) {
        if (amount == null) {
            return EMPTY;
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(' ');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0.00", symbols).format(amount) + " руб.";
    }
}
