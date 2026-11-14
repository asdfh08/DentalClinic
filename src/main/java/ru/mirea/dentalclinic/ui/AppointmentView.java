package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.AppointmentStatus;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.service.AppointmentService;
import ru.mirea.dentalclinic.service.DentistService;
import ru.mirea.dentalclinic.service.PatientService;
import ru.mirea.dentalclinic.util.Formats;

import java.math.BigDecimal;
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
            System.out.println("6. Изменить стоимость приёма");
            System.out.println("7. Удалить запись");
            System.out.println("0. Назад");

            int choice = reader.readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> safe(this::printAll);
                case 2 -> safe(this::create);
                case 3 -> safe(this::findById);
                case 4 -> safe(this::reschedule);
                case 5 -> safe(this::changeStatus);
                case 6 -> safe(this::changePrice);
                case 7 -> safe(this::delete);
                case 0 -> inMenu = false;
                default -> System.out.println("Ошибка: пункта меню " + choice + " не существует.");
            }
        }
    }

    private void printAll() {
        System.out.println(ConsoleFormat.appointmentsTable(appointmentService.findAll()));
    }

    private void create() {
        System.out.println(ConsoleFormat.patientsTable(patientService.findAll()));
        int patientId = reader.readInt("ID пациента: ");

        System.out.println(ConsoleFormat.dentistsTable(dentistService.findAll()));
        int dentistId = reader.readInt("ID врача: ");

        LocalDateTime time = reader.readDateTime("Дата и время приёма");

        ProcedureType procedureType = reader.readChoice("Выберите процедуру:",
                List.of(ProcedureType.values()), this::procedureLabel);
        if (procedureType == null) {
            System.out.println("Создание записи отменено.");
            return;
        }

        BigDecimal price = reader.readOptionalMoney("Стоимость (Enter — базовая "
                + Formats.money(procedureType.getBasePrice()) + "): ");
        String complaint = reader.readOptional("Жалоба пациента (Enter — пропустить): ");

        Appointment appointment = appointmentService.create(patientId, dentistId, time,
                procedureType, price, complaint);

        System.out.println("Запись создана.");
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
            newProcedure = reader.readChoice("Выберите новую процедуру:",
                    List.of(ProcedureType.values()), this::procedureLabel);
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

    private void changePrice() {
        int id = reader.readInt("Введите ID записи: ");
        Appointment appointment = appointmentService.getById(id);
        System.out.println("Текущая стоимость: " + Formats.money(appointment.getPrice()));

        BigDecimal price = reader.readOptionalMoney("Новая стоимость (Enter — базовая цена процедуры): ");
        Appointment updated = appointmentService.changePrice(id, price);
        System.out.println("Стоимость обновлена: " + Formats.money(updated.getPrice()));
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
