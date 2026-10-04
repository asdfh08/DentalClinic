package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.exception.AppException;
import ru.mirea.dentalclinic.util.DatabaseManager;

/**
 * Главное меню консольного приложения (слой Console UI).
 *
 * Класс только выводит меню и передаёт управление сервисам через экраны-представления.
 * SQL-запросов и бизнес-логики здесь нет.
 */
public class ConsoleApp {

    private static final String TITLE = "Система записи на стоматологический приём";

    private final ConsoleReader reader;
    private final PatientView patientView;
    private final DentistView dentistView;
    private final AppointmentView appointmentView;
    private final AnalyticsView analyticsView;

    public ConsoleApp(ConsoleReader reader,
                      PatientView patientView,
                      DentistView dentistView,
                      AppointmentView appointmentView,
                      AnalyticsView analyticsView) {
        this.reader = reader;
        this.patientView = patientView;
        this.dentistView = dentistView;
        this.appointmentView = appointmentView;
        this.analyticsView = analyticsView;
    }

    public void run() {
        System.out.println();
        System.out.println("Подключение к базе данных: " + DatabaseManager.connectionInfo());

        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = reader.readInt("Выберите действие: ");

            switch (choice) {
                case 1 -> safe(patientView::menu);
                case 2 -> safe(dentistView::menu);
                case 3 -> safe(appointmentView::menu);
                case 4 -> safe(analyticsView::searchMenu);
                case 5 -> safe(analyticsView::filterMenu);
                case 6 -> safe(analyticsView::sortMenu);
                case 7 -> safe(analyticsView::showStatistics);
                case 8 -> safe(analyticsView::exportMenu);
                case 9 -> safe(analyticsView::showDatabaseTables);
                case 0 -> running = false;
                default -> System.out.println("Ошибка: пункта меню " + choice + " не существует.");
            }
        }

        System.out.println("Работа программы завершена. До свидания!");
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println(ConsoleFormat.title(TITLE));
        System.out.println("1. Пациенты");
        System.out.println("2. Врачи");
        System.out.println("3. Записи на приём");
        System.out.println("4. Поиск");
        System.out.println("5. Фильтрация");
        System.out.println("6. Сортировка");
        System.out.println("7. Статистика");
        System.out.println("8. Экспорт данных");
        System.out.println("9. Вывести таблицы базы данных");
        System.out.println("0. Выход");
    }

    /**
     * Последний уровень защиты: даже если исключение не было обработано во
     * вложенном меню, программа сообщит об ошибке и вернётся в главное меню.
     */
    private void safe(Runnable action) {
        try {
            action.run();
        } catch (AppException e) {
            System.out.println("Ошибка: " + e.getMessage());
        } catch (RuntimeException e) {
            System.out.println("Непредвиденная ошибка: " + e);
        }
    }
}
