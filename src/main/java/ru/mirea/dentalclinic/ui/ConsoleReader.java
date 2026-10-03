package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.util.Formats;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;

/**
 * Ввод с консоли. Все методы повторяют запрос, пока пользователь не введёт
 * корректное значение, поэтому сервисы получают уже разобранные данные.
 */
public class ConsoleReader {

    private final Scanner scanner;

    public ConsoleReader(InputStream input) {
        this.scanner = new Scanner(input, StandardCharsets.UTF_8);
    }

    private String readLine(String prompt) {
        System.out.print(prompt);
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException e) {
            System.out.println();
            System.out.println("Ввод завершён, выход из программы.");
            System.exit(0);
            return "";
        }
    }

    public int readInt(String prompt) {
        while (true) {
            String line = readLine(prompt);
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: нужно ввести целое число.");
            }
        }
    }

    /** Обязательная строка: повторяет запрос, пока не введено непустое значение. */
    public String readRequired(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("Ошибка: поле не может быть пустым.");
        }
    }

    /** Необязательная строка: пустой ввод возвращается как "" (не null). */
    public String readOptional(String prompt) {
        return readLine(prompt);
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            LocalDate date = parseDate(readLine(prompt + " (дд.мм.гггг): "));
            if (date != null) {
                return date;
            }
        }
    }

    /** Пустой ввод -> null (значение не меняется). */
    public LocalDate readOptionalDate(String prompt) {
        while (true) {
            String line = readLine(prompt + " (дд.мм.гггг, Enter — не менять): ");
            if (line.isEmpty()) {
                return null;
            }
            LocalDate date = parseDate(line);
            if (date != null) {
                return date;
            }
        }
    }

    public LocalDateTime readDateTime(String prompt) {
        while (true) {
            LocalDateTime dateTime = parseDateTime(readLine(prompt + " (дд.мм.гггг чч:мм): "));
            if (dateTime != null) {
                return dateTime;
            }
        }
    }

    /** Пустой ввод -> null (значение не меняется). */
    public LocalDateTime readOptionalDateTime(String prompt) {
        while (true) {
            String line = readLine(prompt + " (дд.мм.гггг чч:мм, Enter — не менять): ");
            if (line.isEmpty()) {
                return null;
            }
            LocalDateTime dateTime = parseDateTime(line);
            if (dateTime != null) {
                return dateTime;
            }
        }
    }

    /** Пустой ввод -> null (сервис подставит базовую цену процедуры). */
    public BigDecimal readOptionalMoney(String prompt) {
        while (true) {
            String line = readLine(prompt);
            if (line.isEmpty()) {
                return null;
            }
            try {
                return new BigDecimal(line.replace(',', '.').replace(" ", ""));
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: введите число, например 7500 или 7500.50.");
            }
        }
    }

    /** Да/нет. Принимает: да, д, yes, y / нет, н, no, n. */
    public boolean confirm(String prompt) {
        while (true) {
            String line = readLine(prompt + " (да/нет): ").toLowerCase();
            switch (line) {
                case "да", "д", "yes", "y" -> {
                    return true;
                }
                case "нет", "н", "no", "n" -> {
                    return false;
                }
                default -> System.out.println("Ошибка: ответьте \"да\" или \"нет\".");
            }
        }
    }

    /**
     * Нумерованный выбор из списка.
     *
     * @return выбранный элемент или null, если пользователь выбрал 0 (отмена)
     */
    public <T> T readChoice(String title, List<T> options, Function<T, String> label) {
        System.out.println(title);
        for (int i = 0; i < options.size(); i++) {
            System.out.println((i + 1) + ". " + label.apply(options.get(i)));
        }
        System.out.println("0. Отмена");

        while (true) {
            int number = readInt("Ваш выбор: ");
            if (number == 0) {
                return null;
            }
            if (number >= 1 && number <= options.size()) {
                return options.get(number - 1);
            }
            System.out.println("Ошибка: выберите число от 0 до " + options.size() + ".");
        }
    }

    private LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text, Formats.DATE);
        } catch (DateTimeParseException e) {
            System.out.println("Ошибка: дата должна быть в формате дд.мм.гггг, например 15.03.1990.");
            return null;
        }
    }

    private LocalDateTime parseDateTime(String text) {
        try {
            return LocalDateTime.parse(text, Formats.DATE_TIME);
        } catch (DateTimeParseException e) {
            System.out.println("Ошибка: формат даты и времени — дд.мм.гггг чч:мм, например 15.03.2027 10:30.");
            return null;
        }
    }
}
