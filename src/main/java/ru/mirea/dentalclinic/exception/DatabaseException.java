package ru.mirea.dentalclinic.exception;

/** Ошибка работы с базой данных (обёртка над SQLException). */
public class DatabaseException extends ClinicException {

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
