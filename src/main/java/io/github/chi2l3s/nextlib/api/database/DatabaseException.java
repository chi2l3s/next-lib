package io.github.chi2l3s.nextlib.api.database;

/**
 * Runtime exception used by the database module.
 */
public class DatabaseException extends RuntimeException {
    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
