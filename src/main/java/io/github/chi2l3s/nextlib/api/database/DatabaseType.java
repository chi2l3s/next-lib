package io.github.chi2l3s.nextlib.api.database;

import java.util.Locale;

/**
 * Supported relational database engines.
 */
public enum DatabaseType {
    MYSQL("com.mysql.cj.jdbc.Driver") {
        @Override
        public String buildJdbcUrl(DatabaseConfig config) {
            return String.format(Locale.ROOT,
                    "jdbc:mysql://%s:%d/%s",
                    config.getHost(),
                    config.getPortOrDefault(3306),
                    config.getDatabase());
        }
    },
    POSTGRESQL("org.postgresql.Driver") {
        @Override
        public String buildJdbcUrl(DatabaseConfig config) {
            return String.format(Locale.ROOT,
                    "jdbc:postgresql://%s:%d/%s",
                    config.getHost(),
                    config.getPortOrDefault(5432),
                    config.getDatabase());
        }
    },
    SQLITE("org.sqlite.JDBC") {
        @Override
        public String buildJdbcUrl(DatabaseConfig config) {
            return "jdbc:sqlite:" + config.getFile();
        }
    };

    private final String driverClassName;

    DatabaseType(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getDriverClassName() {
        return driverClassName;
    }

    public abstract String buildJdbcUrl(DatabaseConfig config);
}
