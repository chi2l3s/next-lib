package io.github.chi2l3s.nextlib.api.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight connection manager that creates {@link DatabaseClient} instances on demand.
 */
public final class DatabaseManager implements AutoCloseable {
    private final Map<String, DatabaseClient> clients = new ConcurrentHashMap<>();
    private volatile String defaultClient;

    public DatabaseClient register(String name, DatabaseConfig config) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(config, "config");
        DatabaseClient client = createClient(config);
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

    private DatabaseClient createClient(DatabaseConfig config) {
        try {
            Class.forName(config.getType().getDriverClassName());
        } catch (ClassNotFoundException exception) {
            throw new DatabaseException("Missing JDBC driver for " + config.getType(), exception);
        }
        Properties properties = new Properties();
        properties.putAll(config.getProperties());
        if (config.getType() != DatabaseType.SQLITE) {
            properties.setProperty("user", config.getUsername());
            properties.setProperty("password", config.getPassword());
        }
        SqlSupplier<Connection> supplier = () -> DriverManager.getConnection(
                config.getType().buildJdbcUrl(config),
                properties);
        return new DatabaseClient(supplier);
    }

    @Override
    public void close() {
        clients.clear();
        defaultClient = null;
    }
}