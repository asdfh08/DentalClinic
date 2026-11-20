package ru.mirea.dentalclinic.fx.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Window;
import ru.mirea.dentalclinic.fx.Dialogs;
import ru.mirea.dentalclinic.fx.TableColumns;
import ru.mirea.dentalclinic.model.AgeCategory;
import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.AppointmentStatus;
import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.service.AppointmentService;
import ru.mirea.dentalclinic.service.PatientService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Контроллер вкладки "Пациенты" (patients.fxml): просмотр связанной сущности
 * и всех записей на приём выбранного пациента (связь appointments.patient_id -> patients.id).
 */
public class PatientsController {

    private final PatientService patientService;
    private final AppointmentService appointmentService;

    private final ObservableList<Patient> patients = FXCollections.observableArrayList();
    private final ObservableList<Appointment> patientAppointments = FXCollections.observableArrayList();

    @FXML
    private Label patientsCountLabel;
    @FXML
    private Label patientAppointmentsTitle;
    @FXML
    private Label patientAppointmentsPlaceholder;
    @FXML
    private TableView<Patient> patientsTable;
    @FXML
    private TableColumn<Patient, String> nameColumn;
    @FXML
    private TableColumn<Patient, String> phoneColumn;
    @FXML
    private TableColumn<Patient, String> emailColumn;
    @FXML
    private TableColumn<Patient, LocalDate> birthDateColumn;
    @FXML
    private TableColumn<Patient, Integer> ageColumn;
    @FXML
    private TableColumn<Patient, AgeCategory> categoryColumn;
    @FXML
    private TableView<Appointment> patientAppointmentsTable;
    @FXML
    private TableColumn<Appointment, LocalDateTime> timeColumn;
    @FXML
    private TableColumn<Appointment, String> dentistColumn;
    @FXML
    private TableColumn<Appointment, ProcedureType> procedureColumn;
    @FXML
    private TableColumn<Appointment, AppointmentStatus> statusColumn;
    @FXML
    private TableColumn<Appointment, BigDecimal> priceColumn;

    public PatientsController(PatientService patientService, AppointmentService appointmentService) {
        this.patientService = patientService;
        this.appointmentService = appointmentService;
    }

    @FXML
    private void initialize() {
        TableColumns.value(nameColumn, Patient::getFullName);
        TableColumns.value(phoneColumn, Patient::getPhone);
        TableColumns.value(emailColumn, patient -> patient.getEmail() == null ? "" : patient.getEmail());
        TableColumns.date(birthDateColumn, Patient::getBirthDate);
        TableColumns.value(ageColumn, Patient::getAge);
        TableColumns.titled(categoryColumn, Patient::getAgeCategory, AgeCategory::getTitle);
        patientsTable.setItems(patients);
        patientsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumns.dateTime(timeColumn, Appointment::getAppointmentTime);
        TableColumns.value(dentistColumn, Appointment::getDentistName);
        TableColumns.titled(procedureColumn, Appointment::getProcedureType, ProcedureType::getTitle);
        TableColumns.status(statusColumn, Appointment::getStatus);
        TableColumns.money(priceColumn, Appointment::getPrice);
        patientAppointmentsTable.setItems(patientAppointments);
        patientAppointmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        patientsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, patient) -> showAppointments(patient));

        refresh();
    }

    public void refresh() {
        Dialogs.run(window(), "Не удалось загрузить пациентов", () -> {
            Patient selected = patientsTable.getSelectionModel().getSelectedItem();
            patients.setAll(patientService.findAll());
            patientsCountLabel.setText("Пациентов: " + patients.size());
            if (selected != null) {
                patients.stream()
                        .filter(patient -> patient.getId() == selected.getId())
                        .findFirst()
                        .ifPresent(patient -> patientsTable.getSelectionModel().select(patient));
            }
            showAppointments(patientsTable.getSelectionModel().getSelectedItem());
        });
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void showAppointments(Patient patient) {
        if (patient == null) {
            patientAppointmentsTitle.setText("Записи пациента на приём");
            patientAppointmentsPlaceholder.setText("Выберите пациента в таблице выше");
            patientAppointments.clear();
            return;
        }
        Dialogs.run(window(), "Не удалось загрузить записи пациента", () -> {
            patientAppointments.setAll(appointmentService.findByPatient(patient.getId()));
            patientAppointmentsPlaceholder.setText("У пациента нет записей на приём");
            patientAppointmentsTitle.setText("Записи пациента: " + patient.getFullName()
                    + " (" + patientAppointments.size() + ")");
        });
    }

    private Window window() {
        return patientsTable.getScene() == null ? null : patientsTable.getScene().getWindow();
    }
}
