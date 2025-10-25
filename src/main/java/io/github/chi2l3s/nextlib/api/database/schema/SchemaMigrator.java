package io.github.chi2l3s.nextlib.api.database.schema;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;
import io.github.chi2l3s.nextlib.api.database.DatabaseException;
import io.github.chi2l3s.nextlib.api.database.DatabaseManager;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Applies schema definitions by emitting {@code CREATE TABLE IF NOT EXISTS} statements
 * for each declared table. This keeps database bootstrapping simple for plugins that
 * ship the generated repositories inside their JARs.
 */
public final class SchemaMigrator {
    private final SchemaParser parser = new SchemaParser();

    /**
     * Parses the supplied YAML schema and executes {@code CREATE TABLE IF NOT EXISTS}
     * statements against the datasource referenced by {@code schema.datasource}.
     */
    public void migrate(DatabaseManager manager, Path schemaPath) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(schemaPath, "schemaPath");
        SchemaDefinition definition = parser.parse(schemaPath);
        migrate(manager, definition);
    }

    /**
     * Executes {@code CREATE TABLE IF NOT EXISTS} statements for every table inside the
     * provided schema using the datasource registered with the schema's datasource id.
     */
    public void migrate(DatabaseManager manager, SchemaDefinition schema) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(schema, "schema");
        DatabaseClient client = manager.getOrThrow(schema.getDatasource());
        migrate(client, schema);
    }

    /**
     * Executes {@code CREATE TABLE IF NOT EXISTS} statements for every table inside the
     * provided schema using the supplied {@link DatabaseClient}.
     */
    public void migrate(DatabaseClient client, SchemaDefinition schema) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(schema, "schema");
        schema.validate();
        client.withConnection(connection -> {
            for (TableDefinition table : schema.getTables()) {
                executeCreateTable(connection, table);
            }
            return null;
        });
    }

    private void executeCreateTable(Connection connection, TableDefinition table) {
        String ddl = buildCreateTableStatement(table);
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(ddl);
        } catch (SQLException exception) {
            throw new DatabaseException("Failed to create table '" + table.getTableName() + "'", exception);
        }
    }

    private String buildCreateTableStatement(TableDefinition table) {
        List<String> columnDefinitions = new ArrayList<>();
        for (FieldDefinition field : table.getFields()) {
            StringBuilder column = new StringBuilder();
            column.append(columnName(field)).append(' ').append(field.getType().getSqlType());
            if (field.isId() || !field.isNullable()) {
                column.append(" NOT NULL");
            }
            if (field.isId()) {
                column.append(" PRIMARY KEY");
            } else if (field.isUnique()) {
                column.append(" UNIQUE");
            }
            if (field.getDefaultValue() != null && !field.getDefaultValue().isBlank()) {
                column.append(" DEFAULT ").append(field.getDefaultValue());
            }
            columnDefinitions.add(column.toString());
        }
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE IF NOT EXISTS ")
                .append(table.getTableName())
                .append(" (")
                .append(String.join(", ", columnDefinitions))
                .append(")");
        return builder.toString();
    }

    private String columnName(FieldDefinition field) {
        return field.getColumnName() != null ? field.getColumnName() : field.getName();
    }
}