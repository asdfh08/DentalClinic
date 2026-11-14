package ru.mirea.dentalclinic.ui;

import ru.mirea.dentalclinic.model.AgeCategory;
import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.service.PatientService;
import ru.mirea.dentalclinic.service.PatientSort;

import java.time.LocalDate;
import java.util.List;

/**
 * Экран работы с пациентами: CRUD, поиск, сортировка в обе стороны и фильтрация.
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
            System.out.println("6. Поиск пациентов");
            System.out.println("7. Сортировка пациентов");
            System.out.println("8. Фильтрация пациентов");
            System.out.println("0. Назад");

            int choice = reader.readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> safe(this::printAll);
                case 2 -> safe(this::create);
                case 3 -> safe(this::findById);
                case 4 -> safe(this::update);
                case 5 -> safe(this::delete);
                case 6 -> safe(this::search);
                case 7 -> safe(this::sort);
                case 8 -> safe(this::filterMenu);
                case 0 -> inMenu = false;
                default -> System.out.println("Ошибка: пункта меню " + choice + " не существует.");
            }
        }
    }

    private void printAll() {
        System.out.println(ConsoleFormat.patientsTable(patientService.findAll()));
    }

    private void print(List<Patient> patients) {
        System.out.println(ConsoleFormat.patientsTable(patients));
    }

    // ------------------------------------------------------------------
    // Поиск
    // ------------------------------------------------------------------

    private void search() {
        System.out.println(ConsoleFormat.subTitle("Поиск пациентов"));
        System.out.println("1. По части ФИО");
        System.out.println("2. По части номера телефона");
        System.out.println("0. Назад");

        int choice = reader.readIntInRange("Выберите действие: ", 0, 2);
        switch (choice) {
            case 1 -> print(patientService.searchByName(reader.readRequired("Часть ФИО: ")));
            case 2 -> print(patientService.searchByPhone(reader.readRequired("Часть телефона: ")));
            default -> { }
        }
    }

    // ------------------------------------------------------------------
    // Сортировка: прямая и обратная по ФИО, возрасту и дате регистрации
    // ------------------------------------------------------------------

    private void sort() {
        System.out.println(ConsoleFormat.subTitle("Сортировка пациентов"));
        PatientSort sort = reader.readChoice("Выберите способ сортировки:",
                List.of(PatientSort.values()), PatientSort::getTitle);
        if (sort == null) {
            return;
        }
        System.out.println("Сортировка: " + sort.getTitle());
        print(patientService.findAllSorted(sort));
    }

    // ------------------------------------------------------------------
    // Фильтрация
    // ------------------------------------------------------------------

    private void filterMenu() {
        boolean inMenu = true;
        while (inMenu) {
            System.out.println(ConsoleFormat.subTitle("Фильтрация пациентов"));
            System.out.println("1. По возрастной категории");
            System.out.println("2. По диапазону возраста");
            System.out.println("3. По наличию e-mail");
            System.out.println("4. Только пациенты с активными записями на приём");
            System.out.println("0. Назад");

            int choice = reader.readInt("Выберите действие: ");
            switch (choice) {
                case 1 -> safe(this::filterByAgeCategory);
                case 2 -> safe(this::filterByAgeRange);
                case 3 -> safe(this::filterByEmail);
                case 4 -> safe(() -> print(patientService.filterWithActiveAppointments()));
                case 0 -> inMenu = false;
                default -> System.out.println("Ошибка: пункта меню " + choice + " не существует.");
            }
        }
    }

    private void filterByAgeCategory() {
        AgeCategory category = reader.readChoice("Выберите возрастную категорию:",
                List.of(AgeCategory.values()), AgeCategory::getTitle);
        if (category == null) {
            return;
        }
        System.out.println("Фильтр: " + category.getTitle());
        print(patientService.filterByAgeCategory(category));
    }

    private void filterByAgeRange() {
        int minAge = reader.readInt("Минимальный возраст, лет: ");
        int maxAge = reader.readInt("Максимальный возраст, лет: ");
        print(patientService.filterByAgeRange(minAge, maxAge));
    }

    private void filterByEmail() {
        boolean hasEmail = reader.confirm("Показать пациентов, у которых e-mail указан?"
                + " (нет — у которых не указан)");
        System.out.println("Фильтр: e-mail " + (hasEmail ? "указан" : "не указан"));
        print(patientService.filterByEmailPresence(hasEmail));
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
        System.out.println("Возрастная категория: " + patient.getAgeCategory().getTitle());
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
