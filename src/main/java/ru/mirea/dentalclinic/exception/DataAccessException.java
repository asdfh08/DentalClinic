package ru.mirea.dentalclinic.exception;

/**
 * Ошибка работы с базой данных: нет подключения, некорректный SQL-запрос,
 * недоступен сервер СУБД. Оборачивает проверяемое {@link java.sql.SQLException}
 * в непроверяемое исключение приложения, чтобы SQL-детали не "протекали"
 * в слои Service и Console UI.
 */
public class DataAccessException extends AppException {

    public DataAccessException(String message) {
        super(message);
    }

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
