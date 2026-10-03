package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.service.PatientService;

import java.time.LocalDate;

/**
 * Экран работы с пациентами (CRUD + сортировка).
 */
public class PatientView extends BaseView {

    private final PatientService patientService;

    public PatientView(ConsoleReader reader, PatientService patientService) {
        super(reader);
        this.patientService = patientService;
    }

    @Override
    protected String screenTitle() {
        return "Пациенты";
    }

    public void menu() {
        boolean inMenu = true;
        while (inMenu) {
            printHeader();
            System.out.println("1. Список всех пациентов");
            System.out.println("2. Добавить пациента");
            System.out.println("3. Найти пациента по ID");
            System.out.println("4. Изменить данные пациента");
            System.out.println("5. Удалить пациента");
            System.out.println("6. Список пациентов по возрасту (сортировка)");
            System.out.println("0. Назад");

            int choice = reader.readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> safe(this::printAll);
                case 2 -> safe(this::create);
                case 3 -> safe(this::findById);
                case 4 -> safe(this::update);
                case 5 -> safe(this::delete);
                case 6 -> safe(this::printSortedByAge);
                case 0 -> inMenu = false;
                default -> System.out.println("Ошибка: пункта меню " + choice + " не существует.");
            }
        }
    }

    private void printAll() {
        System.out.println(ConsoleFormat.patientsTable(patientService.findAll()));
    }

    private void printSortedByAge() {
        System.out.println("Сортировка: от самого старшего пациента к самому младшему");
        System.out.println(ConsoleFormat.patientsTable(patientService.findAllSortedByAge()));
    }

    private void create() {
        System.out.println("Новый пациент:");
        String fullName = reader.readRequired("ФИО: ");
        String phone = reader.readRequired("Телефон (например +79001234567): ");
        String email = reader.readOptional("E-mail (Enter — пропустить): ");
        LocalDate birthDate = reader.readDate("Дата рождения");

        Patient patient = patientService.create(fullName, phone, email, birthDate);
        System.out.println("Пациент добавлен: " + patient.describe());
    }

    private void findById() {
        int id = reader.readInt("Введите ID пациента: ");
        Patient patient = patientService.getById(id);
        System.out.println(patient.describe());
        System.out.println("Записей на приём у пациента: " + patientService.countAppointments(id));
    }

    private void update() {
        int id = reader.readInt("Введите ID пациента: ");
        Patient current = patientService.getById(id);
        System.out.println("Текущие данные: " + current.describe());
        System.out.println("Пустое поле — оставить прежнее значение.");

        String fullName = reader.readOptional("Новое ФИО: ");
        String phone = reader.readOptional("Новый телефон: ");
        String email = reader.readOptional("Новый e-mail: ");
        LocalDate birthDate = reader.readOptionalDate("Новая дата рождения");

        Patient updated = patientService.update(id,
                fullName.isBlank() ? current.getFullName() : fullName,
                phone.isBlank() ? current.getPhone() : phone,
                email.isBlank() ? current.getEmail() : email,
                birthDate == null ? current.getBirthDate() : birthDate);

        System.out.println("Данные обновлены: " + updated.describe());
    }

    private void delete() {
        int id = reader.readInt("Введите ID пациента для удаления: ");
        Patient patient = patientService.getById(id);
        int appointments = patientService.countAppointments(id);

        System.out.println("Будет удалён: " + patient.describe());
        if (appointments > 0) {
            System.out.println("Внимание: вместе с пациентом будет удалена история его приёмов ("
                    + appointments + " шт.).");
        }
        if (!reader.confirm("Подтвердите удаление")) {
            System.out.println("Удаление отменено.");
            return;
        }

        patientService.delete(id);
        System.out.println("Пациент удалён.");
    }
}
