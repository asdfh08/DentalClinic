package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.exception.AppException;

/**
 * Базовый класс всех экранов консольного интерфейса.
 *
 * Здесь собрана единая обработка ошибок: любое исключение приложения
 * превращается в сообщение пользователю, и программа продолжает работу.
 */
public abstract class BaseView {

    protected final ConsoleReader reader;

    protected BaseView(ConsoleReader reader) {
        this.reader = reader;
    }

    /** Заголовок экрана — каждый наследник возвращает свой (полиморфизм). */
    protected abstract String screenTitle();

    protected void printHeader() {
        System.out.println(ConsoleFormat.subTitle(screenTitle()));
    }

    /**
     * Выполняет действие и перехватывает исключения, чтобы программа
     * не завершалась аварийно и возвращалась в то же меню.
     */
    protected void safe(Runnable action) {
        try {
            action.run();
        } catch (AppException e) {
            System.out.println("Ошибка: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Непредвиденная ошибка: " + e);
        }
    }
}
