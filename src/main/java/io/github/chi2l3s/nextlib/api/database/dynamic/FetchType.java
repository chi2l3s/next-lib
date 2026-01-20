package io.github.chi2l3s.nextlib.api.database.dynamic;

/**
 * Defines strategies for fetching related entities.
 *
 * @since 1.0.7
 */
public enum FetchType {
    /**
     * Fetch the relationship immediately when loading the entity.
     */
    EAGER,

    /**
     * Fetch the relationship on-demand when first accessed.
     * Uses dynamic proxies to delay database queries.
     */
    LAZY
}
