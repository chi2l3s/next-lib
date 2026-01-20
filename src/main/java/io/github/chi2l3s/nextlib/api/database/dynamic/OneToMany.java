package io.github.chi2l3s.nextlib.api.database.dynamic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a one-to-many relationship with another entity.
 * <p>
 * Example:
 * <pre>{@code
 * public class Player {
 *     @PrimaryKey
 *     private UUID id;
 *
 *     @OneToMany(mappedBy = "player", cascade = CascadeType.ALL)
 *     private List<Quest> quests;
 * }
 *
 * public class Quest {
 *     @PrimaryKey
 *     private UUID id;
 *
 *     @ManyToOne
 *     @JoinColumn(name = "player_id")
 *     private Player player;
 * }
 * }</pre>
 *
 * @since 1.0.7
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface OneToMany {
    /**
     * The field in the target entity that owns the relationship.
     * Required for @OneToMany.
     *
     * @return mapped by field name
     */
    String mappedBy();

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
}
