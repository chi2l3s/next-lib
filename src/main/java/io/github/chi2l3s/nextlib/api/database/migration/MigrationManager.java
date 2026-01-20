package io.github.chi2l3s.nextlib.api.database.migration;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;
import io.github.chi2l3s.nextlib.api.database.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages database schema migrations with versioning and rollback support.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MigrationManager migrations = new MigrationManager(databaseClient);
 * migrations.init();
 *
 * Migration m1 = Migration.builder("001")
 *     .description("Add email column to players")
 *     .addColumn("players", "email", "TEXT")
 *     .build();
 *
 * migrations.apply(m1);
 * }</pre>
 *
 * @since 1.0.7
 */
public final class MigrationManager {
    private static final String MIGRATIONS_TABLE = "schema_migrations";
    private final DatabaseClient client;

    public MigrationManager(DatabaseClient client) {
        this.client = client;
    }

    /**
     * Initializes the migration tracking table.
     */
    public void init() {
        String createTable = "CREATE TABLE IF NOT EXISTS " + MIGRATIONS_TABLE + " (" +
                "version VARCHAR(255) PRIMARY KEY, " +
                "description TEXT, " +
                "applied_at TIMESTAMP NOT NULL, " +
                "execution_time_ms BIGINT NOT NULL" +
                ")";

        client.execute(createTable, stmt -> {});
    }

    /**
     * Applies a migration if not already applied.
     *
     * @param migration the migration to apply
     * @return true if applied, false if already applied
     */
    public boolean apply(Migration migration) {
        if (isApplied(migration.getVersion())) {
            return false;
        }

        long startTime = System.currentTimeMillis();

        try {
            client.withConnection(conn -> {
                conn.setAutoCommit(false);
                try {
                    // Execute all up statements
                    for (String statement : migration.getUpStatements()) {
                        try (PreparedStatement stmt = conn.prepareStatement(statement)) {
                            stmt.execute();
                        }
                    }

                    // Record migration
                    recordMigration(conn, migration, System.currentTimeMillis() - startTime);

                    conn.commit();
                    return null;
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            });
            return true;
        } catch (Exception e) {
            throw new DatabaseException("Failed to apply migration " + migration.getVersion(), e);
        }
    }

    /**
     * Rolls back a migration.
     *
     * @param version the migration version to rollback
     */
    public void rollback(String version) {
        Migration migration = getAppliedMigration(version);
        if (migration == null) {
            throw new DatabaseException("Migration " + version + " is not applied");
        }

        try {
            client.withConnection(conn -> {
                conn.setAutoCommit(false);
                try {
                    // Execute all down statements
                    for (String statement : migration.getDownStatements()) {
                        if (statement.startsWith("--")) {
                            // Skip comments (usually irreversible operations)
                            continue;
                        }
                        try (PreparedStatement stmt = conn.prepareStatement(statement)) {
                            stmt.execute();
                        }
                    }

                    // Remove migration record
                    String deleteSql = "DELETE FROM " + MIGRATIONS_TABLE + " WHERE version = ?";
                    try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
                        stmt.setString(1, version);
                        stmt.executeUpdate();
                    }

                    conn.commit();
                    return null;
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            });
        } catch (Exception e) {
            throw new DatabaseException("Failed to rollback migration " + version, e);
        }
    }

    /**
     * Checks if a migration has been applied.
     *
     * @param version the migration version
     * @return true if applied
     */
    public boolean isApplied(String version) {
        String sql = "SELECT COUNT(*) FROM " + MIGRATIONS_TABLE + " WHERE version = ?";
        return client.queryOne(sql,
                stmt -> stmt.setString(1, version),
                rs -> rs.getInt(1) > 0
        ).orElse(false);
    }

    /**
     * Gets all applied migrations.
     *
     * @return list of applied migrations
     */
    public List<AppliedMigration> getAppliedMigrations() {
        String sql = "SELECT version, description, applied_at, execution_time_ms FROM " +
                MIGRATIONS_TABLE + " ORDER BY version";

        return client.query(sql, stmt -> {}, rs -> {
            AppliedMigration migration = new AppliedMigration();
            migration.version = rs.getString("version");
            migration.description = rs.getString("description");
            migration.appliedAt = rs.getTimestamp("applied_at").toInstant();
            migration.executionTimeMs = rs.getLong("execution_time_ms");
            return migration;
        });
    }

    private Migration getAppliedMigration(String version) {
        // In real implementation, this would load the migration from a file or registry
        // For now, return null to indicate we can't load it
        return null;
    }

    private void recordMigration(Connection conn, Migration migration, long executionTimeMs) throws SQLException {
        String sql = "INSERT INTO " + MIGRATIONS_TABLE +
                " (version, description, applied_at, execution_time_ms) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, migration.getVersion());
            stmt.setString(2, migration.getDescription());
            stmt.setTimestamp(3, java.sql.Timestamp.from(Instant.now()));
            stmt.setLong(4, executionTimeMs);
            stmt.executeUpdate();
        }
    }

    /**
     * Information about an applied migration.
     */
    public static class AppliedMigration {
        private String version;
        private String description;
        private Instant appliedAt;
        private long executionTimeMs;

        public String getVersion() {
            return version;
        }

        public String getDescription() {
            return description;
        }

        public Instant getAppliedAt() {
            return appliedAt;
        }

        public long getExecutionTimeMs() {
            return executionTimeMs;
        }
    }
}
