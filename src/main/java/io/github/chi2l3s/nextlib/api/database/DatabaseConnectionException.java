package io.github.chi2l3s.nextlib.api.database;

/**
 * Exception thrown when database connection fails.
 */
public class DatabaseConnectionException extends DatabaseException {

    public DatabaseConnectionException(String message) {
        super(message);
    }

    public DatabaseConnectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatabaseConnectionException(Throwable cause) {
        super("Failed to establish database connection", cause);
    }
}
