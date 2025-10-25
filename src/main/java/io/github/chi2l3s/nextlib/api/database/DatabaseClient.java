package io.github.chi2l3s.nextlib.api.database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class DatabaseClient {
    private final SqlSupplier<Connection> connectionSupplier;

    DatabaseClient(SqlSupplier<Connection> connectionSupplier) {
        this.connectionSupplier = connectionSupplier;
    }

    public Connection openConnection() {
        try {
            return connectionSupplier.get();
        } catch (SQLException exception) {
            throw new DatabaseException("Failed to open database connection", exception);
        }
    }

    public <T> T withConnection(SqlFunction<Connection, T> callback) {
        try (Connection connection = openConnection()) {
            return callback.apply(connection);
        } catch (SQLException exception) {
            throw new DatabaseException("Failed to execute database callback", exception);
        }
    }

    public <T> List<T> query(String sql, SqlConsumer<PreparedStatement> binder, SqlFunction<ResultSet, T> mapper) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.accept(statement);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                List<T> results = new ArrayList<>();
                while (resultSet.next()) {
                    results.add(mapper.apply(resultSet));
                }
                return results;
            }
        } catch (SQLException exception) {
            throw new DatabaseException("Failed to execute query: " + sql, exception);
        }
    }

    public <T> Optional<T> queryOne(String sql, SqlConsumer<PreparedStatement> binder, SqlFunction<ResultSet, T> mapper) {
        List<T> results = query(sql, binder, mapper);
        if (results.isEmpty()) {
            return Optional.empty();
        }
        if (results.size() > 1) {
            throw new DatabaseException("Expected a single result for query but received " + results.size());
        }
        return Optional.of(results.get(0));
    }

    public int execute(String sql, SqlConsumer<PreparedStatement> binder) {
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (binder != null) {
                binder.accept(statement);
            }
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new DatabaseException("Failed to execute statement: " + sql, exception);
        }
    }

    public int[] executeBatch(String sql, List<SqlConsumer<PreparedStatement>> binders) {
        if (binders == null || binders.isEmpty()) {
            return new int[0];
        }
        try (Connection connection = openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (SqlConsumer<PreparedStatement> binder : binders) {
                binder.accept(statement);
                statement.addBatch();
            }
            return statement.executeBatch();
        } catch (SQLException exception) {
            throw new DatabaseException("Failed to execute batch: " + sql, exception);
        }
    }

}
