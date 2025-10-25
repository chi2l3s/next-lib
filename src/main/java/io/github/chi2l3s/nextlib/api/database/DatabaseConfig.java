package io.github.chi2l3s.nextlib.api.database;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable configuration for database connections.
 */
public final class DatabaseConfig {
    private final DatabaseType type;
    private final String host;
    private final Integer port;
    private final String database;
    private final String username;
    private final String password;
    private final String file;
    private final Map<String, String> properties;

    private DatabaseConfig(Builder builder) {
        this.type = Objects.requireNonNull(builder.type, "type");
        this.host = builder.host;
        this.port = builder.port;
        this.database = builder.database;
        this.username = builder.username;
        this.password = builder.password;
        this.file = builder.file;
        this.properties = Collections.unmodifiableMap(new HashMap<>(builder.properties));
        validate();
    }

    private void validate() {
        switch (type) {
            case MYSQL:
            case POSTGRESQL:
                Objects.requireNonNull(host, "host");
                Objects.requireNonNull(database, "database");
                Objects.requireNonNull(username, "username");
                Objects.requireNonNull(password, "password");
                break;
            case SQLITE:
                Objects.requireNonNull(file, "file");
                break;
            default:
                throw new IllegalStateException("Unsupported database type: " + type);
        }
    }

    public static Builder builder(DatabaseType type) {
        return new Builder(type);
    }

    public DatabaseType getType() {
        return type;
    }

    public String getHost() {
        return host;
    }

    public int getPortOrDefault(int defaultPort) {
        return Optional.ofNullable(port).orElse(defaultPort);
    }

    public Integer getPort() {
        return port;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getFile() {
        return file;
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public static final class Builder {
        private final DatabaseType type;
        private String host;
        private Integer port;
        private String database;
        private String username;
        private String password;
        private String file;
        private final Map<String, String> properties = new HashMap<>();

        private Builder(DatabaseType type) {
            this.type = Objects.requireNonNull(type, "type");
        }

        public Builder host(String host) {
            this.host = host;
            return this;
        }

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public Builder database(String database) {
            this.database = database;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder file(String file) {
            this.file = file;
            return this;
        }

        public Builder property(String key, String value) {
            this.properties.put(key, value);
            return this;
        }

        public Builder properties(Map<String, String> properties) {
            this.properties.putAll(properties);
            return this;
        }

        public DatabaseConfig build() {
            return new DatabaseConfig(this);
        }
    }
}
