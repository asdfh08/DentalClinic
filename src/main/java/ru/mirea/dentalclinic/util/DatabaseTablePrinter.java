package ru.mirea.dentalclinic.util;

import ru.mirea.dentalclinic.exception.DataAccessException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Пункт меню "Вывести таблицы базы данных".
 *
 * Список таблиц берётся из метаданных JDBC ({@link DatabaseMetaData}),
 * содержимое читается через ResultSet + ResultSetMetaData, поэтому класс
 * работает одинаково и с MySQL, и с PostgreSQL.
 */
public class DatabaseTablePrinter {

    private static final int MAX_ROWS = 20;

    /** Возвращает готовый к выводу текст: структура и первые строки каждой таблицы. */
    public String describeAllTables() {
        StringBuilder report = new StringBuilder();

        try (Connection connection = DatabaseManager.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            List<String> tables = readTableNames(connection, metaData);

            report.append("СУБД: ").append(metaData.getDatabaseProductName())
                    .append(' ').append(metaData.getDatabaseProductVersion())
                    .append(System.lineSeparator())
                    .append("Таблиц найдено: ").append(tables.size())
                    .append(System.lineSeparator());

            for (String table : tables) {
                report.append(System.lineSeparator())
                        .append("=== ТАБЛИЦА ").append(table.toUpperCase()).append(" ===")
                        .append(System.lineSeparator());
                report.append(dumpTable(connection, table));
            }
            return report.toString();
        } catch (SQLException e) {
            throw new DataAccessException("не удалось прочитать метаданные базы данных: "
                    + e.getMessage(), e);
        }
    }

    private List<String> readTableNames(Connection connection, DatabaseMetaData metaData) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet resultSet = metaData.getTables(connection.getCatalog(), connection.getSchema(),
                "%", new String[]{"TABLE"})) {

            while (resultSet.next()) {
                String name = resultSet.getString("TABLE_NAME");
                // системные таблицы PostgreSQL отфильтровываем
                if (name != null && name.matches("[A-Za-z0-9_]+") && !name.startsWith("pg_")) {
                    tables.add(name);
                }
            }
        }
        return tables;
    }

    /**
     * Читает первые {@value #MAX_ROWS} строк таблицы.
     * Имя таблицы подставляется в запрос, но получено из метаданных базы данных
     * и дополнительно проверено регулярным выражением — пользовательский ввод сюда не попадает.
     */
    private String dumpTable(Connection connection, String table) throws SQLException {
        String sql = "SELECT * FROM " + table;
        StringBuilder result = new StringBuilder();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setMaxRows(MAX_ROWS);

            try (ResultSet resultSet = statement.executeQuery()) {
                ResultSetMetaData meta = resultSet.getMetaData();
                int columnCount = meta.getColumnCount();

                StringBuilder header = new StringBuilder();
                for (int i = 1; i <= columnCount; i++) {
                    header.append(String.format("%-22s", Formats.cut(meta.getColumnLabel(i), 21)));
                }
                result.append(header).append(System.lineSeparator());
                result.append("-".repeat(Math.min(header.length(), 140))).append(System.lineSeparator());

                int rows = 0;
                while (resultSet.next()) {
                    StringBuilder line = new StringBuilder();
                    for (int i = 1; i <= columnCount; i++) {
                        Object value = resultSet.getObject(i);
                        line.append(String.format("%-22s",
                                Formats.cut(value == null ? "NULL" : String.valueOf(value), 21)));
                    }
                    result.append(line).append(System.lineSeparator());
                    rows++;
                }
                result.append("Строк выведено: ").append(rows)
                        .append(rows == MAX_ROWS ? " (показаны первые " + MAX_ROWS + ")" : "")
                        .append(System.lineSeparator());
            }
        }
        return result.toString();
    }
}
