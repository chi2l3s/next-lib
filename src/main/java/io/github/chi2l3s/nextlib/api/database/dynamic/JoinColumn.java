package io.github.chi2l3s.nextlib.api.database.dynamic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the foreign key column for a relationship.
 * <p>
 * Example:
 * <pre>{@code
 * @ManyToOne
 * @JoinColumn(name = "player_id", nullable = false)
 * private Player player;
 * }</pre>
 *
 * @since 1.0.7
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface JoinColumn {
    /**
     * The name of the foreign key column.
     *
     * @return column name
     */
    String name();

    /**
     * Whether the foreign key column can be null.
     *
     * @return true if nullable
     */
    boolean nullable() default true;

    /**
     * Whether the foreign key column is unique.
     *
     * @return true if unique
     */
    boolean unique() default false;
}
