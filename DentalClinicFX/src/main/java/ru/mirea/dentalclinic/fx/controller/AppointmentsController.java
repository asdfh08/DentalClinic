package ru.mirea.dentalclinic.fx.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import ru.mirea.dentalclinic.exception.ValidationException;
import ru.mirea.dentalclinic.fx.AppContext;
import ru.mirea.dentalclinic.fx.Combos;
import ru.mirea.dentalclinic.fx.DatePickers;
import ru.mirea.dentalclinic.fx.Dialogs;
import ru.mirea.dentalclinic.fx.TableColumns;
import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.AppointmentStatus;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.service.AppointmentFilter;
import ru.mirea.dentalclinic.service.AppointmentService;
import ru.mirea.dentalclinic.service.CsvExportService;
import ru.mirea.dentalclinic.service.DentistService;
import ru.mirea.dentalclinic.service.PatientService;
import ru.mirea.dentalclinic.util.Formats;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Контроллер вкладки "Записи на приём" (appointments.fxml).
 *
 * Данные: ObservableList (все записи из базы) -> FilteredList (поиск и фильтры)
 * -> SortedList (сортировка) -> TableView. При изменении списка или условий
 * таблица обновляется автоматически, потому что TableView наблюдает за ObservableList.
 *
 * Контроллер не содержит SQL и бизнес-правил: он вызывает методы сервисов,
 * а ошибки показывает через {@link Dialogs}.
 */
public class AppointmentsController {

    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    private final AppContext context;
    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DentistService dentistService;
    private final CsvExportService csvExportService;

    /** Все записи из базы данных. */
    private final ObservableList<Appointment> appointments = FXCollections.observableArrayList();
    private final FilteredList<Appointment> filtered = new FilteredList<>(appointments, appointment -> true);
    private final SortedList<Appointment> sorted = new SortedList<>(filtered);

    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;
    @FXML
    private Label countLabel;
    @FXML
    private TextField searchField;
    @FXML
    private ComboBox<AppointmentStatus> statusFilter;
    @FXML
    private ComboBox<Dentist> dentistFilter;
    @FXML
    private ComboBox<ProcedureType> procedureFilter;
    @FXML
    private DatePicker dateFromFilter;
    @FXML
    private DatePicker dateToFilter;
    @FXML
    private ComboBox<SortOption> sortBox;
    @FXML
    private Label filterErrorLabel;
    @FXML
    private TableView<Appointment> appointmentsTable;
    @FXML
    private TableColumn<Appointment, Integer> idColumn;
    @FXML
    private TableColumn<Appointment, LocalDateTime> timeColumn;
    @FXML
    private TableColumn<Appointment, String> patientColumn;
    @FXML
    private TableColumn<Appointment, String> dentistColumn;
    @FXML
    private TableColumn<Appointment, ProcedureType> procedureColumn;
    @FXML
    private TableColumn<Appointment, AppointmentStatus> statusColumn;
    @FXML
    private TableColumn<Appointment, BigDecimal> priceColumn;
    @FXML
    private TableColumn<Appointment, String> complaintColumn;
    @FXML
    private HBox detailsBox;

    public AppointmentsController(AppContext context, AppointmentService appointmentService,
                                  PatientService patientService, DentistService dentistService,
                                  CsvExportService csvExportService) {
        this.context = context;
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.dentistService = dentistService;
        this.csvExportService = csvExportService;
    }

