package ru.mirea.dentalclinic.exception;

/** Объект с указанным ID не найден в базе данных. */
public class EntityNotFoundException extends ClinicException {

    public EntityNotFoundException(String entityName, int id) {
        super("не найдено: " + entityName + " с ID " + id);
    }
}
