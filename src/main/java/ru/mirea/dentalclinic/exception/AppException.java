package ru.mirea.dentalclinic.exception;

/**
 * Базовое собственное исключение приложения.
 * Все остальные исключения системы наследуются от него, поэтому консольный слой
 * может перехватить один тип и гарантировать, что программа не завершится аварийно.
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}
