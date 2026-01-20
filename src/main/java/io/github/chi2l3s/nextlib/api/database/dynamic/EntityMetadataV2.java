package io.github.chi2l3s.nextlib.api.database.dynamic;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;
import io.github.chi2l3s.nextlib.api.database.DatabaseException;
import io.github.chi2l3s.nextlib.api.database.EntityMappingException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enhanced entity metadata with support for embedded objects and relationships.
 *
 * @since 1.0.8
 */
final class EntityMetadataV2<T> {
    private final Class<T> entityType;
    private final Constructor<T> constructor;
    private final List<EntityField> regularFields;  // Regular + flattened embedded fields
    private final List<EmbeddedFieldHandler> embeddedHandlers;
    private final List<RelationshipMetadata> relationships;
    private final Map<String, EntityField> fieldsByName;
    private final EntityField primaryKey;
    private final String columnList;

    private EntityMetadataV2(Class<T> entityType,
                            Constructor<T> constructor,
                            List<EntityField> regularFields,
                            List<EmbeddedFieldHandler> embeddedHandlers,
                            List<RelationshipMetadata> relationships,
                            EntityField primaryKey) {
        this.entityType = entityType;
        this.constructor = constructor;
        this.regularFields = regularFields;
        this.embeddedHandlers = embeddedHandlers;
        this.relationships = relationships;
        this.primaryKey = primaryKey;
        this.fieldsByName = new LinkedHashMap<>();

        for (EntityField field : regularFields) {
            this.fieldsByName.put(field.getFieldName(), field);
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < regularFields.size(); i++) {
            if (i > 0) {
                builder.append(',').append(' ');
            }
            builder.append(regularFields.get(i).getColumnName());
        }
        this.columnList = builder.toString();
    }

    static <T> EntityMetadataV2<T> inspect(Class<T> type) {
        List<Field> declaredFields = collectInstanceFields(type);
        if (declaredFields.isEmpty()) {
            throw new EntityMappingException(type, "Entity does not declare any fields");
        }

        List<EntityField> regularFields = new ArrayList<>();
        List<EmbeddedFieldHandler> embeddedHandlers = new ArrayList<>();
        List<RelationshipMetadata> relationships = new ArrayList<>();
        List<Field> constructorFields = new ArrayList<>();
        EntityField primaryKey = null;

        for (Field field : declaredFields) {
            // Check if it's an embedded field
            if (field.isAnnotationPresent(Embedded.class)) {
                EmbeddedFieldHandler handler = EmbeddedFieldHandler.from(field);
                embeddedHandlers.add(handler);
                regularFields.addAll(handler.getFlattenedFields());
                // Embedded fields are not part of constructor
                continue;
            }

            // Check if it's a relationship field
            RelationshipMetadata relationship = RelationshipMetadata.from(field);
            if (relationship != null) {
                relationships.add(relationship);
                // Relationships are not part of constructor
                continue;
            }

            // Regular field
            EntityField entityField = EntityField.from(field);
            regularFields.add(entityField);
            constructorFields.add(field);

            if (entityField.isPrimaryKey()) {
                primaryKey = entityField;
            }
        }

        if (primaryKey == null && !regularFields.isEmpty()) {
            primaryKey = regularFields.get(0);
        }

        Constructor<T> constructor = resolveConstructor(type, constructorFields);

        return new EntityMetadataV2<>(type, constructor, regularFields, embeddedHandlers,
                relationships, primaryKey);
    }

    private static <T> Constructor<T> resolveConstructor(Class<T> type, List<Field> fields) {
        List<Class<?>> parameterTypes = new ArrayList<>();
        for (Field field : fields) {
            parameterTypes.add(field.getType());
        }
        try {
            Constructor<T> constructor = type.getDeclaredConstructor(parameterTypes.toArray(new Class<?>[0]));
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor;
        } catch (ReflectiveOperationException exception) {
            throw new EntityMappingException(type, "Failed to resolve constructor", exception);
        }
    }

