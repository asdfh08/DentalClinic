package ru.mirea.dentalclinic.repository;

import ru.mirea.dentalclinic.model.Appointment;
import ru.mirea.dentalclinic.model.AppointmentStatus;
import ru.mirea.dentalclinic.model.ProcedureType;
import ru.mirea.dentalclinic.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Слой Repository для основной сущности — таблицы appointments.
 *
 * Все запросы параметризованные (PreparedStatement), соединения закрываются
 * через try-with-resources. ФИО пациента и врача подтягиваются через JOIN.
 */
public class AppointmentRepository extends AbstractRepository implements CrudRepository<Appointment> {

    /** Базовый SELECT со связями к таблицам patients и dentists. */
    private static final String SELECT_BASE = """
            SELECT a.id, a.patient_id, a.dentist_id, a.appointment_time, a.procedure_type,
                   a.status, a.price, a.complaint, a.created_at,
                   p.full_name AS patient_name, d.full_name AS dentist_name
            FROM appointments a
                     JOIN patients p ON p.id = a.patient_id
                     JOIN dentists d ON d.id = a.dentist_id
            """;

    @Override
    protected String tableName() {
        return "appointments";
    }

    @Override
    public int save(Appointment appointment) {
        String sql = """
                INSERT INTO appointments (patient_id, dentist_id, appointment_time, procedure_type,
                                          status, price, complaint)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, appointment.getPatientId());
            statement.setInt(2, appointment.getDentistId());
            statement.setTimestamp(3, Timestamp.valueOf(appointment.getAppointmentTime()));
            statement.setString(4, appointment.getProcedureType().name());
            statement.setString(5, appointment.getStatus().name());
            statement.setBigDecimal(6, appointment.getPrice());
            statement.setString(7, appointment.getComplaint().isBlank() ? null : appointment.getComplaint());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    appointment.setId(keys.getInt(1));
                }
            }
            return appointment.getId();
        } catch (SQLException e) {
            throw translate(e, "сохранить запись на приём");
        }
    }

    @Override
    public Optional<Appointment> findById(int id) {
        String sql = SELECT_BASE + " WHERE a.id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw translate(e, "получить запись на приём по ID");
        }
    }

    @Override
    public List<Appointment> findAll() {
        String sql = SELECT_BASE + " ORDER BY a.appointment_time";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            return readAll(statement);
        } catch (SQLException e) {
            throw translate(e, "получить список записей на приём");
        }
    }

    @Override
    public boolean update(Appointment appointment) {
        String sql = """
                UPDATE appointments
                SET patient_id = ?, dentist_id = ?, appointment_time = ?, procedure_type = ?,
                    status = ?, price = ?, complaint = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, appointment.getPatientId());
            statement.setInt(2, appointment.getDentistId());
            statement.setTimestamp(3, Timestamp.valueOf(appointment.getAppointmentTime()));
            statement.setString(4, appointment.getProcedureType().name());
            statement.setString(5, appointment.getStatus().name());
            statement.setBigDecimal(6, appointment.getPrice());
            statement.setString(7, appointment.getComplaint().isBlank() ? null : appointment.getComplaint());
            statement.setInt(8, appointment.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translate(e, "обновить запись на приём");
        }
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM appointments WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translate(e, "удалить запись на приём");
        }
    }

    // ------------------------------------------------------------------
    // Поиск и выборки, выполняемые средствами SQL
    // ------------------------------------------------------------------

    /** Поиск записей по части ФИО пациента. */
    public List<Appointment> findByPatientNameLike(String part) {
        String sql = SELECT_BASE + " WHERE LOWER(p.full_name) LIKE ? ORDER BY a.appointment_time";
        return queryWithSinglePattern(sql, "%" + part.toLowerCase() + "%", "найти записи по ФИО пациента");
    }

    /** Поиск записей по части телефона пациента. */
    public List<Appointment> findByPatientPhoneLike(String part) {
        String sql = SELECT_BASE + " WHERE p.phone LIKE ? ORDER BY a.appointment_time";
        return queryWithSinglePattern(sql, "%" + part + "%", "найти записи по телефону пациента");
    }

    /** Поиск записей по тексту жалобы. */
    public List<Appointment> findByComplaintLike(String part) {
        String sql = SELECT_BASE + " WHERE LOWER(a.complaint) LIKE ? ORDER BY a.appointment_time";
        return queryWithSinglePattern(sql, "%" + part.toLowerCase() + "%", "найти записи по жалобе");
    }

    /** Записи в интервале времени [from; to). */
    public List<Appointment> findByPeriod(LocalDateTime from, LocalDateTime to) {
        String sql = SELECT_BASE + " WHERE a.appointment_time >= ? AND a.appointment_time < ?"
                + " ORDER BY a.appointment_time";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setTimestamp(1, Timestamp.valueOf(from));
            statement.setTimestamp(2, Timestamp.valueOf(to));
            return readAll(statement);
        } catch (SQLException e) {
            throw translate(e, "получить записи за период");
        }
    }

    /** Записи конкретного врача. */
    public List<Appointment> findByDentistId(int dentistId) {
        String sql = SELECT_BASE + " WHERE a.dentist_id = ? ORDER BY a.appointment_time";
        return queryWithSingleInt(sql, dentistId, "получить записи врача");
    }

    /** Записи конкретного пациента. */
    public List<Appointment> findByPatientId(int patientId) {
        String sql = SELECT_BASE + " WHERE a.patient_id = ? ORDER BY a.appointment_time";
        return queryWithSingleInt(sql, patientId, "получить записи пациента");
    }

    /** Фильтр по статусу средствами SQL. */
    public List<Appointment> findByStatus(AppointmentStatus status) {
        String sql = SELECT_BASE + " WHERE a.status = ? ORDER BY a.appointment_time";
        return queryWithSinglePattern(sql, status.name(), "отфильтровать записи по статусу");
    }

    /** Записи врача в интервале времени — нужны для проверки занятости слота. */
    public List<Appointment> findByDentistAndPeriod(int dentistId, LocalDateTime from, LocalDateTime to) {
        String sql = SELECT_BASE + " WHERE a.dentist_id = ? AND a.appointment_time >= ?"
                + " AND a.appointment_time < ? ORDER BY a.appointment_time";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, dentistId);
            statement.setTimestamp(2, Timestamp.valueOf(from));
            statement.setTimestamp(3, Timestamp.valueOf(to));
            return readAll(statement);
        } catch (SQLException e) {
            throw translate(e, "проверить расписание врача");
        }
    }

    /** Записи пациента в интервале времени — нужны для проверки двойной записи. */
    public List<Appointment> findByPatientAndPeriod(int patientId, LocalDateTime from, LocalDateTime to) {
        String sql = SELECT_BASE + " WHERE a.patient_id = ? AND a.appointment_time >= ?"
                + " AND a.appointment_time < ? ORDER BY a.appointment_time";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientId);
            statement.setTimestamp(2, Timestamp.valueOf(from));
            statement.setTimestamp(3, Timestamp.valueOf(to));
            return readAll(statement);
        } catch (SQLException e) {
            throw translate(e, "проверить записи пациента");
        }
    }

    /** Количество активных (незавершённых) записей пациента. */
    public long countActiveByPatientId(int patientId) {
        String sql = "SELECT COUNT(*) FROM appointments WHERE patient_id = ? AND status IN ('CREATED', 'CONFIRMED')";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, patientId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            throw translate(e, "подсчитать активные записи пациента");
        }
    }

    // ------------------------------------------------------------------
    // Вспомогательные методы
    // ------------------------------------------------------------------

    private List<Appointment> queryWithSinglePattern(String sql, String parameter, String action) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, parameter);
            return readAll(statement);
        } catch (SQLException e) {
            throw translate(e, action);
        }
    }

    private List<Appointment> queryWithSingleInt(String sql, int parameter, String action) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, parameter);
            return readAll(statement);
        } catch (SQLException e) {
            throw translate(e, action);
        }
    }

    private List<Appointment> readAll(PreparedStatement statement) throws SQLException {
        List<Appointment> appointments = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                appointments.add(map(resultSet));
            }
        }
        return appointments;
    }

    /** ResultSet -> объект Appointment. */
    private Appointment map(ResultSet resultSet) throws SQLException {
        Timestamp appointmentTime = resultSet.getTimestamp("appointment_time");
        Timestamp createdAt = resultSet.getTimestamp("created_at");

        return new Appointment(
                resultSet.getInt("id"),
                resultSet.getInt("patient_id"),
                resultSet.getInt("dentist_id"),
                resultSet.getString("patient_name"),
                resultSet.getString("dentist_name"),
                appointmentTime == null ? null : appointmentTime.toLocalDateTime(),
                ProcedureType.parse(resultSet.getString("procedure_type")),
                AppointmentStatus.parse(resultSet.getString("status")),
                resultSet.getBigDecimal("price"),
                resultSet.getString("complaint"),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }
}
