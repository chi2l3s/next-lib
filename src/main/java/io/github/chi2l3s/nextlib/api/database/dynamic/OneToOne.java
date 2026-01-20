package io.github.chi2l3s.nextlib.api.database.dynamic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a one-to-one relationship with another entity.
 * <p>
 * Example:
 * <pre>{@code
 * public class Player {
 *     @PrimaryKey
 *     private UUID id;
 *
 *     @OneToOne(cascade = CascadeType.ALL)
 *     @JoinColumn(name = "profile_id")
 *     private PlayerProfile profile;
 * }
 * }</pre>
 *
 * @since 1.0.7
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OneToOne {
    /**
     * Cascade operations to apply.
     *
     * @return cascade types
     */
    CascadeType[] cascade() default {};

    /**
     * Whether to fetch the relationship eagerly or lazily.
     *
     * @return fetch type
     */
    FetchType fetch() default FetchType.LAZY;

    /**
     * The field in the target entity that owns the relationship.
     * Used for bidirectional relationships.
     *
     * @return mapped by field name
     */
    String mappedBy() default "";
}
