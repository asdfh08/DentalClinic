package ru.mirea.dentalclinic.exception;

/**
 * Нарушение бизнес-правила предметной области:
 * занятый слот врача, запись в прошлое, запрещённый переход между статусами и т.д.
 * Выбрасывается слоем Service.
 */
public class BusinessException extends AppException {

    public BusinessException(String message) {
        super(message);
    }
}
