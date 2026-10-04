package ru.mirea.dentalclinic.repository;

import ru.mirea.dentalclinic.exception.AppException;
import ru.mirea.dentalclinic.exception.BusinessException;
import ru.mirea.dentalclinic.exception.DataAccessException;
import ru.mirea.dentalclinic.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Общая часть всех репозиториев: подсчёт строк и единая обработка SQLException.
 * Абстрактный класс + интерфейс {@link CrudRepository} = переиспользование кода
 * без дублирования try-with-resources в каждом методе.
 */
public abstract class AbstractRepository {

    /** Имя таблицы конкретного репозитория. */
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
     * Превращает техническое SQLException в понятное исключение приложения.
     * SQL-состояния класса 23 — это нарушения ограничений целостности
     * (UNIQUE, NOT NULL, FOREIGN KEY, CHECK), то есть по сути бизнес-ошибка.
     */
    protected AppException translate(SQLException e, String action) {
        String state = e.getSQLState();
        if (state != null && state.startsWith("23")) {
            return new BusinessException("база данных отклонила операцию (" + action
                    + "): нарушено ограничение целостности. " + e.getMessage());
        }
        return new DataAccessException("ошибка базы данных при попытке " + action
                + ": " + e.getMessage(), e);
    }
}
