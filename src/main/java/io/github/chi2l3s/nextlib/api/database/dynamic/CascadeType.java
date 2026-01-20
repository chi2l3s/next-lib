package io.github.chi2l3s.nextlib.api.database.dynamic;

/**
 * Defines cascade operations that should be propagated to related entities.
 *
 * @since 1.0.7
 */
public enum CascadeType {
    /**
     * Cascade persist (insert) operations.
     */
    PERSIST,

    /**
     * Cascade remove (delete) operations.
     */
    REMOVE,

    /**
     * Cascade merge (update) operations.
     */
    MERGE,

    /**
     * Cascade refresh operations.
     */
    REFRESH,

    /**
     * Cascade all operations (PERSIST, REMOVE, MERGE, REFRESH).
     */
    ALL
}
