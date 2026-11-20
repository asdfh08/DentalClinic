package ru.mirea.dentalclinic.repository;

import ru.mirea.dentalclinic.model.Patient;
import ru.mirea.dentalclinic.util.DatabaseManager;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Слой Repository для таблицы patients.
 * Только SQL и преобразование ResultSet -> объект. Бизнес-логики здесь нет.
 */
public class PatientRepository extends AbstractRepository implements CrudRepository<Patient> {

    private static final String COLUMNS = "id, full_name, phone, email, birth_date, created_at";

    @Override
    protected String tableName() {
        return "patients";
    }

    @Override
    public int save(Patient patient) {
        String sql = "INSERT INTO patients (full_name, phone, email, birth_date) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, patient.getFullName());
            statement.setString(2, patient.getPhone());
            statement.setString(3, patient.getEmail());
            statement.setDate(4, Date.valueOf(patient.getBirthDate()));
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    patient.setId(keys.getInt(1));
                }
            }
            return patient.getId();
        } catch (SQLException e) {
            throw translate(e, "сохранить пациента");
        }
    }

    @Override
    public Optional<Patient> findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM patients WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw translate(e, "получить пациента по ID");
        }
    }

    @Override
    public List<Patient> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM patients ORDER BY full_name";
        return query(sql);
    }

    @Override
    public boolean update(Patient patient) {
        String sql = "UPDATE patients SET full_name = ?, phone = ?, email = ?, birth_date = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, patient.getFullName());
            statement.setString(2, patient.getPhone());
            statement.setString(3, patient.getEmail());
            statement.setDate(4, Date.valueOf(patient.getBirthDate()));
            statement.setInt(5, patient.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translate(e, "обновить пациента");
        }
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM patients WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translate(e, "удалить пациента");
        }
    }

    /** Поиск по части ФИО (параметризованный LIKE, без склейки строк). */
    public List<Patient> findByNameLike(String part) {
        String sql = "SELECT " + COLUMNS + " FROM patients WHERE LOWER(full_name) LIKE ? ORDER BY full_name";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "%" + part.toLowerCase() + "%");
            return readAll(statement);
        } catch (SQLException e) {
            throw translate(e, "найти пациентов по ФИО");
        }
    }

    /** Поиск по части номера телефона. */
    public List<Patient> findByPhoneLike(String part) {
        String sql = "SELECT " + COLUMNS + " FROM patients WHERE phone LIKE ? ORDER BY full_name";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, "%" + part + "%");
            return readAll(statement);
        } catch (SQLException e) {
            throw translate(e, "найти пациентов по телефону");
        }
    }

    private List<Patient> query(String sql) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            return readAll(statement);
        } catch (SQLException e) {
            throw translate(e, "получить список пациентов");
        }
    }

    private List<Patient> readAll(PreparedStatement statement) throws SQLException {
        List<Patient> patients = new ArrayList<>();
        try (ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                patients.add(map(resultSet));
            }
        }
        return patients;
    }

    /** ResultSet -> объект предметной модели. */
    private Patient map(ResultSet resultSet) throws SQLException {
        Date birthDate = resultSet.getDate("birth_date");
        Timestamp createdAt = resultSet.getTimestamp("created_at");

        return new Patient(
                resultSet.getInt("id"),
                resultSet.getString("full_name"),
                resultSet.getString("phone"),
                resultSet.getString("email"),
                birthDate == null ? null : birthDate.toLocalDate(),
                createdAt == null ? null : createdAt.toLocalDateTime()
        );
    }
}
