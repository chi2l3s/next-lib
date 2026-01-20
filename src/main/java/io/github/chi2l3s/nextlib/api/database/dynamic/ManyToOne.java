package io.github.chi2l3s.nextlib.api.database.dynamic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a many-to-one relationship with another entity.
 * <p>
 * Example:
 * <pre>{@code
 * public class Quest {
 *     @PrimaryKey
 *     private UUID id;
 *     private String name;
 *
 *     @ManyToOne(fetch = FetchType.EAGER)
 *     @JoinColumn(name = "player_id")
 *     private Player player;
 * }
 *
 * public class Player {
 *     @PrimaryKey
 *     private UUID id;
 *
 *     @OneToMany(mappedBy = "player")
 *     private List<Quest> quests;
 * }
 * }</pre>
 *
 * @since 1.0.7
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ManyToOne {
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
    FetchType fetch() default FetchType.EAGER;
}
