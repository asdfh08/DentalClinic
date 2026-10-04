package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.AppointmentStatus;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.service.AppointmentService;
import ru.mirea.dentalclinic.service.DentistService;
import ru.mirea.dentalclinic.service.PatientService;
import ru.mirea.dentalclinic.util.Formats;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Экран работы с основной сущностью — записями пациентов на приём.
 */
public class AppointmentView extends BaseView {

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DentistService dentistService;

    public AppointmentView(ConsoleReader reader,
                           AppointmentService appointmentService,
                           PatientService patientService,
                           DentistService dentistService) {
        super(reader);
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.dentistService = dentistService;
    }

    @Override
    protected String screenTitle() {
        return "Записи на приём";
    }

    public void menu() {
        boolean inMenu = true;
        while (inMenu) {
            printHeader();
            System.out.println("1. Все записи на приём");
            System.out.println("2. Создать запись");
            System.out.println("3. Показать запись по ID");
            System.out.println("4. Перенести запись / изменить процедуру");
            System.out.println("5. Изменить статус записи");
            System.out.println("6. Удалить запись");
            System.out.println("0. Назад");

            int choice = reader.readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> safe(this::printAll);
                case 2 -> safe(this::create);
                case 3 -> safe(this::findById);
                case 4 -> safe(this::reschedule);
                case 5 -> safe(this::changeStatus);
                case 6 -> safe(this::delete);
                case 0 -> inMenu = false;
                default -> System.out.println("Ошибка: пункта меню " + choice + " не существует.");
            }
        }
    }

    private void printAll() {
        System.out.println(ConsoleFormat.appointmentsTable(appointmentService.findAll()));
    }

    private void create() {
        // ID проверяются сразу после ввода, а не в конце анкеты
        System.out.println(ConsoleFormat.patientsTable(patientService.findAll()));
        Patient patient = patientService.getById(reader.readInt("ID пациента: "));

        System.out.println(ConsoleFormat.dentistsTable(dentistService.findAll()));
        Dentist dentist = dentistService.getById(reader.readInt("ID врача: "));

        // БП-10: предлагаются только процедуры специализации выбранного врача
        ProcedureType procedureType = reader.readChoice(
                "Процедуры врача " + dentist.getFullName()
                        + " (" + dentist.getSpecialization().getTitle() + "):",
                dentist.getSpecialization().getProcedures(), this::procedureLabel);
        if (procedureType == null) {
            System.out.println("Создание записи отменено.");
            return;
        }

        LocalDateTime time = reader.readDateTime("Дата и время приёма");
        String complaint = reader.readOptional("Жалоба пациента (Enter — пропустить): ");

        // стоимость не вводится: она берётся из прайса процедуры
        Appointment appointment = appointmentService.create(patient.getId(), dentist.getId(), time,
                procedureType, complaint);

        System.out.println("Запись создана. Стоимость по прайсу: " + Formats.money(appointment.getPrice()));
        System.out.println(ConsoleFormat.appointmentCard(appointment));
    }

    private void findById() {
        int id = reader.readInt("Введите ID записи: ");
        System.out.println(ConsoleFormat.appointmentCard(appointmentService.getById(id)));
    }

    private void reschedule() {
        int id = reader.readInt("Введите ID записи: ");
        Appointment current = appointmentService.getById(id);
        System.out.println(ConsoleFormat.appointmentCard(current));

        LocalDateTime newTime = reader.readOptionalDateTime("Новая дата и время приёма");

        ProcedureType newProcedure = null;
        if (reader.confirm("Изменить тип процедуры?")) {
            Dentist dentist = dentistService.getById(current.getDentistId());
            newProcedure = reader.readChoice(
                    "Процедуры врача " + dentist.getFullName()
                            + " (" + dentist.getSpecialization().getTitle() + "), цена изменится по прайсу:",
                    dentist.getSpecialization().getProcedures(), this::procedureLabel);
        }

        String newComplaint = reader.readOptional("Новая жалоба (Enter — не менять): ");

        Appointment updated = appointmentService.reschedule(id, newTime, newProcedure,
                newComplaint.isBlank() ? null : newComplaint);

        System.out.println("Запись обновлена.");
        System.out.println(ConsoleFormat.appointmentCard(updated));
    }

    private void changeStatus() {
        int id = reader.readInt("Введите ID записи: ");
        Appointment appointment = appointmentService.getById(id);
        System.out.println(ConsoleFormat.appointmentCard(appointment));

        List<AppointmentStatus> transitions = appointment.getStatus().allowedTransitions();
        if (transitions.isEmpty()) {
            System.out.println("Статус \"" + appointment.getStatus().getTitle()
                    + "\" является финальным, изменение невозможно.");
            return;
        }

        AppointmentStatus target = reader.readChoice("Доступные статусы:", transitions,
                status -> status.getTitle() + " [" + status.name() + "]");
        if (target == null) {
            System.out.println("Изменение статуса отменено.");
            return;
        }

        Appointment updated = appointmentService.changeStatus(id, target);
        System.out.println("Новый статус записи #" + updated.getId() + ": "
                + updated.getStatus().getTitle());
    }

    private void delete() {
        int id = reader.readInt("Введите ID записи для удаления: ");
        Appointment appointment = appointmentService.getById(id);
        System.out.println(ConsoleFormat.appointmentCard(appointment));

        if (!reader.confirm("Удалить эту запись?")) {
            System.out.println("Удаление отменено.");
            return;
        }

        appointmentService.delete(id);
        System.out.println("Запись #" + id + " удалена.");
    }

    private String procedureLabel(ProcedureType procedureType) {
        return procedureType.getTitle() + " — " + procedureType.getDurationMinutes() + " мин, "
                + Formats.money(procedureType.getBasePrice());
    }
}
