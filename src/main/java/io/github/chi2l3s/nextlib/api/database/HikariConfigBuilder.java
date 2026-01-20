package io.github.chi2l3s.nextlib.api.database;

import com.zaxxer.hikari.HikariConfig;

import java.util.Locale;
import java.util.Properties;

/**
 * Builder for creating HikariCP configuration from DatabaseConfig.
 * <p>
 * Handles property mapping and validation for HikariCP connection pools.
 * </p>
 *
 * @since 1.0.6
 */
public final class HikariConfigBuilder {

    private HikariConfigBuilder() {
        // Utility class
    }

    /**
     * Creates a HikariConfig from DatabaseConfig.
     *
     * @param poolName the name for the connection pool
     * @param config   the database configuration
     * @return configured HikariConfig instance
     * @throws ConfigurationException if configuration is invalid
     */
    public static HikariConfig build(String poolName, DatabaseConfig config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setPoolName("nextlib-" + poolName);
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

        return hikariConfig;
    }

    /**
     * Applies a HikariCP-specific property to the configuration.
     *
     * @param hikariConfig the config to modify
     * @param key          property name
     * @param value        property value
     * @return true if the property was recognized and applied, false otherwise
     * @throws ConfigurationException if property value is invalid
     */
    private static boolean applyHikariProperty(HikariConfig hikariConfig, String key, String value) {
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
            throw new ConfigurationException("Invalid HikariCP property value for '" + key + "': " + value, exception);
        }
    }
}
