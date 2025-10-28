package io.github.chi2l3s.nextlib.api.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight connection manager that creates {@link DatabaseClient} instances on demand.
 */
public final class DatabaseManager implements AutoCloseable {
    private final Map<String, DatabaseClient> clients = new ConcurrentHashMap<>();
    private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private volatile String defaultClient;

    public DatabaseClient register(String name, DatabaseConfig config) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(config, "config");
        closeDataSource(name);
        DatabaseClient client = createClient(name, config);
        clients.put(name, client);
        if (defaultClient == null) {
            defaultClient = name;
        }
        return client;
    }

    public Optional<DatabaseClient> get(String name) {
        return Optional.ofNullable(clients.get(name));
    }

    public DatabaseClient getOrThrow(String name) {
        return get(name).orElseThrow(() ->
                new DatabaseException("No database client registered with name '" + name + "'"));
    }

    public DatabaseClient getDefault() {
        if (defaultClient == null) {
            throw new DatabaseException("No database clients have been registered");
        }
        return getOrThrow(defaultClient);
    }

    public void unregister(String name) {
        clients.remove(name);
        if (Objects.equals(defaultClient, name)) {
            defaultClient = clients.keySet().stream().findFirst().orElse(null);
        }
    }

    public void setDefaultClient(String name) {
        if (!clients.containsKey(name)) {
            throw new DatabaseException("No database client registered with name '" + name + "'");
        }
        this.defaultClient = name;
    }

    private DatabaseClient createClient(String name, DatabaseConfig config) {
        try {
            Class.forName(config.getType().getDriverClassName());
        } catch (ClassNotFoundException exception) {
            throw new DatabaseException("Missing JDBC driver for " + config.getType(), exception);
        }
        HikariDataSource dataSource = createDataSource(name, config);
        dataSources.put(name, dataSource);
        SqlSupplier<Connection> supplier = dataSource::getConnection;
        return new DatabaseClient(supplier);
    }

    private HikariDataSource createDataSource(String name, DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("nextlib-" + name);
        hikariConfig.setDriverClassName(config.getType().getDriverClassName());
        hikariConfig.setJdbcUrl(config.getType().buildJdbcUrl(config));
        if (config.getType() != DatabaseType.SQLITE) {
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(config.getPassword());
        }
        Properties properties = new Properties();
        properties.putAll(config.getProperties());

        properties.forEach((key, value) -> {
            if (key == null || value == null) {
                return;
            }
            String propertyName = key.toString();
            String propertyValue = value.toString();
            if (!applyHikariProperty(hikariConfig, propertyName, propertyValue)) {
                hikariConfig.addDataSourceProperty(propertyName, propertyValue);
            }
        });

        try {
            return new HikariDataSource(hikariConfig);
        } catch (RuntimeException exception) {
            throw new DatabaseException("Failed to configure HikariCP pool for '" + name + "'", exception);
        }
    }

    private boolean applyHikariProperty(HikariConfig hikariConfig, String key, String value) {
        String normalized = key.toLowerCase(Locale.ROOT);
        try {
            return switch (normalized) {
                case "maximumpoolsize", "maxpoolsize" -> {
                    hikariConfig.setMaximumPoolSize(Integer.parseInt(value));
                    yield true;
                }
                case "minimumidle" -> {
                    hikariConfig.setMinimumIdle(Integer.parseInt(value));
                    yield true;
                }
                case "idletimeout" -> {
                    hikariConfig.setIdleTimeout(Long.parseLong(value));
                    yield true;
                }
                case "connectiontimeout" -> {
                    hikariConfig.setConnectionTimeout(Long.parseLong(value));
                    yield true;
                }
                case "maxlifetime" -> {
                    hikariConfig.setMaxLifetime(Long.parseLong(value));
                    yield true;
                }
                case "keepalivetime" -> {
                    hikariConfig.setKeepaliveTime(Long.parseLong(value));
                    yield true;
                }
                case "leakdetectionthreshold" -> {
                    hikariConfig.setLeakDetectionThreshold(Long.parseLong(value));
                    yield true;
                }
                case "initializationfailtimeout" -> {
                    hikariConfig.setInitializationFailTimeout(Long.parseLong(value));
                    yield true;
                }
                case "validationtimeout" -> {
                    hikariConfig.setValidationTimeout(Long.parseLong(value));
                    yield true;
                }
                case "schema" -> {
                    hikariConfig.setSchema(value);
                    yield true;
                }
                case "autocommit" -> {
                    hikariConfig.setAutoCommit(Boolean.parseBoolean(value));
                    yield true;
                }
                case "datasourceclassname" -> {
                    hikariConfig.setDataSourceClassName(value);
                    yield true;
                }
                default -> false;
            };
        } catch (NumberFormatException exception) {
            throw new DatabaseException("Invalid HikariCP property value for '" + key + "': " + value, exception);
        }
    }

    private void closeDataSource(String name) {
        HikariDataSource dataSource = dataSources.remove(name);
        if (dataSource != null) {
            dataSource.close();
        }
    }

    @Override
    public void close() {
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
        clients.clear();
        defaultClient = null;
    }
}