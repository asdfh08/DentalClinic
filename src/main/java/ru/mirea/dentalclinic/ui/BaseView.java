package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.exception.ClinicException;

/** Базовый класс консольных экранов: заголовок и безопасный запуск действий. */
public abstract class BaseView {

    protected final ConsoleReader reader;

    protected BaseView(ConsoleReader reader) {
        this.reader = reader;
    }

    /** Название экрана, которое печатается в заголовке меню. */
    protected abstract String screenTitle();

    protected void printHeader() {
        System.out.println();
        System.out.println("===== " + screenTitle() + " =====");
    }

    /**
     * Выполняет действие меню и превращает ошибки приложения в понятное сообщение,
     * чтобы программа не завершалась из-за неверного ввода или нарушенного правила.
     */
    protected void safe(Runnable action) {
        try {
            action.run();
        } catch (ClinicException e) {
            System.out.println("Ошибка: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Непредвиденная ошибка: " + e);
        }
    }
}
