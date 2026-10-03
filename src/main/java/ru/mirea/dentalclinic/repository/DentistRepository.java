package ru.mirea.dentalclinic.repository;

import ru.mirea.dentalclinic.model.Dentist;
import ru.mirea.dentalclinic.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Слой Repository для таблицы dentists (врачи-стоматологи).
 */
public class DentistRepository extends AbstractRepository implements CrudRepository<Dentist> {

    private static final String COLUMNS = "id, full_name, phone, specialization, cabinet";

    @Override
    protected String tableName() {
        return "dentists";
    }

    @Override
    public int save(Dentist dentist) {
        String sql = "INSERT INTO dentists (full_name, phone, specialization, cabinet) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, dentist.getFullName());
            statement.setString(2, dentist.getPhone());
            statement.setString(3, dentist.getSpecialization());
            statement.setInt(4, dentist.getCabinet());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    dentist.setId(keys.getInt(1));
                }
            }
            return dentist.getId();
        } catch (SQLException e) {
            throw translate(e, "сохранить врача");
        }
    }

    @Override
    public Optional<Dentist> findById(int id) {
        String sql = "SELECT " + COLUMNS + " FROM dentists WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? Optional.of(map(resultSet)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw translate(e, "получить врача по ID");
        }
    }

    @Override
    public List<Dentist> findAll() {
        String sql = "SELECT " + COLUMNS + " FROM dentists ORDER BY full_name";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            List<Dentist> dentists = new ArrayList<>();
            while (resultSet.next()) {
                dentists.add(map(resultSet));
            }
            return dentists;
        } catch (SQLException e) {
            throw translate(e, "получить список врачей");
        }
    }

    @Override
    public boolean update(Dentist dentist) {
        String sql = "UPDATE dentists SET full_name = ?, phone = ?, specialization = ?, cabinet = ? WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, dentist.getFullName());
            statement.setString(2, dentist.getPhone());
            statement.setString(3, dentist.getSpecialization());
            statement.setInt(4, dentist.getCabinet());
            statement.setInt(5, dentist.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translate(e, "обновить врача");
        }
    }

    @Override
    public boolean deleteById(int id) {
        String sql = "DELETE FROM dentists WHERE id = ?";

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException e) {
            throw translate(e, "удалить врача");
        }
    }

    private Dentist map(ResultSet resultSet) throws SQLException {
        return new Dentist(
                resultSet.getInt("id"),
                resultSet.getString("full_name"),
                resultSet.getString("phone"),
                resultSet.getString("specialization"),
                resultSet.getInt("cabinet")
        );
    }
}
