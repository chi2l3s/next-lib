package io.github.chi2l3s.nextlib.api.database;

/**
 * Exception thrown when SQL query execution fails.
 */
public class QueryExecutionException extends DatabaseException {

    private final String sql;

    public QueryExecutionException(String sql, Throwable cause) {
        super("Failed to execute query: " + sql, cause);
        this.sql = sql;
    }

    public QueryExecutionException(String sql, String message, Throwable cause) {
        super(message + ": " + sql, cause);
        this.sql = sql;
    }

    public String getSql() {
        return sql;
    }
}
