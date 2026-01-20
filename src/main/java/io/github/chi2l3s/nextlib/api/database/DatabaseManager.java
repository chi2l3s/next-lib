package io.github.chi2l3s.nextlib.api.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight connection manager that creates {@link DatabaseClient} instances on demand.
 * <p>
 * Manages multiple database connections with HikariCP pooling. Supports MySQL, PostgreSQL, and SQLite.
 * Each registered client maintains its own connection pool and can be accessed by name or as the default client.
 * </p>
 *
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * DatabaseManager manager = new DatabaseManager();
 *
 * // Register MySQL client
 * DatabaseConfig config = DatabaseConfig.builder(DatabaseType.MYSQL)
 *     .host("localhost")
 *     .port(3306)
 *     .database("mydb")
 *     .username("root")
 *     .password("password")
 *     .property("maximumPoolSize", "10")
 *     .build();
 *
 * DatabaseClient client = manager.register("main", config);
 *
 * // Use default client
 * DatabaseClient defaultClient = manager.getDefault();
 *
 * // Close all connections when done
 * manager.close();
 * }</pre>
 *
 * @see DatabaseClient
 * @see DatabaseConfig
 * @see DatabaseType
 * @since 1.0.0
 */
public final class DatabaseManager implements AutoCloseable {
    private final Map<String, DatabaseClient> clients = new ConcurrentHashMap<>();
    private final Map<String, HikariDataSource> dataSources = new ConcurrentHashMap<>();
    private volatile String defaultClient;

    /**
     * Registers a new database client with the specified name and configuration.
     * <p>
     * If a client with the same name already exists, it will be closed and replaced.
     * The first registered client automatically becomes the default client.
     * </p>
     *
     * @param name   unique identifier for this client (not null)
     * @param config database configuration (not null)
     * @return the registered DatabaseClient
     * @throws ConfigurationException if the configuration is invalid or JDBC driver is missing
     * @throws NullPointerException   if name or config is null
     */
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

    /**
     * Retrieves a database client by name.
     *
     * @param name the client name
     * @return Optional containing the client if found, empty otherwise
     */
    public Optional<DatabaseClient> get(String name) {
        return Optional.ofNullable(clients.get(name));
    }

    /**
     * Retrieves a database client by name, throwing an exception if not found.
     *
     * @param name the client name
     * @return the DatabaseClient
     * @throws DatabaseException if no client with the given name exists
     */
    public DatabaseClient getOrThrow(String name) {
        return get(name).orElseThrow(() ->
                new DatabaseException("No database client registered with name '" + name + "'"));
    }

    /**
     * Returns the default database client.
     * <p>
     * The first registered client automatically becomes the default.
     * The default can be changed using {@link #setDefaultClient(String)}.
     * </p>
     *
     * @return the default DatabaseClient
     * @throws DatabaseException if no clients have been registered
     */
    public DatabaseClient getDefault() {
        if (defaultClient == null) {
            throw new DatabaseException("No database clients have been registered");
        }
        return getOrThrow(defaultClient);
    }

    /**
     * Unregisters and closes a database client.
     * <p>
     * If the unregistered client was the default, a new default will be automatically selected
     * from the remaining clients.
     * </p>
     *
     * @param name the client name to unregister
     */
    public void unregister(String name) {
        clients.remove(name);
        if (Objects.equals(defaultClient, name)) {
            defaultClient = clients.keySet().stream().findFirst().orElse(null);
        }
    }

    /**
     * Sets the default database client.
     *
     * @param name the name of the client to set as default
     * @throws DatabaseException if no client with the given name exists
     */
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
            throw new ConfigurationException("Missing JDBC driver for " + config.getType(), exception);
        }
        HikariDataSource dataSource = createDataSource(name, config);
        dataSources.put(name, dataSource);
        SqlSupplier<Connection> supplier = dataSource::getConnection;
        return new DatabaseClient(supplier);
    }

    private HikariDataSource createDataSource(String name, DatabaseConfig config) {
        try {
            HikariConfig hikariConfig = HikariConfigBuilder.build(name, config);
            return new HikariDataSource(hikariConfig);
        } catch (RuntimeException exception) {
            throw new ConfigurationException("Failed to configure HikariCP pool for '" + name + "'", exception);
        }
    }

    private void closeDataSource(String name) {
        HikariDataSource dataSource = dataSources.remove(name);
        if (dataSource != null) {
            dataSource.close();
        }
    }

    /**
     * Closes all registered database clients and their connection pools.
     * <p>
     * This method should be called when shutting down the application to properly release
     * all database resources.
     * </p>
     */
    @Override
    public void close() {
        dataSources.values().forEach(HikariDataSource::close);
        dataSources.clear();
        clients.clear();
        defaultClient = null;
    }
}