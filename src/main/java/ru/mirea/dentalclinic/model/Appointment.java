package ru.mirea.dentalclinic.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * ОСНОВНАЯ СУЩНОСТЬ ВАРИАНТА — запись пациента на стоматологический приём.
 * Таблица базы данных: appointments.
 *
 * Поля patientName и dentistName не хранятся в таблице appointments,
 * они заполняются при чтении через JOIN и нужны только для вывода в консоль.
 */
public class Appointment {

    private int id;
    private int patientId;
    private int dentistId;
    private String patientName;
    private String dentistName;
    private LocalDateTime appointmentTime;
    private ProcedureType procedureType;
    private AppointmentStatus status;
    private BigDecimal price;
    private String complaint;
    private LocalDateTime createdAt;

    /** Полный конструктор — используется при чтении строки из ResultSet. */
    public Appointment(int id, int patientId, int dentistId, String patientName, String dentistName,
                       LocalDateTime appointmentTime, ProcedureType procedureType, AppointmentStatus status,
                       BigDecimal price, String complaint, LocalDateTime createdAt) {
        this.id = id;
        this.patientId = patientId;
        this.dentistId = dentistId;
        this.patientName = patientName;
        this.dentistName = dentistName;
        this.appointmentTime = appointmentTime;
        this.procedureType = procedureType;
        this.status = status;
        this.price = price;
        this.complaint = complaint;
        this.createdAt = createdAt;
    }

    /** Конструктор для новой записи: ID выдаёт база данных, статус — CREATED. */
    public Appointment(int patientId, int dentistId, LocalDateTime appointmentTime,
                       ProcedureType procedureType, BigDecimal price, String complaint) {
        this(0, patientId, dentistId, null, null, appointmentTime, procedureType,
                AppointmentStatus.CREATED, price, complaint, null);
    }

    /** Время окончания приёма — зависит от длительности выбранной процедуры. */
    public LocalDateTime getEndTime() {
        return appointmentTime.plusMinutes(procedureType.getDurationMinutes());
    }

    /** Пересекается ли этот приём по времени с другим (для проверки занятости врача). */
    public boolean overlaps(Appointment other) {
        return appointmentTime.isBefore(other.getEndTime())
                && other.getAppointmentTime().isBefore(getEndTime());
    }

    public boolean isActive() {
        return status.isActive();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getPatientId() {
        return patientId;
    }

    public void setPatientId(int patientId) {
        this.patientId = patientId;
    }

    public int getDentistId() {
        return dentistId;
    }

    public void setDentistId(int dentistId) {
        this.dentistId = dentistId;
    }

    public String getPatientName() {
        return patientName == null ? "" : patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getDentistName() {
        return dentistName == null ? "" : dentistName;
    }

    public void setDentistName(String dentistName) {
        this.dentistName = dentistName;
    }

    public LocalDateTime getAppointmentTime() {
        return appointmentTime;
    }

    public void setAppointmentTime(LocalDateTime appointmentTime) {
        this.appointmentTime = appointmentTime;
    }

    public ProcedureType getProcedureType() {
        return procedureType;
    }

    public void setProcedureType(ProcedureType procedureType) {
        this.procedureType = procedureType;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public void setStatus(AppointmentStatus status) {
        this.status = status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getComplaint() {
        return complaint == null ? "" : complaint;
    }

    public void setComplaint(String complaint) {
        this.complaint = complaint;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "Запись #" + id + ": " + getPatientName() + " -> " + getDentistName()
                + ", " + procedureType.getTitle() + ", статус " + status.getTitle();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        return id == ((Appointment) other).id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
