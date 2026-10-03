package ru.mirea.dentalclinic.exception;

/** Введены некорректные данные (пустое поле, неверный формат, значение вне диапазона). */
public class ValidationException extends ClinicException {

    public ValidationException(String message) {
        super(message);
    }
}
