package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Specialization;
import ru.mirea.dentalclinic.service.DentistService;

import java.util.List;

/**
 * Экран работы с врачами-стоматологами.
 */
public class DentistView extends BaseView {

    private final DentistService dentistService;

    public DentistView(ConsoleReader reader, DentistService dentistService) {
        super(reader);
        this.dentistService = dentistService;
    }

    @Override
    protected String screenTitle() {
        return "Врачи-стоматологи";
    }

    public void menu() {
        boolean inMenu = true;
        while (inMenu) {
            printHeader();
            System.out.println("1. Список всех врачей");
            System.out.println("2. Добавить врача");
            System.out.println("3. Найти врача по ID");
            System.out.println("4. Изменить данные врача");
            System.out.println("5. Удалить врача");
            System.out.println("0. Назад");

            int choice = reader.readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> safe(this::printAll);
                case 2 -> safe(this::create);
                case 3 -> safe(this::findById);
                case 4 -> safe(this::update);
                case 5 -> safe(this::delete);
                case 0 -> inMenu = false;
                default -> System.out.println("Ошибка: пункта меню " + choice + " не существует.");
            }
        }
    }

    private void printAll() {
        System.out.println(ConsoleFormat.dentistsTable(dentistService.findAll()));
    }

    private void create() {
        System.out.println("Новый врач:");
        String fullName = reader.readRequired("ФИО: ");
        String phone = reader.readRequired("Телефон: ");
        Specialization specialization = reader.readChoice("Специализация:",
                List.of(Specialization.values()), this::specializationLabel);
        if (specialization == null) {
            System.out.println("Добавление врача отменено.");
            return;
        }
        int cabinet = reader.readInt("Номер кабинета: ");

        Dentist dentist = dentistService.create(fullName, phone, specialization, cabinet);
        System.out.println("Врач добавлен: " + dentist.describe());
    }

    private void findById() {
        int id = reader.readInt("Введите ID врача: ");
        Dentist dentist = dentistService.getById(id);
        System.out.println(dentist.describe());
        System.out.println("Выполняет процедуры: " + dentist.getSpecialization().procedureTitles());
    }

    private void update() {
        int id = reader.readInt("Введите ID врача: ");
        Dentist current = dentistService.getById(id);
        System.out.println("Текущие данные: " + current.describe());
        System.out.println("Пустое поле — оставить прежнее значение.");

        String fullName = reader.readOptional("Новое ФИО: ");
        String phone = reader.readOptional("Новый телефон: ");
        Specialization specialization = current.getSpecialization();
        if (reader.confirm("Изменить специализацию?")) {
            Specialization chosen = reader.readChoice("Новая специализация:",
                    List.of(Specialization.values()), this::specializationLabel);
            if (chosen != null) {
                specialization = chosen;
            }
        }
        String cabinetInput = reader.readOptional("Новый кабинет: ");

        int cabinet = current.getCabinet();
        if (!cabinetInput.isBlank()) {
            try {
                cabinet = Integer.parseInt(cabinetInput);
            } catch (NumberFormatException e) {
                System.out.println("Ошибка: номер кабинета должен быть целым числом, значение не изменено.");
            }
        }

        Dentist updated = dentistService.update(id,
                fullName.isBlank() ? current.getFullName() : fullName,
                phone.isBlank() ? current.getPhone() : phone,
                specialization,
                cabinet);

        System.out.println("Данные обновлены: " + updated.describe());
    }

    /** Специализация и перечень процедур, которые она разрешает. */
    private String specializationLabel(Specialization specialization) {
        return specialization.getTitle() + " — " + specialization.procedureTitles();
    }

    private void delete() {
        int id = reader.readInt("Введите ID врача для удаления: ");
        Dentist dentist = dentistService.getById(id);

        System.out.println("Будет удалён: " + dentist.describe());
        if (!reader.confirm("Подтвердите удаление")) {
            System.out.println("Удаление отменено.");
            return;
        }

        dentistService.delete(id);
        System.out.println("Врач удалён.");
    }
}
