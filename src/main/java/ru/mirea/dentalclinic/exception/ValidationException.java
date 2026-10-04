package ru.mirea.dentalclinic.exception;

/**
 * Некорректные данные объекта: пустое ФИО, неверный телефон, отрицательная цена и т.п.
 * Выбрасывается на уровне сервисов и моделей (проверка формата данных).
 */
public class ValidationException extends AppException {

    public ValidationException(String message) {
        super(message);
    }
}
