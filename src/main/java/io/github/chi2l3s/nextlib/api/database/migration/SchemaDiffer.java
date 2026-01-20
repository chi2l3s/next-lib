package io.github.chi2l3s.nextlib.api.database.migration;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * Compares database schema with entity definitions to generate migrations.
 *
 * @since 1.0.7
 */
public final class SchemaDiffer {
    private final DatabaseClient client;

    public SchemaDiffer(DatabaseClient client) {
        this.client = client;
    }

    /**
     * Generates a migration by comparing current schema with expected schema.
     *
     * @param tableName the table to check
     * @param expectedColumns the expected column definitions
     * @return a migration or null if no changes needed
     */
    public Migration generateMigration(String tableName, Map<String, ColumnDefinition> expectedColumns) {
        Map<String, ColumnInfo> currentColumns = getCurrentColumns(tableName);

        Migration.Builder migration = Migration.builder("auto_" + System.currentTimeMillis())
                .description("Auto-generated schema update for " + tableName);

        boolean hasChanges = false;

        // Check for new columns
        for (Map.Entry<String, ColumnDefinition> entry : expectedColumns.entrySet()) {
            String columnName = entry.getKey();
            ColumnDefinition expected = entry.getValue();

            if (!currentColumns.containsKey(columnName.toLowerCase())) {
                // Column doesn't exist - add it
                migration.addColumn(tableName, columnName, expected.getType());
                hasChanges = true;
            }
        }

        // Check for removed columns
        for (String currentColumn : currentColumns.keySet()) {
            if (!expectedColumns.containsKey(currentColumn)) {
                // Column exists but not in entity - might want to remove it
                // Be cautious here - commenting out removal for safety
                // migration.dropColumn(tableName, currentColumn);
                // hasChanges = true;
            }
        }

        return hasChanges ? migration.build() : null;
    }

    /**
     * Gets current columns from database.
     */
    private Map<String, ColumnInfo> getCurrentColumns(String tableName) {
        return client.withConnection(conn -> {
            Map<String, ColumnInfo> columns = new HashMap<>();

            try {
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet rs = metaData.getColumns(null, null, tableName, null)) {
                    while (rs.next()) {
                        ColumnInfo info = new ColumnInfo();
                        info.name = rs.getString("COLUMN_NAME").toLowerCase();
                        info.type = rs.getString("TYPE_NAME");
                        info.size = rs.getInt("COLUMN_SIZE");
                        info.nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;

                        columns.put(info.name, info);
                    }
                }
            } catch (SQLException e) {
                // Table might not exist yet
                return columns;
            }

            return columns;
        });
    }

    /**
     * Checks if a table exists.
     */
    public boolean tableExists(String tableName) {
        return client.withConnection(conn -> {
            try {
                DatabaseMetaData metaData = conn.getMetaData();
                try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
                    return rs.next();
                }
            } catch (SQLException e) {
                return false;
            }
        });
    }

    /**
     * Definition of a column in entity.
     */
    public static class ColumnDefinition {
        private final String type;
        private final boolean nullable;
        private final boolean primaryKey;

        public ColumnDefinition(String type, boolean nullable, boolean primaryKey) {
            this.type = type;
            this.nullable = nullable;
            this.primaryKey = primaryKey;
        }

        public String getType() {
            return type;
        }

        public boolean isNullable() {
            return nullable;
        }

        public boolean isPrimaryKey() {
            return primaryKey;
        }
    }

    /**
     * Information about a column in database.
     */
    private static class ColumnInfo {
        String name;
        String type;
        int size;
        boolean nullable;
    }
}
