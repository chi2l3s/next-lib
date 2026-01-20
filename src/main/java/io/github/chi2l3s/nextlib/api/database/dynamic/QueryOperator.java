package io.github.chi2l3s.nextlib.api.database.dynamic;

/**
 * SQL comparison operators for query building.
 *
 * @since 1.0.7
 */
public enum QueryOperator {
    /**
     * Equals (=)
     */
    EQUALS("="),

    /**
     * Not equals (!=)
     */
    NOT_EQUALS("!="),

    /**
     * Greater than (>)
     */
    GREATER_THAN(">"),

    /**
     * Greater than or equals (>=)
     */
    GREATER_THAN_OR_EQUALS(">="),

    /**
     * Less than (<)
     */
    LESS_THAN("<"),

    /**
     * Less than or equals (<=)
     */
    LESS_THAN_OR_EQUALS("<="),

    /**
     * SQL LIKE pattern matching
     */
    LIKE("LIKE"),

    /**
     * SQL NOT LIKE pattern matching
     */
    NOT_LIKE("NOT LIKE"),

    /**
     * IN clause (value in list)
     */
    IN("IN"),

    /**
     * NOT IN clause (value not in list)
     */
    NOT_IN("NOT IN"),

    /**
     * BETWEEN clause (value between two values)
     */
    BETWEEN("BETWEEN"),

    /**
     * IS NULL check
     */
    IS_NULL("IS NULL"),

    /**
     * IS NOT NULL check
     */
    IS_NOT_NULL("IS NOT NULL");

    private final String sql;

    QueryOperator(String sql) {
        this.sql = sql;
    }

    /**
     * Returns the SQL representation of this operator.
     *
     * @return SQL string
     */
    public String getSql() {
        return sql;
    }
}
