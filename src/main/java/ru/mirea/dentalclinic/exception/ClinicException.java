package ru.mirea.dentalclinic.exception;

/** Базовое исключение приложения. Сообщение предназначено для показа пользователю в консоли. */
public class ClinicException extends RuntimeException {

    public ClinicException(String message) {
        super(message);
    }

    public ClinicException(String message, Throwable cause) {
        super(message, cause);
    }
}
