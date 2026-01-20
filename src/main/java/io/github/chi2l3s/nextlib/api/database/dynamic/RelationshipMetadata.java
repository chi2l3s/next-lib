package io.github.chi2l3s.nextlib.api.database.dynamic;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;
import io.github.chi2l3s.nextlib.api.database.DatabaseException;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Metadata for entity relationships.
 *
 * @since 1.0.7
 */
final class RelationshipMetadata {
    private final Field field;
    private final String fieldName;
    private final RelationshipType type;
    private final Class<?> targetEntity;
    private final FetchType fetchType;
    private final Set<CascadeType> cascadeTypes;
    private final String joinColumn;
    private final String mappedBy;
    private final boolean nullable;
    private final boolean unique;

    private RelationshipMetadata(Field field, RelationshipType type, Class<?> targetEntity,
                                FetchType fetchType, Set<CascadeType> cascadeTypes,
                                String joinColumn, String mappedBy, boolean nullable, boolean unique) {
        this.field = field;
        this.fieldName = field.getName();
        this.type = type;
        this.targetEntity = targetEntity;
        this.fetchType = fetchType;
        this.cascadeTypes = cascadeTypes;
        this.joinColumn = joinColumn;
        this.mappedBy = mappedBy;
        this.nullable = nullable;
        this.unique = unique;

        if (!field.canAccess(null)) {
            field.setAccessible(true);
        }
    }

    static RelationshipMetadata from(Field field) {
        RelationshipType type = detectRelationshipType(field);
        if (type == null) {
            return null;
        }

        Class<?> targetEntity = resolveTargetEntity(field, type);
        FetchType fetchType = extractFetchType(field, type);
        Set<CascadeType> cascadeTypes = extractCascadeTypes(field, type);
        String mappedBy = extractMappedBy(field, type);

        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        String joinColumnName = joinColumn != null ? joinColumn.name() : field.getName() + "_id";
        boolean nullable = joinColumn == null || joinColumn.nullable();
        boolean unique = joinColumn != null && joinColumn.unique();

        return new RelationshipMetadata(field, type, targetEntity, fetchType, cascadeTypes,
                joinColumnName, mappedBy, nullable, unique);
    }

    private static RelationshipType detectRelationshipType(Field field) {
        if (field.isAnnotationPresent(OneToOne.class)) {
            return RelationshipType.ONE_TO_ONE;
        }
        if (field.isAnnotationPresent(OneToMany.class)) {
            return RelationshipType.ONE_TO_MANY;
        }
        if (field.isAnnotationPresent(ManyToOne.class)) {
            return RelationshipType.MANY_TO_ONE;
        }
        return null;
    }

    private static Class<?> resolveTargetEntity(Field field, RelationshipType type) {
        if (type == RelationshipType.ONE_TO_MANY) {
            // Extract generic type from Collection
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType paramType) {
                Type[] typeArgs = paramType.getActualTypeArguments();
                if (typeArgs.length == 1 && typeArgs[0] instanceof Class<?>) {
                    return (Class<?>) typeArgs[0];
                }
            }
            throw new DatabaseException("Cannot determine target entity for @OneToMany field " + field.getName());
        }
        return field.getType();
    }

    private static FetchType extractFetchType(Field field, RelationshipType type) {
        return switch (type) {
            case ONE_TO_ONE -> field.getAnnotation(OneToOne.class).fetch();
            case ONE_TO_MANY -> field.getAnnotation(OneToMany.class).fetch();
            case MANY_TO_ONE -> field.getAnnotation(ManyToOne.class).fetch();
        };
    }

    private static Set<CascadeType> extractCascadeTypes(Field field, RelationshipType type) {
        CascadeType[] cascades = switch (type) {
            case ONE_TO_ONE -> field.getAnnotation(OneToOne.class).cascade();
            case ONE_TO_MANY -> field.getAnnotation(OneToMany.class).cascade();
            case MANY_TO_ONE -> field.getAnnotation(ManyToOne.class).cascade();
        };

        Set<CascadeType> result = new HashSet<>();
        for (CascadeType cascade : cascades) {
            if (cascade == CascadeType.ALL) {
                result.add(CascadeType.PERSIST);
                result.add(CascadeType.REMOVE);
                result.add(CascadeType.MERGE);
                result.add(CascadeType.REFRESH);
            } else {
                result.add(cascade);
            }
        }
        return result;
    }

    private static String extractMappedBy(Field field, RelationshipType type) {
        return switch (type) {
            case ONE_TO_ONE -> field.getAnnotation(OneToOne.class).mappedBy();
            case ONE_TO_MANY -> field.getAnnotation(OneToMany.class).mappedBy();
            case MANY_TO_ONE -> ""; // ManyToOne doesn't have mappedBy
        };
    }

    String getFieldName() {
        return fieldName;
    }

    RelationshipType getType() {
        return type;
    }

    Class<?> getTargetEntity() {
        return targetEntity;
    }

    FetchType getFetchType() {
        return fetchType;
    }

    String getJoinColumn() {
        return joinColumn;
    }

    String getMappedBy() {
        return mappedBy;
    }

    boolean isNullable() {
        return nullable;
    }

    boolean isUnique() {
        return unique;
    }

    boolean shouldCascade(CascadeType operation) {
        return cascadeTypes.contains(operation);
    }

    boolean isOwner() {
        return mappedBy.isEmpty();
    }

    Object getValue(Object instance) {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Failed to get relationship value for field " + fieldName, e);
        }
    }

    void setValue(Object instance, Object value) {
        try {
            field.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Failed to set relationship value for field " + fieldName, e);
        }
    }

    enum RelationshipType {
        ONE_TO_ONE,
        ONE_TO_MANY,
        MANY_TO_ONE
    }
}
