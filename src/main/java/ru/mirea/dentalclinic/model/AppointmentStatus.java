package ru.mirea.dentalclinic.model;

import ru.mirea.dentalclinic.exception.ValidationException;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Статус записи пациента на приём.
 * Перечисление хранит не только константы, но и поведение:
 * русское название, признак завершённости и правила переходов между статусами.
 */
public enum AppointmentStatus {

    CREATED("Создана"),
    CONFIRMED("Подтверждена"),
    COMPLETED("Завершена"),
    CANCELLED("Отменена"),
    NO_SHOW("Неявка");

    private final String title;

    AppointmentStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    /** Финальный статус изменить уже нельзя. */
    public boolean isFinal() {
        return this == COMPLETED || this == CANCELLED || this == NO_SHOW;
    }

    /** Активная запись — та, которая ещё будет обслужена. */
    public boolean isActive() {
        return !isFinal();
    }

    /** Разрешённые переходы: CREATED -> CONFIRMED -> COMPLETED / CANCELLED / NO_SHOW. */
    public List<AppointmentStatus> allowedTransitions() {
        return switch (this) {
            case CREATED -> List.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> List.of(COMPLETED, CANCELLED, NO_SHOW);
            case COMPLETED, CANCELLED, NO_SHOW -> List.of();
        };
    }

    public boolean canChangeTo(AppointmentStatus target) {
        return target != null && allowedTransitions().contains(target);
    }

    /** Разбор значения, прочитанного из базы данных или введённого пользователем. */
    public static AppointmentStatus parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Статус записи не может быть пустым");
        }
        String value = raw.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(status -> status.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new ValidationException(
                        "Недопустимый статус записи: " + raw + ". Допустимые значения: " + names()));
    }

    public static String names() {
        return Arrays.stream(values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
