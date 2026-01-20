package io.github.chi2l3s.nextlib.api.database;

/**
 * Exception thrown when database configuration is invalid.
 */
public class ConfigurationException extends DatabaseException {

    public ConfigurationException(String message) {
        super("Invalid database configuration: " + message);
    }

    public ConfigurationException(String message, Throwable cause) {
        super("Invalid database configuration: " + message, cause);
    }
}
