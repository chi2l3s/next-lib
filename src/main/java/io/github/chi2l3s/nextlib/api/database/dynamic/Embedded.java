package io.github.chi2l3s.nextlib.api.database.dynamic;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as an embedded object whose fields should be flattened into the parent table.
 * <p>
 * Example:
 * <pre>{@code
 * public class Address {
 *     private String street;
 *     private String city;
 *     private String zipCode;
 * }
 *
 * public class Player {
 *     @PrimaryKey
 *     private UUID id;
 *     private String name;
 *
 *     @Embedded
 *     private Address address; // Creates columns: address_street, address_city, address_zipCode
 * }
 * }</pre>
 *
 * @since 1.0.7
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Embedded {
    /**
     * Optional prefix for embedded field columns.
     * If not specified, uses the field name as prefix.
     *
     * @return column prefix
     */
    String prefix() default "";
}
