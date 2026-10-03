package ru.mirea.dentalclinic.exception;

/** Нарушено бизнес-правило (врач занят, запись в прошлом, запрещённый переход статуса и т.д.). */
public class BusinessException extends ClinicException {

    public BusinessException(String message) {
        super(message);
    }
}
