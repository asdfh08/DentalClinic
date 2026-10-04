package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.AppointmentStatus;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.service.AppointmentService;
import ru.mirea.dentalclinic.service.AppointmentSort;
import ru.mirea.dentalclinic.service.DentistService;
import ru.mirea.dentalclinic.service.PatientService;
import ru.mirea.dentalclinic.service.StatisticsService;
import ru.mirea.dentalclinic.util.DataExporter;
import ru.mirea.dentalclinic.util.DatabaseTablePrinter;
import ru.mirea.dentalclinic.util.ExportData;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/**
 * Экран поиска, фильтрации, сортировки, статистики и экспорта данных.
 */
public class AnalyticsView extends BaseView {

    private static final Path EXPORT_DIRECTORY = Path.of("export");

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DentistService dentistService;
    private final StatisticsService statisticsService;
    private final List<DataExporter> exporters;
    private final DatabaseTablePrinter tablePrinter;

    public AnalyticsView(ConsoleReader reader,
                         AppointmentService appointmentService,
                         PatientService patientService,
                         DentistService dentistService,
                         StatisticsService statisticsService,
                         List<DataExporter> exporters,
                         DatabaseTablePrinter tablePrinter) {
        super(reader);
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.dentistService = dentistService;
        this.statisticsService = statisticsService;
        this.exporters = exporters;
        this.tablePrinter = tablePrinter;
    }

    @Override
    protected String screenTitle() {
        return "Поиск, фильтрация и отчёты";
    }

    // ------------------------------------------------------------------
    // Поиск (5 способов)
    // ------------------------------------------------------------------

    public void searchMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println(ConsoleFormat.subTitle("Поиск"));
            System.out.println("1. Записи по ФИО пациента");
            System.out.println("2. Записи по телефону пациента");
            System.out.println("3. Записи по дате приёма");
            System.out.println("4. Записи по врачу");
            System.out.println("5. Записи по тексту жалобы");
            System.out.println("6. Пациенты по ФИО");
            System.out.println("7. Пациенты по телефону");
            System.out.println("0. Назад");

            int choice = reader.readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> safe(() -> printAppointments(appointmentService.searchByPatientName(
                        reader.readRequired("Часть ФИО пациента: "))));
                case 2 -> safe(() -> printAppointments(appointmentService.searchByPatientPhone(
                        reader.readRequired("Часть телефона: "))));
                case 3 -> safe(() -> printAppointments(appointmentService.searchByDate(
                        reader.readDate("Дата приёма"))));
                case 4 -> safe(this::searchByDentist);
                case 5 -> safe(() -> printAppointments(appointmentService.searchByComplaint(
                        reader.readRequired("Часть текста жалобы: "))));
                case 6 -> safe(() -> System.out.println(ConsoleFormat.patientsTable(
                        patientService.searchByName(reader.readRequired("Часть ФИО: ")))));
                case 7 -> safe(() -> System.out.println(ConsoleFormat.patientsTable(
                        patientService.searchByPhone(reader.readRequired("Часть телефона: ")))));
                case 0 -> inMenu = false;
                default -> System.out.println("Ошибка: пункта меню " + choice + " не существует.");
            }
        }
    }

    private void searchByDentist() {
        System.out.println(ConsoleFormat.dentistsTable(dentistService.findAll()));
        int dentistId = reader.readInt("ID врача: ");
        printAppointments(appointmentService.searchByDentist(dentistId));
    }

    // ------------------------------------------------------------------
    // Фильтрация (5 фильтров)
    // ------------------------------------------------------------------

    public void filterMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println(ConsoleFormat.subTitle("Фильтрация"));
            System.out.println("1. По статусу записи");
            System.out.println("2. По типу процедуры");
            System.out.println("3. Только активные записи");
            System.out.println("4. По диапазону дат");
            System.out.println("5. По минимальной стоимости");
            System.out.println("0. Назад");

            int choice = reader.readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> safe(this::filterByStatus);
                case 2 -> safe(this::filterByProcedure);
                case 3 -> safe(() -> printAppointments(appointmentService.filterActive()));
                case 4 -> safe(this::filterByPeriod);
                case 5 -> safe(this::filterByMinPrice);
                case 0 -> inMenu = false;
                default -> System.out.println("Ошибка: пункта меню " + choice + " не существует.");
            }
        }
    }

    private void filterByStatus() {
        AppointmentStatus status = reader.readChoice("Выберите статус:",
                List.of(AppointmentStatus.values()),
                value -> value.getTitle() + " [" + value.name() + "]");
        if (status == null) {
            return;
        }
        printAppointments(appointmentService.filterByStatus(status));
    }

    private void filterByProcedure() {
        ProcedureType procedureType = reader.readChoice("Выберите тип процедуры:",
                List.of(ProcedureType.values()),
                value -> value.getTitle() + " [" + value.name() + "]");
        if (procedureType == null) {
            return;
        }
        printAppointments(appointmentService.filterByProcedure(procedureType));
    }

    private void filterByPeriod() {
        LocalDate from = reader.readDate("Дата начала периода");
        LocalDate to = reader.readDate("Дата окончания периода");
        printAppointments(appointmentService.filterByPeriod(from, to));
    }

    private void filterByMinPrice() {
        BigDecimal minPrice = reader.readOptionalMoney("Минимальная стоимость приёма (Enter — 0): ");
        printAppointments(appointmentService.filterByMinPrice(
                minPrice == null ? BigDecimal.ZERO : minPrice));
    }

    // ------------------------------------------------------------------
    // Сортировка (6 способов, см. enum AppointmentSort)
    // ------------------------------------------------------------------

    public void sortMenu() {
        System.out.println(ConsoleFormat.subTitle("Сортировка"));
        AppointmentSort sort = reader.readChoice("Выберите способ сортировки:",
                List.of(AppointmentSort.values()), AppointmentSort::getTitle);
        if (sort == null) {
            return;
        }
        System.out.println("Сортировка: " + sort.getTitle());
        printAppointments(appointmentService.findAllSorted(sort));
    }

    // ------------------------------------------------------------------
    // Статистика
    // ------------------------------------------------------------------

    public void showStatistics() {
        System.out.println(ConsoleFormat.subTitle("Статистика системы"));
        System.out.print(ConsoleFormat.statistics(statisticsService.collect()));
    }

    // ------------------------------------------------------------------
    // Экспорт данных
    // ------------------------------------------------------------------

    public void exportMenu() {
        System.out.println(ConsoleFormat.subTitle("Экспорт данных"));

        DataExporter exporter = reader.readChoice("Выберите формат выгрузки:",
                exporters, DataExporter::formatName);
        if (exporter == null) {
            return;
        }

        ExportData data = new ExportData(
                patientService.findAll(),
                dentistService.findAll(),
                appointmentService.findAll(),
                statisticsService.collect());

        try {
            List<Path> files = exporter.export(data, EXPORT_DIRECTORY);
            System.out.println("Выгрузка в формате " + exporter.formatName() + " завершена:");
            for (Path file : files) {
                System.out.println("  " + file.toAbsolutePath());
            }
        } catch (IOException e) {
            System.out.println("Ошибка: не удалось записать файл выгрузки — " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // Таблицы базы данных
    // ------------------------------------------------------------------

    public void showDatabaseTables() {
        System.out.println(ConsoleFormat.subTitle("Таблицы базы данных"));
        System.out.println(tablePrinter.describeAllTables());
    }

    private void printAppointments(List<Appointment> appointments) {
        System.out.println(ConsoleFormat.appointmentsTable(appointments));
    }
}
