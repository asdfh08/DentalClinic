package ru.mirea.dentalclinic;

import ru.mirea.dentalclinic.exception.ClinicException;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.repository.AppointmentRepository;
import ru.mirea.dentalclinic.repository.DentistRepository;
import ru.mirea.dentalclinic.repository.PatientRepository;
import ru.mirea.dentalclinic.service.AppointmentService;
import ru.mirea.dentalclinic.service.DentistService;
import ru.mirea.dentalclinic.service.PatientService;
import ru.mirea.dentalclinic.ui.AppointmentView;
import ru.mirea.dentalclinic.ui.ConsoleReader;
import ru.mirea.dentalclinic.ui.DentistView;
import ru.mirea.dentalclinic.ui.PatientView;
import ru.mirea.dentalclinic.util.DatabaseManager;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Точка входа консольного приложения. Здесь собираются все слои:
 * база данных -> репозитории -> сервисы -> консольные экраны.
 */
public class Main {

    public static void main(String[] args) {
        DatabaseManager.init();

        PatientRepository patientRepository = new PatientRepository();
        DentistRepository dentistRepository = new DentistRepository();
        AppointmentRepository appointmentRepository = new AppointmentRepository();

        PatientService patientService = new PatientService(patientRepository, appointmentRepository);
        DentistService dentistService = new DentistService(dentistRepository, appointmentRepository);
        AppointmentService appointmentService =
                new AppointmentService(appointmentRepository, patientService, dentistService);

        seedDemoData(patientService, dentistService, appointmentService);

        ConsoleReader reader = new ConsoleReader(System.in);
        PatientView patientView = new PatientView(reader, patientService);
        DentistView dentistView = new DentistView(reader, dentistService);
        AppointmentView appointmentView =
                new AppointmentView(reader, appointmentService, patientService, dentistService);

        System.out.println("Стоматологическая клиника");
        boolean running = true;
        while (running) {
            System.out.println();
            System.out.println("===== Главное меню =====");
            System.out.println("1. Пациенты");
            System.out.println("2. Врачи");
            System.out.println("3. Записи на приём");
            System.out.println("0. Выход");

            int choice = reader.readInt("Выберите раздел: ");
            switch (choice) {
                case 1 -> patientView.menu();
                case 2 -> dentistView.menu();
                case 3 -> appointmentView.menu();
                case 0 -> running = false;
                default -> System.out.println("Ошибка: пункта меню " + choice + " не существует.");
            }
        }
        System.out.println("До свидания!");
    }

    /** При пустой базе добавляет несколько демонстрационных записей. */
    private static void seedDemoData(PatientService patientService, DentistService dentistService,
                                     AppointmentService appointmentService) {
        if (patientService.count() > 0 || dentistService.count() > 0) {
            return;
        }
        try {
            Dentist therapist = dentistService.create("Иванов Пётр Сергеевич", "+74951112233", "Терапевт", 101);
            Dentist surgeon = dentistService.create("Соколова Анна Викторовна", "+74951112244", "Хирург", 102);

            Patient first = patientService.create("Смирнов Алексей Игоревич", "+79001234567",
                    "smirnov@example.com", LocalDate.of(1990, 5, 14));
            Patient second = patientService.create("Кузнецова Мария Олеговна", "+79007654321",
                    null, LocalDate.of(1985, 11, 2));

            LocalDate day = LocalDate.now().plusDays(1);
            if (day.getDayOfWeek() == DayOfWeek.SUNDAY) {
                day = day.plusDays(1);
            }
            LocalDateTime time = day.atTime(10, 0);

            appointmentService.create(first.getId(), therapist.getId(), time,
                    ProcedureType.CONSULTATION, null, "Болит зуб от холодного");
            appointmentService.create(second.getId(), surgeon.getId(), time.plusHours(2),
                    ProcedureType.EXTRACTION, null, null);
        } catch (ClinicException e) {
            System.out.println("Не удалось добавить демонстрационные данные: " + e.getMessage());
        }
    }
}
