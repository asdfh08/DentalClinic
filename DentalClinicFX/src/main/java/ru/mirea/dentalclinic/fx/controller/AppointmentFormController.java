package ru.mirea.dentalclinic.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.dentalclinic.exception.AppException;
import ru.mirea.dentalclinic.fx.Combos;
import ru.mirea.dentalclinic.fx.DatePickers;
import ru.mirea.dentalclinic.fx.Dialogs;
import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.AppointmentStatus;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.service.AppointmentService;
import ru.mirea.dentalclinic.service.DentistService;
import ru.mirea.dentalclinic.service.PatientService;
import ru.mirea.dentalclinic.util.Formats;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Контроллер диалогового окна записи на приём (appointment-form.fxml).
 *
 * Один и тот же FXML используется для добавления и для редактирования.
 * Порядок сохранения:
 *  1) проверка ввода в форме: обязательные поля, формат даты/времени/суммы, длина текста;
 *  2) вызов AppointmentService, который проверяет бизнес-правила КР №1
 *     (прошедшее время, часы работы, занятость врача, переходы статусов и т.д.);
 *  3) ошибка показывается в окне, и форма не закрывается, чтобы пользователь мог исправить данные.
 */
public class AppointmentFormController {

    private static final Logger LOG = LoggerFactory.getLogger(AppointmentFormController.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");
    private static final String ERROR_STYLE = "field-error";

    private final AppointmentService appointmentService;
    private final PatientService patientService;
    private final DentistService dentistService;

    /** null — добавление новой записи, иначе — редактируемая запись. */
    private Appointment editing;
    private int savedId;

    @FXML
    private Label titleLabel;
    @FXML
    private ComboBox<Patient> patientBox;
    @FXML
    private ComboBox<Dentist> dentistBox;
    @FXML
    private DatePicker datePicker;
    @FXML
    private ComboBox<String> timeBox;
    @FXML
    private ComboBox<ProcedureType> procedureBox;
    @FXML
    private TextField priceField;
    @FXML
    private Label statusLabel;
    @FXML
    private ComboBox<AppointmentStatus> statusBox;
    @FXML
    private TextArea complaintArea;
    @FXML
    private Label complaintCounter;
    @FXML
    private Label hintLabel;
    @FXML
    private Label errorLabel;
    @FXML
    private Button saveButton;

    public AppointmentFormController(AppointmentService appointmentService,
                                     PatientService patientService,
                                     DentistService dentistService) {
        this.appointmentService = appointmentService;
        this.patientService = patientService;
        this.dentistService = dentistService;
    }

    @FXML
    private void initialize() {
        Combos.display(patientBox, Combos::patient);
        Combos.display(dentistBox, Combos::dentist);
        Combos.display(procedureBox, Combos::procedure);
        Combos.display(statusBox, AppointmentStatus::getTitle);
        procedureBox.getItems().setAll(ProcedureType.values());

        // Время начала: готовые варианты каждые 15 минут, но можно ввести своё
        for (LocalTime time = LocalTime.of(9, 0); time.isBefore(LocalTime.of(20, 0)); time = time.plusMinutes(15)) {
            timeBox.getItems().add(time.format(Formats.TIME));
        }

        DatePickers.useDayMonthYear(datePicker);

        // При выборе процедуры подставляется цена по прайсу (её можно изменить)
        procedureBox.valueProperty().addListener((obs, oldValue, procedure) -> {
            if (procedure != null) {
                priceField.setText(procedure.getBasePrice().toPlainString());
                hintLabel.setText("Длительность процедуры: " + procedure.getDurationMinutes()
                        + " мин, цена по прайсу: " + Formats.money(procedure.getBasePrice()));
            }
        });

        complaintArea.textProperty().addListener((obs, oldValue, text) -> updateComplaintCounter());
        updateComplaintCounter();

        // Подсветка ошибки снимается, как только пользователь начинает исправлять поле
        for (Node field : fields()) {
            field.focusedProperty().addListener((obs, wasFocused, focused) -> {
                if (focused) {
                    field.getStyleClass().remove(ERROR_STYLE);
                }
            });
        }
    }

    // ------------------------------------------------------------------
    // Открытие формы
    // ------------------------------------------------------------------

    public void openForCreate() {
        editing = null;
        titleLabel.setText("Новая запись на приём");
        patientBox.getItems().setAll(patientService.findAll());
        dentistBox.getItems().setAll(dentistService.findAll());
        datePicker.setValue(LocalDate.now().plusDays(1));
        setStatusVisible(false);
        hintLabel.setText("Выберите пациента, врача, дату, время и процедуру. "
                + "Стоимость можно не указывать — будет взята цена по прайсу.");
    }

    public void openForEdit(Appointment appointment) {
        editing = appointment;
        titleLabel.setText("Запись №" + appointment.getId());

        // Пациент и врач показываются, но в существующей записи не меняются
        patientBox.getItems().setAll(patientService.getById(appointment.getPatientId()));
        patientBox.getSelectionModel().selectFirst();
        dentistBox.getItems().setAll(dentistService.getById(appointment.getDentistId()));
        dentistBox.getSelectionModel().selectFirst();
        patientBox.setDisable(true);
        dentistBox.setDisable(true);

        datePicker.setValue(appointment.getAppointmentTime().toLocalDate());
        timeBox.getEditor().setText(Formats.time(appointment.getAppointmentTime()));
        procedureBox.setValue(appointment.getProcedureType());
        priceField.setText(appointment.getPrice().toPlainString());
        complaintArea.setText(appointment.getComplaint());

        // В списке статусов — текущий и разрешённые переходы (БП-6)
        AppointmentStatus current = appointment.getStatus();
        List<AppointmentStatus> statuses = new ArrayList<>();
        statuses.add(current);
        statuses.addAll(current.allowedTransitions());
        statusBox.getItems().setAll(statuses);
        statusBox.setValue(current);
        setStatusVisible(true);

        if (current.isFinal()) {
            for (Node field : fields()) {
                field.setDisable(true);
            }
            saveButton.setDisable(true);
            hintLabel.setText("Статус «" + current.getTitle() + "» финальный: такую запись изменить нельзя. "
                    + "Форма открыта только для просмотра.");
        } else {
            hintLabel.setText("Пациента и врача в существующей записи изменить нельзя — для этого создайте "
                    + "новую запись. В списке статусов — только разрешённые переходы из статуса «"
                    + current.getTitle() + "».");
        }
    }

    /** ID сохранённой записи; 0 — пользователь закрыл окно без сохранения. */
    public int getSavedId() {
        return savedId;
    }

    // ------------------------------------------------------------------
    // Сохранение
    // ------------------------------------------------------------------

    @FXML
    private void onSave() {
        clearErrors();
        List<String> errors = new ArrayList<>();

        Patient patient = patientBox.getValue();
        if (patient == null) {
            fieldError(errors, patientBox, "Пациент", "выберите пациента из списка");
        }
        Dentist dentist = dentistBox.getValue();
        if (dentist == null) {
            fieldError(errors, dentistBox, "Врач", "выберите врача из списка");
        }
        LocalDate date = readDate(errors);
        LocalTime time = readTime(errors);
        ProcedureType procedure = procedureBox.getValue();
        if (procedure == null) {
            fieldError(errors, procedureBox, "Процедура", "выберите процедуру из списка");
        }
        BigDecimal price = readPrice(errors);
        String complaint = complaintArea.getText();
        if (complaint != null && complaint.trim().length() > AppointmentService.MAX_COMPLAINT_LENGTH) {
            fieldError(errors, complaintArea, "Жалоба", "сократите текст до "
                    + AppointmentService.MAX_COMPLAINT_LENGTH + " символов (сейчас " + complaint.trim().length() + ")");
        }
        if (editing != null && statusBox.getValue() == null) {
            fieldError(errors, statusBox, "Статус", "выберите статус из списка");
        }

        if (!errors.isEmpty()) {
            showError("Исправьте ошибки в форме:\n• " + String.join("\n• ", errors));
            return;
        }

        LocalDateTime dateTime = LocalDateTime.of(date, time);
        try {
            Appointment saved = editing == null
                    ? appointmentService.create(patient.getId(), dentist.getId(), dateTime, procedure, price, complaint)
                    : appointmentService.update(editing.getId(), dateTime, procedure, price, complaint,
                    statusBox.getValue());
            savedId = saved.getId();
            close();
        } catch (AppException e) {
            // Нарушено бизнес-правило (врач занят, нерабочее время и т.д.) — форма остаётся открытой
            showError(Dialogs.sentence(e.getMessage()));
        } catch (RuntimeException e) {
            LOG.error("Ошибка при сохранении записи на приём", e);
            showError("Непредвиденная ошибка при сохранении. Подробности записаны в журнал приложения.");
        }
    }

    @FXML
    private void onCancel() {
        close();
    }

    // ------------------------------------------------------------------
    // Чтение и проверка полей
    // ------------------------------------------------------------------

    private LocalDate readDate(List<String> errors) {
        String text = datePicker.getEditor().getText();
        if (text == null || text.isBlank()) {
            fieldError(errors, datePicker, "Дата приёма", "выберите дату в календаре");
            return null;
        }
        LocalDate date = DatePickers.parseOrNull(text);
        if (date == null) {
            fieldError(errors, datePicker, "Дата приёма",
                    "дата «" + text.trim() + "» указана неверно, нужен формат ДД.ММ.ГГГГ, например 15.12.2026");
        }
        return date;
    }

    private LocalTime readTime(List<String> errors) {
        String text = timeBox.getEditor().getText();
        if (text == null || text.isBlank()) {
            fieldError(errors, timeBox, "Время начала", "выберите время из списка или введите его");
            return null;
        }
        try {
            return LocalTime.parse(text.trim(), TIME_FORMAT);
        } catch (DateTimeParseException e) {
            fieldError(errors, timeBox, "Время начала",
                    "время «" + text.trim() + "» указано неверно, нужен формат ЧЧ:ММ, например 10:30");
            return null;
        }
    }

    /** Пустое поле — цена по прайсу (null), иначе число от 0 до максимальной стоимости. */
    private BigDecimal readPrice(List<String> errors) {
        String text = priceField.getText();
        if (text == null || text.isBlank()) {
            return null;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(text.trim().replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException e) {
            fieldError(errors, priceField, "Стоимость", "введите число, например 1500 или 1500,50");
            return null;
        }
        if (price.scale() > 2) {
            fieldError(errors, priceField, "Стоимость", "укажите не более двух знаков после запятой");
        } else if (price.signum() < 0) {
            fieldError(errors, priceField, "Стоимость", "стоимость не может быть отрицательной");
        } else if (price.compareTo(AppointmentService.MAX_PRICE) > 0) {
            fieldError(errors, priceField, "Стоимость", "стоимость не может быть больше "
                    + Formats.money(AppointmentService.MAX_PRICE));
        }
        return price;
    }

    // ------------------------------------------------------------------
    // Отображение ошибок
    // ------------------------------------------------------------------

    private void fieldError(List<String> errors, Node field, String fieldName, String hint) {
        errors.add("Поле «" + fieldName + "»: " + hint + ".");
        if (!field.getStyleClass().contains(ERROR_STYLE)) {
            field.getStyleClass().add(ERROR_STYLE);
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        errorLabel.getScene().getWindow().sizeToScene();
    }

    private void clearErrors() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        for (Node field : fields()) {
            field.getStyleClass().remove(ERROR_STYLE);
        }
    }

    private void updateComplaintCounter() {
        int length = complaintArea.getText() == null ? 0 : complaintArea.getText().trim().length();
        complaintCounter.setText(length + " / " + AppointmentService.MAX_COMPLAINT_LENGTH + " символов");
        complaintCounter.getStyleClass().remove("hint-warning");
        if (length > AppointmentService.MAX_COMPLAINT_LENGTH) {
            complaintCounter.getStyleClass().add("hint-warning");
        }
    }

    private void setStatusVisible(boolean visible) {
        statusLabel.setVisible(visible);
        statusLabel.setManaged(visible);
        statusBox.setVisible(visible);
        statusBox.setManaged(visible);
    }

    private List<Node> fields() {
        return List.of(patientBox, dentistBox, datePicker, timeBox, procedureBox, priceField, statusBox, complaintArea);
    }

    private void close() {
        ((Stage) saveButton.getScene().getWindow()).close();
    }
}
