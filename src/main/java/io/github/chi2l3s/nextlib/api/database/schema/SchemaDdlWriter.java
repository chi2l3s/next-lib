package io.github.chi2l3s.nextlib.api.database.schema;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Utility for turning {@link SchemaDefinition schema definitions} into SQL DDL statements.
 * <p>
 * The generator can use this writer to persist an accompanying {@code schema.sql} file during
 * build time, so plugins may ship the DDL alongside the compiled repositories and execute it when
 * a datasource becomes available at runtime.
 */
public final class SchemaDdlWriter {

    /**
     * Creates {@code CREATE TABLE IF NOT EXISTS} statements for each table described in the schema.
     *
     * @param schema schema metadata to transform into DDL
     * @return ordered list of SQL statements
     */
    public List<String> createStatements(SchemaDefinition schema) {
        Objects.requireNonNull(schema, "schema");
        schema.validate();
        List<String> statements = new ArrayList<>();
        for (TableDefinition table : schema.getTables()) {
            statements.add(buildCreateTableStatement(table));
        }
        return statements;
    }

    /**
     * Writes {@code CREATE TABLE IF NOT EXISTS} statements into the provided file. Each statement
     * is terminated with a semicolon and separated by a newline.
     */
    public void writeToFile(SchemaDefinition schema, Path outputFile) {
        Objects.requireNonNull(schema, "schema");
        Objects.requireNonNull(outputFile, "outputFile");
        try {
            Path parent = outputFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            List<String> statements = createStatements(schema);
            StringBuilder builder = new StringBuilder();
            for (String statement : statements) {
                builder.append(statement).append(";").append(System.lineSeparator());
            }
            Files.writeString(outputFile, builder.toString(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to write schema DDL", exception);
        }
    }

    private String buildCreateTableStatement(TableDefinition table) {
        Objects.requireNonNull(table, "table");
        List<String> columnDefinitions = new ArrayList<>();
        for (FieldDefinition field : table.getFields()) {
            columnDefinitions.add(buildColumnDefinition(field));
        }
        StringBuilder builder = new StringBuilder();
        builder.append("CREATE TABLE IF NOT EXISTS ")
                .append(table.getTableName())
                .append(" (")
                .append(String.join(", ", columnDefinitions))
                .append(")");
        return builder.toString();
    }

    private String buildColumnDefinition(FieldDefinition field) {
        StringBuilder column = new StringBuilder();
        column.append(columnName(field))
                .append(' ')
                .append(field.getType().getSqlType());
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
        return column.toString();
    }

    private String columnName(FieldDefinition field) {
        return field.getColumnName() != null ? field.getColumnName() : field.getName();
    }
}