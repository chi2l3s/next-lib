package io.github.chi2l3s.nextlib.api.database;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Executes SQL scripts that consist of {@code ;}-terminated statements using an existing
 * {@link DatabaseClient}. The script is expected to be encoded in UTF-8.
 */
public final class SqlScriptRunner {
    private SqlScriptRunner() {
    }

    /**
     * Executes the statements contained inside the provided reader.
     *
     * @param client database client that supplies JDBC connections
     * @param scriptReader reader for the SQL script; the reader will be closed when execution finishes
     */
    public static void run(DatabaseClient client, Reader scriptReader) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(scriptReader, "scriptReader");
        client.withConnection(connection -> {
            executeScript(connection, scriptReader);
            return null;
        });
    }

    private static void executeScript(Connection connection, Reader scriptReader) {
        try (BufferedReader reader = new BufferedReader(scriptReader)) {
            StringBuilder current = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                    continue;
                }
                current.append(line).append(System.lineSeparator());
                if (trimmed.endsWith(";")) {
                    runStatement(connection, current.toString());
                    current.setLength(0);
                }
            }
            if (current.length() > 0) {
                runStatement(connection, current.toString());
            }
        } catch (IOException exception) {
            throw new DatabaseException("Failed to read SQL script", exception);
        }
    }

    private static void runStatement(Connection connection, String sql) {
        String trimmed = sql.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        if (trimmed.endsWith(";")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        try (Statement statement = connection.createStatement()) {
            statement.execute(trimmed);
        } catch (SQLException exception) {
            throw new DatabaseException("Failed to execute SQL statement", exception);
        }
    }
}