    /** Вызывается FXMLLoader после того, как все поля с @FXML заполнены. */
    @FXML
    private void initialize() {
        setUpTable();
        setUpFilters();

        // Кнопки "Изменить" и "Удалить" доступны только при выбранной строке
        editButton.disableProperty().bind(appointmentsTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(appointmentsTable.getSelectionModel().selectedItemProperty().isNull());

        refresh();
    }

    // ------------------------------------------------------------------
    // Настройка таблицы
    // ------------------------------------------------------------------

    private void setUpTable() {
        TableColumns.value(idColumn, Appointment::getId);
        TableColumns.dateTime(timeColumn, Appointment::getAppointmentTime);
        TableColumns.value(patientColumn, Appointment::getPatientName);
        TableColumns.value(dentistColumn, Appointment::getDentistName);
        TableColumns.titled(procedureColumn, Appointment::getProcedureType, ProcedureType::getTitle);
        TableColumns.status(statusColumn, Appointment::getStatus);
        TableColumns.money(priceColumn, Appointment::getPrice);
        TableColumns.value(complaintColumn, Appointment::getComplaint);

        // Встроенная сортировка TableView: щелчок по заголовку меняет comparator таблицы,
        // SortedList берёт этот comparator и пересортировывает данные.
        sorted.comparatorProperty().bind(appointmentsTable.comparatorProperty());
        appointmentsTable.setItems(sorted);
        appointmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        appointmentsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, selected) -> showDetails(selected));

