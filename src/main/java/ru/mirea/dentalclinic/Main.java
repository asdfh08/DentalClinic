package ru.mirea.dentalclinic;

import ru.mirea.dentalclinic.exception.AppException;
import ru.mirea.dentalclinic.repository.AppointmentRepository;
import ru.mirea.dentalclinic.repository.DentistRepository;
import ru.mirea.dentalclinic.repository.PatientRepository;
import ru.mirea.dentalclinic.service.AppointmentService;
import ru.mirea.dentalclinic.service.DentistService;
import ru.mirea.dentalclinic.service.PatientService;
import ru.mirea.dentalclinic.service.StatisticsService;
import ru.mirea.dentalclinic.ui.AnalyticsView;
import ru.mirea.dentalclinic.ui.AppointmentView;
import ru.mirea.dentalclinic.ui.ConsoleApp;
import ru.mirea.dentalclinic.ui.ConsoleReader;
import ru.mirea.dentalclinic.ui.DentistView;
import ru.mirea.dentalclinic.ui.PatientView;
import ru.mirea.dentalclinic.util.CsvExporter;
import ru.mirea.dentalclinic.util.DataExporter;
import ru.mirea.dentalclinic.util.DatabaseManager;
import ru.mirea.dentalclinic.util.DatabaseTablePrinter;
import ru.mirea.dentalclinic.util.ExcelExporter;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Scanner;

/**
 * Точка входа приложения.
 *
 * Здесь только сборка слоёв (Repository -> Service -> Console UI).
 * Вся логика вынесена в отдельные классы — реализовывать систему внутри Main запрещено.
 */
public class Main {

    public static void main(String[] args) {

        // 1. Подключение к базе данных (MySQL или PostgreSQL — по db.properties)
        try {
            DatabaseManager.init();
        } catch (AppException e) {
            System.out.println("Не удалось запустить приложение: " + e.getMessage());
            System.out.println("Проверьте, что сервер базы данных запущен, база создана скриптом");
            System.out.println("из папки db и параметры в src/main/resources/db.properties верны.");
            return;
        }

        // 2. Слой Repository (JDBC)
        PatientRepository patientRepository = new PatientRepository();
        DentistRepository dentistRepository = new DentistRepository();
        AppointmentRepository appointmentRepository = new AppointmentRepository();

        // 3. Слой Service (бизнес-логика и проверки)
        PatientService patientService = new PatientService(patientRepository, appointmentRepository);
        DentistService dentistService = new DentistService(dentistRepository, appointmentRepository);
        AppointmentService appointmentService =
                new AppointmentService(appointmentRepository, patientService, dentistService);
        StatisticsService statisticsService =
                new StatisticsService(patientService, dentistService, appointmentService);

        // Полиморфизм: два разных экспортёра за одним интерфейсом DataExporter
        List<DataExporter> exporters = List.of(new ExcelExporter(), new CsvExporter());

        // 4. Слой Console UI
        Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);
        ConsoleReader reader = new ConsoleReader(scanner);

        PatientView patientView = new PatientView(reader, patientService);
        DentistView dentistView = new DentistView(reader, dentistService);
        AppointmentView appointmentView =
                new AppointmentView(reader, appointmentService, patientService, dentistService);
        AnalyticsView analyticsView = new AnalyticsView(reader, appointmentService, patientService,
                dentistService, statisticsService, exporters, new DatabaseTablePrinter());

        new ConsoleApp(reader, patientView, dentistView, appointmentView, analyticsView).run();
    }
}
