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
import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.AppointmentStatus;
import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.model.Specialization;
import ru.mirea.dentalclinic.service.AppointmentService;
import ru.mirea.dentalclinic.service.DentistService;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Контроллер вкладки "Врачи" (dentists.fxml): просмотр связанной сущности
 * и всех записей на приём к выбранному врачу (связь appointments.dentist_id -> dentists.id).
 */
public class DentistsController {

    private final DentistService dentistService;
    private final AppointmentService appointmentService;

    private final ObservableList<Dentist> dentists = FXCollections.observableArrayList();
    private final ObservableList<Appointment> dentistAppointments = FXCollections.observableArrayList();

    @FXML
    private Label dentistsCountLabel;
    @FXML
    private Label dentistAppointmentsTitle;
    @FXML
    private Label dentistAppointmentsPlaceholder;
    @FXML
    private TableView<Dentist> dentistsTable;
    @FXML
    private TableColumn<Dentist, String> nameColumn;
    @FXML
    private TableColumn<Dentist, Specialization> specializationColumn;
    @FXML
    private TableColumn<Dentist, Integer> cabinetColumn;
    @FXML
    private TableColumn<Dentist, String> phoneColumn;
    @FXML
    private TableView<Appointment> dentistAppointmentsTable;
    @FXML
    private TableColumn<Appointment, LocalDateTime> timeColumn;
    @FXML
    private TableColumn<Appointment, String> patientColumn;
    @FXML
    private TableColumn<Appointment, ProcedureType> procedureColumn;
    @FXML
    private TableColumn<Appointment, AppointmentStatus> statusColumn;
    @FXML
    private TableColumn<Appointment, BigDecimal> priceColumn;

    public DentistsController(DentistService dentistService, AppointmentService appointmentService) {
        this.dentistService = dentistService;
        this.appointmentService = appointmentService;
    }

    @FXML
    private void initialize() {
        TableColumns.value(nameColumn, Dentist::getFullName);
        TableColumns.titled(specializationColumn, Dentist::getSpecialization, Specialization::getTitle);
        TableColumns.value(cabinetColumn, Dentist::getCabinet);
        TableColumns.value(phoneColumn, Dentist::getPhone);
        dentistsTable.setItems(dentists);
        dentistsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumns.dateTime(timeColumn, Appointment::getAppointmentTime);
        TableColumns.value(patientColumn, Appointment::getPatientName);
        TableColumns.titled(procedureColumn, Appointment::getProcedureType, ProcedureType::getTitle);
        TableColumns.status(statusColumn, Appointment::getStatus);
        TableColumns.money(priceColumn, Appointment::getPrice);
        dentistAppointmentsTable.setItems(dentistAppointments);
        dentistAppointmentsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        dentistsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, dentist) -> showAppointments(dentist));

        refresh();
    }

    public void refresh() {
        Dialogs.run(window(), "Не удалось загрузить врачей", () -> {
            Dentist selected = dentistsTable.getSelectionModel().getSelectedItem();
            dentists.setAll(dentistService.findAll());
            dentistsCountLabel.setText("Врачей: " + dentists.size());
            if (selected != null) {
                dentists.stream()
                        .filter(dentist -> dentist.getId() == selected.getId())
                        .findFirst()
                        .ifPresent(dentist -> dentistsTable.getSelectionModel().select(dentist));
            }
            showAppointments(dentistsTable.getSelectionModel().getSelectedItem());
        });
    }

    @FXML
    private void onRefresh() {
        refresh();
    }

    private void showAppointments(Dentist dentist) {
        if (dentist == null) {
            dentistAppointmentsTitle.setText("Записи к врачу");
            dentistAppointmentsPlaceholder.setText("Выберите врача в таблице выше");
            dentistAppointments.clear();
            return;
        }
        Dialogs.run(window(), "Не удалось загрузить записи к врачу", () -> {
            dentistAppointments.setAll(appointmentService.searchByDentist(dentist.getId()));
            dentistAppointmentsPlaceholder.setText("К врачу нет записей на приём");
            dentistAppointmentsTitle.setText("Записи к врачу: " + dentist.getFullName()
                    + " (" + dentistAppointments.size() + ")");
        });
    }

    private Window window() {
        return dentistsTable.getScene() == null ? null : dentistsTable.getScene().getWindow();
    }
}