        // Двойной щелчок по строке открывает форму редактирования
        appointmentsTable.setRowFactory(table -> {
            TableRow<Appointment> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() == 2 && !row.isEmpty()) {
                    onEdit();
                }
            });
            return row;
        });
    }

    private void setUpFilters() {
        DatePickers.useDayMonthYear(dateFromFilter);
        DatePickers.useDayMonthYear(dateToFilter);
        Combos.withAll(statusFilter, List.of(AppointmentStatus.values()), AppointmentStatus::getTitle, "Все статусы");
        Combos.withAll(procedureFilter, List.of(ProcedureType.values()), ProcedureType::getTitle, "Все процедуры");
        Combos.withAll(dentistFilter, List.of(), Dentist::getFullName, "Все врачи");

        // Сортировка минимум по двум параметрам: выбор в списке включает встроенную сортировку столбца
        sortBox.getItems().setAll(
                new SortOption("Дата приёма: сначала ранние", timeColumn, TableColumn.SortType.ASCENDING),
                new SortOption("Дата приёма: сначала поздние", timeColumn, TableColumn.SortType.DESCENDING),
                new SortOption("Стоимость: по возрастанию", priceColumn, TableColumn.SortType.ASCENDING),
                new SortOption("Стоимость: по убыванию", priceColumn, TableColumn.SortType.DESCENDING),
                new SortOption("Пациент: А-Я", patientColumn, TableColumn.SortType.ASCENDING),
                new SortOption("Пациент: Я-А", patientColumn, TableColumn.SortType.DESCENDING),
                new SortOption("Статус: от «Создана» к «Неявка»", statusColumn, TableColumn.SortType.ASCENDING));
        Combos.display(sortBox, SortOption::title);
        sortBox.valueProperty().addListener((obs, oldValue, option) -> applySort(option));
        sortBox.getSelectionModel().selectFirst();

        // Поиск и фильтры применяются сразу при изменении любого поля
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        statusFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        dentistFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        procedureFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        dateFromFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
        dateToFilter.valueProperty().addListener((obs, oldValue, newValue) -> applyFilter());
    }

    private void applySort(SortOption option) {
        if (option == null) {
            return;
        }
        option.column().setSortType(option.sortType());
        appointmentsTable.getSortOrder().setAll(List.of(option.column()));
    }

    private void applyFilter() {
        Dentist dentist = dentistFilter.getValue();
        try {
            AppointmentFilter filter = new AppointmentFilter(searchField.getText(), statusFilter.getValue(),
                    dentist == null ? null : dentist.getId(), procedureFilter.getValue(),
                    dateFromFilter.getValue(), dateToFilter.getValue());
            filtered.setPredicate(filter);
            showFilterError(null);
        } catch (ValidationException e) {
            showFilterError(Dialogs.sentence(e.getMessage()));
        }
        updateCount();
    }

    private void showFilterError(String message) {
        boolean hasError = message != null;
        filterErrorLabel.setText(message);
        filterErrorLabel.setVisible(hasError);
        filterErrorLabel.setManaged(hasError);
        for (DatePicker picker : List.of(dateFromFilter, dateToFilter)) {
            picker.getStyleClass().remove("field-error");
            if (hasError) {
                picker.getStyleClass().add("field-error");
            }
        }
    }

    // ------------------------------------------------------------------
    // Действия пользователя
    // ------------------------------------------------------------------

    /** Перечитать данные из базы (кнопка "Обновить", переход на вкладку, после сохранения). */
    public void refresh() {
        Dialogs.run(window(), "Не удалось загрузить записи на приём", () -> {
            Appointment selected = appointmentsTable.getSelectionModel().getSelectedItem();
            reloadDentistFilter();
            appointments.setAll(appointmentService.findAll());
            applyFilter();
            select(selected == null ? 0 : selected.getId());
        });
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    @FXML
    private void onAdd() {
        openForm(null);
    }

    @FXML
    private void onEdit() {
        Appointment selected = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openForm(selected);
        }
    }

    /** Удаление с обязательным подтверждением. */
    @FXML
    private void onDelete() {
        Appointment selected = appointmentsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        boolean confirmed = Dialogs.confirmDelete(window(), "Удалить запись №" + selected.getId() + "?",
                selected.getPatientName() + " — " + selected.getDentistName() + "\n"
                        + Formats.dateTime(selected.getAppointmentTime()) + ", "
                        + selected.getProcedureType().getTitle() + "\n\n"
                        + "Удалённую запись восстановить нельзя.");
        if (!confirmed) {
            return;
        }
        Dialogs.run(window(), "Запись не удалена", () -> {
            appointmentService.delete(selected.getId());
            refresh();
        });
    }

    @FXML
    private void onResetFilters() {
        searchField.clear();
        statusFilter.setValue(null);
        dentistFilter.setValue(null);
        procedureFilter.setValue(null);
        dateFromFilter.setValue(null);
        dateToFilter.setValue(null);
        sortBox.getSelectionModel().selectFirst();
    }

    /** Экспорт текущего набора данных таблицы в .csv; путь выбирает пользователь. */
    @FXML
    private void onExport() {
        Dialogs.run(window(), "Экспорт не выполнен", () -> {
            List<Appointment> rows = new ArrayList<>(sorted);
            if (rows.isEmpty()) {
                throw new ValidationException("нет данных для экспорта: в таблице нет ни одной записи");
            }

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Сохранить записи на приём в CSV");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Файл CSV (*.csv)", "*.csv"));
            chooser.setInitialFileName("appointments_" + LocalDateTime.now().format(FILE_TIME) + ".csv");

            File file = chooser.showSaveDialog(window());
            if (file == null) {
                return; // пользователь закрыл окно выбора файла
            }
            Path path = file.toPath();
            if (!path.getFileName().toString().toLowerCase().endsWith(".csv")) {
                path = path.resolveSibling(path.getFileName() + ".csv");
            }

            int count = csvExportService.exportAppointments(rows, path);
            Dialogs.info(window(), "Экспорт выполнен",
                    "Выгружено записей: " + count + "\nФайл: " + path.toAbsolutePath());
        });
    }

    // ------------------------------------------------------------------
    // Форма добавления и редактирования
    // ------------------------------------------------------------------

    private void openForm(Appointment appointment) {
        Dialogs.run(window(), "Не удалось открыть форму записи", () -> {
            FXMLLoader loader = context.loader("appointment-form.fxml");
            Parent root = context.load(loader);
            AppointmentFormController form = loader.getController();

            Stage stage = new Stage();
            stage.initOwner(window());
            stage.initModality(Modality.WINDOW_MODAL);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(context.stylesheet());
            stage.setScene(scene);
            stage.setMinWidth(560);

            if (appointment == null) {
                stage.setTitle("Новая запись на приём");
                form.openForCreate();
            } else {
                stage.setTitle("Изменение записи №" + appointment.getId());
                form.openForEdit(appointmentService.getById(appointment.getId()));
            }
            stage.showAndWait();

            if (form.getSavedId() > 0) {
                refresh();
                select(form.getSavedId());
            }
        });
    }

    // ------------------------------------------------------------------
    // Карточка выбранной записи (связанные данные)
    // ------------------------------------------------------------------

    private void showDetails(Appointment appointment) {
        detailsBox.getChildren().clear();
        if (appointment == null) {
            Label empty = new Label("Выберите запись в таблице, чтобы увидеть сведения о приёме, пациенте и враче.");
            empty.setWrapText(true);
            empty.getStyleClass().add("hint-label");
            detailsBox.getChildren().add(empty);
            return;
        }

        VBox record = card("Запись №" + appointment.getId());
        row(record, "Начало", Formats.dateTime(appointment.getAppointmentTime()));
        row(record, "Окончание", Formats.time(appointment.getEndTime()));
        row(record, "Процедура", appointment.getProcedureType().getTitle()
                + " (" + appointment.getProcedureType().getDurationMinutes() + " мин)");
        row(record, "Статус", appointment.getStatus().getTitle());
        row(record, "Стоимость", Formats.money(appointment.getPrice()));
        row(record, "Жалоба", appointment.getComplaint().isBlank() ? "не указана" : appointment.getComplaint());

        Dialogs.run(window(), "Не удалось загрузить связанные данные", () -> {
            Patient patient = patientService.getById(appointment.getPatientId());
            VBox patientCard = card("Пациент");
            row(patientCard, "ФИО", patient.getFullName());
            row(patientCard, "Телефон", patient.getPhone());
            row(patientCard, "E-mail", patient.getEmail() == null || patient.getEmail().isBlank()
                    ? "не указан" : patient.getEmail());
            row(patientCard, "Дата рождения", Formats.date(patient.getBirthDate()));
            row(patientCard, "Возраст", String.valueOf(patient.getAge()));
            row(patientCard, "Категория", patient.getAgeCategory().getTitle());

            Dentist dentist = dentistService.getById(appointment.getDentistId());
            VBox dentistCard = card("Врач");
            row(dentistCard, "ФИО", dentist.getFullName());
            row(dentistCard, "Специализация", dentist.getSpecialization().getTitle());
            row(dentistCard, "Кабинет", String.valueOf(dentist.getCabinet()));
            row(dentistCard, "Телефон", dentist.getPhone());
        });
    }

    /** Карточка сведений: заголовок и строки "название — значение". */
    private VBox card(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("section-title");
        VBox card = new VBox(label);
        card.getStyleClass().add("details-card");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        detailsBox.getChildren().add(card);
        return card;
    }

    private void row(VBox card, String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("details-key");
        keyLabel.setMinWidth(105);
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("details-value");
        valueLabel.setWrapText(true);
        HBox row = new HBox(8, keyLabel, valueLabel);
        card.getChildren().add(row);
    }

    // ------------------------------------------------------------------
    // Вспомогательные методы
    // ------------------------------------------------------------------

    private void reloadDentistFilter() {
        Dentist current = dentistFilter.getValue();
        List<Dentist> dentists = dentistService.findAll();
        Combos.withAll(dentistFilter, dentists, Dentist::getFullName, "Все врачи");
        if (current != null) {
            dentists.stream()
                    .filter(dentist -> dentist.getId() == current.getId())
                    .findFirst()
                    .ifPresent(dentistFilter::setValue);
        }
    }

    private void select(int appointmentId) {
        for (Appointment appointment : sorted) {
            if (appointment.getId() == appointmentId) {
                appointmentsTable.getSelectionModel().select(appointment);
                appointmentsTable.scrollTo(appointment);
                return;
            }
        }
        appointmentsTable.getSelectionModel().clearSelection();
        showDetails(null);
    }

    private void updateCount() {
        countLabel.setText("Показано записей: " + filtered.size() + " из " + appointments.size());
    }

    private Window window() {
        return appointmentsTable.getScene() == null ? null : appointmentsTable.getScene().getWindow();
    }

    /** Пункт списка "Сортировка": столбец таблицы и направление. */
    private record SortOption(String title, TableColumn<Appointment, ?> column, TableColumn.SortType sortType) {
    }
}
