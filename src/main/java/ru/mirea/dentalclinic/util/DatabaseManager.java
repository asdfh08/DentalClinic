package ru.mirea.dentalclinic.util;

import ru.mirea.dentalclinic.exception.DataAccessException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Единая точка получения JDBC-соединения с MySQL/PostgreSQL.
 *
 * Параметры читаются из src/main/resources/db.properties,
 * их можно переопределить переменными окружения DB_URL / DB_USER / DB_PASSWORD.
 */
public final class DatabaseManager {

    private static final String CONFIG_FILE = "db.properties";

    private static String url;
    private static String user;
    private static String password;
    private static String databaseProduct = "неизвестная СУБД";

    private DatabaseManager() {
    }

    /**
     * Читает настройки и сразу проверяет соединение.
     * Вызывается один раз при старте приложения.
     */
    public static void init() {
        Properties properties = loadProperties();

        url = value("DB_URL", properties.getProperty("db.url"));
        user = value("DB_USER", properties.getProperty("db.user"));
        password = value("DB_PASSWORD", properties.getProperty("db.password"));

        if (url == null || url.isBlank()) {
            throw new DataAccessException("В файле " + CONFIG_FILE + " не указан параметр db.url");
        }

        // try-with-resources: соединение закрывается автоматически
        try (Connection connection = getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            databaseProduct = metaData.getDatabaseProductName() + " " + metaData.getDatabaseProductVersion();
        } catch (SQLException e) {
            throw new DataAccessException("не удалось подключиться к базе данных (" + url + "): "
                    + e.getMessage(), e);
        }
    }

    /**
     * Новое соединение с базой данных.
     * Драйвер (MySQL или PostgreSQL) выбирается автоматически по строке подключения.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    public static String connectionInfo() {
        return databaseProduct + " | " + url;
    }

    private static Properties loadProperties() {
        Properties properties = new Properties();
        try (InputStream in = DatabaseManager.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new DataAccessException("файл настроек " + CONFIG_FILE
                        + " не найден в src/main/resources");
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                properties.load(reader);
            }
        } catch (IOException e) {
            throw new DataAccessException("не удалось прочитать файл настроек " + CONFIG_FILE, e);
        }
        return properties;
    }

    private static String value(String environmentKey, String fallback) {
        String fromEnvironment = System.getenv(environmentKey);
        return (fromEnvironment == null || fromEnvironment.isBlank()) ? fallback : fromEnvironment;
    }
}
