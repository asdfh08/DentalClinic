package ru.mirea.dentalclinic.repository;

import ru.mirea.dentalclinic.exception.BusinessException;
import ru.mirea.dentalclinic.exception.DatabaseException;
import ru.mirea.dentalclinic.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Общая часть всех репозиториев: подсчёт строк и перевод SQLException
 * в понятные пользователю исключения приложения.
 */
public abstract class AbstractRepository {

    /** Имя таблицы, с которой работает репозиторий. */
    protected abstract String tableName();

    public long count() {
        String sql = "SELECT COUNT(*) FROM " + tableName();

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException e) {
            throw translate(e, "подсчитать записи таблицы " + tableName());
        }
    }

    /**
     * Превращает SQLException в исключение приложения.
     *
     * @param action что пытались сделать, в инфинитиве: "сохранить пациента"
     */
    protected RuntimeException translate(SQLException e, String action) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("23")) {
            return new BusinessException("не удалось " + action
                    + ": нарушено ограничение целостности данных (связанные записи или дубликат)");
        }
        return new DatabaseException("не удалось " + action + ": " + e.getMessage(), e);
    }
}
