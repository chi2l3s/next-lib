package io.github.chi2l3s.nextlib.api.database.migration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a database schema migration.
 *
 * @since 1.0.7
 */
public final class Migration {
    private final String version;
    private final String description;
    private final List<String> upStatements;
    private final List<String> downStatements;
    private final Instant createdAt;

    private Migration(String version, String description, List<String> upStatements,
                     List<String> downStatements, Instant createdAt) {
        this.version = version;
        this.description = description;
        this.upStatements = upStatements;
        this.downStatements = downStatements;
        this.createdAt = createdAt;
    }

    public static Builder builder(String version) {
        return new Builder(version);
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getUpStatements() {
        return upStatements;
    }

    public List<String> getDownStatements() {
        return downStatements;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public static class Builder {
        private final String version;
        private String description = "";
        private final List<String> upStatements = new ArrayList<>();
        private final List<String> downStatements = new ArrayList<>();

        private Builder(String version) {
            this.version = version;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder addUp(String statement) {
            upStatements.add(statement);
            return this;
        }

        public Builder addDown(String statement) {
            downStatements.add(statement);
            return this;
        }

        public Builder addColumn(String table, String column, String type) {
            upStatements.add("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
            downStatements.add("ALTER TABLE " + table + " DROP COLUMN " + column);
            return this;
        }

        public Builder dropColumn(String table, String column) {
            upStatements.add("ALTER TABLE " + table + " DROP COLUMN " + column);
            // Note: Cannot safely recreate dropped column without type info
            downStatements.add("-- Cannot auto-rollback DROP COLUMN " + column);
            return this;
        }

        public Builder createTable(String table, List<String> columns) {
            StringBuilder sb = new StringBuilder("CREATE TABLE ");
            sb.append(table).append(" (");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append(columns.get(i));
            }
            sb.append(")");
            upStatements.add(sb.toString());
            downStatements.add("DROP TABLE " + table);
            return this;
        }

        public Builder dropTable(String table) {
            upStatements.add("DROP TABLE " + table);
            // Note: Cannot safely recreate dropped table
            downStatements.add("-- Cannot auto-rollback DROP TABLE " + table);
            return this;
        }

        public Migration build() {
            return new Migration(version, description, upStatements, downStatements, Instant.now());
        }
    }
}
