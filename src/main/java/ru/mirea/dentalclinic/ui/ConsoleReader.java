package ru.mirea.dentalclinic.ui;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.function.Function;

import ru.mirea.dentalclinic.util.Formats;

/**
 * Чтение данных из консоли с обработкой некорректного ввода.
 *
 * Требование КР: программа не должна аварийно завершаться, если пользователь
 * ввёл текст вместо числа или неверную дату — вместо этого выводится сообщение
 * и ввод повторяется.
 */
public class ConsoleReader {

    private static final String DATE_HINT = "дд.мм.гггг";
    private static final String DATE_TIME_HINT = "дд.мм.гггг чч:мм";

    private final Scanner scanner;

    public ConsoleReader(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        try {
            String line = scanner.nextLine();
            return line == null ? "" : line.trim();
        } catch (NoSuchElementException e) {
            // поток ввода закрыт (например, Ctrl+D) — корректно завершаем работу
            System.out.println();
            System.out.println("Ввод завершён, программа остановлена.");
            System.exit(0);
            return "";
        }
    }

    /** Обязательная строка: пустой ввод не принимается. */
    public String readRequired(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isBlank()) {
                return value;
            }
            System.out.println("Ошибка: значение не может быть пустым.");
        }
    }

    /** Необязательная строка: пустой ввод означает "не менять / не указывать". */
    public String readOptional(String prompt) {
        return readLine(prompt);
    }

    /** Целое число с обработкой NumberFormatException. */
    public int readInt(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: нужно ввести целое число, получено \"" + value + "\".");
            }
        }
    }

    public int readIntInRange(String prompt, int min, int max) {
        while (true) {
            int value = readInt(prompt);
            if (value >= min && value <= max) {
                return value;
            }
            System.out.println("Ошибка: число должно быть от " + min + " до " + max + ".");
        }
    }

    /** Денежная сумма; пустой ввод возвращает null (значение по умолчанию). */
    public BigDecimal readOptionalMoney(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value.isBlank()) {
                return null;
            }
            try {
                return new BigDecimal(value.replace(',', '.').replace(" ", ""));
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: сумма должна быть числом, например 7500 или 7500.50.");
            }
        }
    }

    public LocalDate readDate(String prompt) {
        while (true) {
            String value = readRequired(prompt + " (" + DATE_HINT + "): ");
            try {
                return LocalDate.parse(value, Formats.DATE);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: дата должна быть в формате " + DATE_HINT + ", например 15.03.1990.");
            }
        }
    }

    /** Дата; пустой ввод возвращает null. */
    public LocalDate readOptionalDate(String prompt) {
        while (true) {
            String value = readLine(prompt + " (" + DATE_HINT + ", Enter — пропустить): ");
            if (value.isBlank()) {
                return null;
            }
            try {
                return LocalDate.parse(value, Formats.DATE);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: дата должна быть в формате " + DATE_HINT + ".");
            }
        }
    }

    public LocalDateTime readDateTime(String prompt) {
        while (true) {
            String value = readRequired(prompt + " (" + DATE_TIME_HINT + "): ");
            try {
                return LocalDateTime.parse(value, Formats.DATE_TIME);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: дата и время должны быть в формате "
                        + DATE_TIME_HINT + ", например 25.09.2026 10:30.");
            }
        }
    }

    /** Дата и время; пустой ввод возвращает null. */
    public LocalDateTime readOptionalDateTime(String prompt) {
        while (true) {
            String value = readLine(prompt + " (" + DATE_TIME_HINT + ", Enter — не менять): ");
            if (value.isBlank()) {
                return null;
            }
            try {
                return LocalDateTime.parse(value, Formats.DATE_TIME);
            } catch (DateTimeParseException e) {
                System.out.println("Ошибка: дата и время должны быть в формате " + DATE_TIME_HINT + ".");
            }
        }
    }

    public boolean confirm(String prompt) {
        while (true) {
            String value = readLine(prompt + " (да/нет): ").toLowerCase();
            if (value.equals("да") || value.equals("d") || value.equals("y") || value.equals("yes")) {
                return true;
            }
            if (value.equals("нет") || value.equals("n") || value.equals("no")) {
                return false;
            }
            System.out.println("Ошибка: введите \"да\" или \"нет\".");
        }
    }

    /**
     * Выбор одного элемента из списка по номеру.
     * Используется для выбора типа процедуры, статуса, формата экспорта и т.п.
     *
     * @return выбранный элемент или null, если список пуст либо пользователь отказался
     */
    public <T> T readChoice(String title, List<T> options, Function<T, String> label) {
        if (options.isEmpty()) {
            System.out.println("Выбирать нечего: список пуст.");
            return null;
        }

        System.out.println(title);
        for (int i = 0; i < options.size(); i++) {
            System.out.println("  " + (i + 1) + ". " + label.apply(options.get(i)));
        }
        System.out.println("  0. Отмена");

        int choice = readIntInRange("Ваш выбор: ", 0, options.size());
        return choice == 0 ? null : options.get(choice - 1);
    }

    public void pause() {
        readLine("Нажмите Enter, чтобы продолжить...");
    }
}
