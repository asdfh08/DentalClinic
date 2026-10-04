package ru.mirea.dentalclinic.exception;

/**
 * Запись с указанным идентификатором отсутствует в базе данных.
 */
public class EntityNotFoundException extends AppException {

    public EntityNotFoundException(String entityTitle, int id) {
        super(entityTitle + " с ID = " + id + " не найден(а)");
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}