    private static List<Field> collectInstanceFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Field field : type.getDeclaredFields()) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || field.isSynthetic()) {
                continue;
            }
            if (!Modifier.isPublic(modifiers) || !Modifier.isPublic(type.getModifiers())) {
                field.setAccessible(true);
            }
            fields.add(field);
        }
        return fields;
    }

    Class<T> getEntityType() {
        return entityType;
    }

    List<EntityField> getFields() {
        return regularFields;
    }

    List<EmbeddedFieldHandler> getEmbeddedHandlers() {
        return embeddedHandlers;
    }

    List<RelationshipMetadata> getRelationships() {
        return relationships;
    }

    EntityField getPrimaryKey() {
        return primaryKey;
    }

    String columnList() {
        return columnList;
    }

    EntityField requireField(String name) {
        EntityField field = fieldsByName.get(name);
        if (field == null) {
            throw new DatabaseException("Unknown field '" + name + "' for entity " + entityType.getName());
        }
        return field;
    }

    Object getValue(Object instance, EntityField field) {
        try {
            return field.getValue(instance);
        } catch (IllegalAccessException exception) {
            throw new DatabaseException("Failed to read field '" + field.getFieldName() + "'", exception);
        }
    }

    T map(ResultSet resultSet, DatabaseClient client) {
        return map(resultSet, client, false);
    }

    T map(ResultSet resultSet, DatabaseClient client, boolean loadRelationships) {
        try {
            // Read regular fields
            Object[] constructorArgs = new Object[constructor.getParameterCount()];
            int argIndex = 0;

            for (EntityField field : regularFields) {
                // Skip embedded fields - they're not constructor parameters
                boolean isEmbeddedField = false;
                for (EmbeddedFieldHandler handler : embeddedHandlers) {
                    if (handler.getFlattenedFields().contains(field)) {
                        isEmbeddedField = true;
                        break;
                    }
                }

                if (!isEmbeddedField) {
                    constructorArgs[argIndex++] = field.read(resultSet);
                }
            }

            // Create instance
            T instance = constructor.newInstance(constructorArgs);

            // Reconstruct embedded objects
            for (EmbeddedFieldHandler handler : embeddedHandlers) {
                Object embedded = handler.reconstructEmbedded(resultSet);
                handler.setEmbedded(instance, embedded);
            }

            // Load relationships if requested
            if (loadRelationships) {
                loadRelationships(instance, client);
            }

            return instance;
        } catch (ReflectiveOperationException | SQLException exception) {
            throw new EntityMappingException(entityType, "Failed to map result set", exception);
        }
    }

    private void loadRelationships(T instance, DatabaseClient client) {
        for (RelationshipMetadata relationship : relationships) {
            if (relationship.getFetchType() == FetchType.EAGER) {
                // Load eagerly
                Object relationshipValue = loadRelationship(instance, relationship, client);
                relationship.setValue(instance, relationshipValue);
            } else {
                // Create lazy proxy
                Object proxy = createLazyProxy(instance, relationship, client);
                relationship.setValue(instance, proxy);
            }
        }
    }

    private Object loadRelationship(T instance, RelationshipMetadata relationship, DatabaseClient client) {
        Class<?> targetType = relationship.getTargetEntity();
        String joinColumn = relationship.getJoinColumn();

        // Get the foreign key value
        Object foreignKey = getValue(instance, primaryKey);

        // Load the related entity based on relationship type
        return switch (relationship.getType()) {
            case ONE_TO_ONE, MANY_TO_ONE -> {
                // Single entity
                EntityMetadataV2<?> targetMetadata = EntityMetadataV2.inspect(targetType);
                String tableName = getTableName(targetType);
                String columnList = targetMetadata.columnList();

                String sql = "SELECT " + columnList + " FROM " + tableName +
                            " WHERE " + joinColumn + " = ? LIMIT 1";

                yield client.queryOne(sql, stmt -> stmt.setObject(1, foreignKey), rs -> {
                    return targetMetadata.map(rs, client, false);
                }).orElse(null);
            }
            case ONE_TO_MANY -> {
                // Collection of entities
                EntityMetadataV2<?> targetMetadata = EntityMetadataV2.inspect(targetType);
                String tableName = getTableName(targetType);
                String columnList = targetMetadata.columnList();

                String sql = "SELECT " + columnList + " FROM " + tableName +
                            " WHERE " + joinColumn + " = ?";

                yield client.query(sql, stmt -> stmt.setObject(1, foreignKey), rs -> {
                    return targetMetadata.map(rs, client, false);
                });
            }
        };
    }

    private static String getTableName(Class<?> entityType) {
        // Convert class name to table name using snake_case
        String simpleName = entityType.getSimpleName();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < simpleName.length(); i++) {
            char current = simpleName.charAt(i);
            if (Character.isUpperCase(current) && i > 0) {
                builder.append('_');
            }
            builder.append(Character.toLowerCase(current));
        }
        builder.append('s');
        return builder.toString();
    }

    private Object createLazyProxy(T instance, RelationshipMetadata relationship, DatabaseClient client) {
        Object foreignKey = getValue(instance, primaryKey);

        return switch (relationship.getType()) {
            case ONE_TO_ONE, MANY_TO_ONE ->
                    LazyLoadingHandler.createProxy(client, relationship.getTargetEntity(),
                            foreignKey, relationship.getJoinColumn());
            case ONE_TO_MANY ->
                    LazyLoadingHandler.createCollectionProxy(client, relationship.getTargetEntity(),
                            foreignKey, relationship.getJoinColumn());
        };
    }

    boolean hasEmbeddedFields() {
        return !embeddedHandlers.isEmpty();
    }

    boolean hasRelationships() {
        return !relationships.isEmpty();
    }
}
