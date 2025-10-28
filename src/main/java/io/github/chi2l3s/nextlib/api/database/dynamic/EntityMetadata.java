package io.github.chi2l3s.nextlib.api.database.dynamic;

import io.github.chi2l3s.nextlib.api.database.DatabaseException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class EntityMetadata<T> {
    private final Class<T> entityType;
    private final Constructor<T> constructor;
    private final List<EntityField> fields;
    private final Map<String, EntityField> byName;
    private final EntityField primaryKey;
    private final String columnList;

    private EntityMetadata(Class<T> entityType,
                           Constructor<T> constructor,
                           List<EntityField> fields,
                           EntityField primaryKey) {
        this.entityType = entityType;
        this.constructor = constructor;
        this.fields = fields;
        this.primaryKey = primaryKey;
        this.byName = new LinkedHashMap<>();
        for (EntityField field : fields) {
            this.byName.put(field.getFieldName(), field);
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                builder.append(',').append(' ');
            }
            builder.append(fields.get(i).getColumnName());
        }
        this.columnList = builder.toString();
    }

    static <T> EntityMetadata<T> inspect(Class<T> type) {
        List<Field> declaredFields = collectInstanceFields(type);
        if (declaredFields.isEmpty()) {
            throw new DatabaseException("Entity " + type.getName() + " does not declare any fields");
        }
        Constructor<T> constructor = resolveConstructor(type, declaredFields);
        List<EntityField> entityFields = new ArrayList<>();
        EntityField primaryKey = null;
        for (int i = 0; i < declaredFields.size(); i++) {
            Field field = declaredFields.get(i);
            EntityField entityField = EntityField.from(field);
            entityFields.add(entityField);
            if (primaryKey == null || entityField.isPrimaryKey()) {
                primaryKey = entityField;
            }
        }
        if (primaryKey == null) {
            primaryKey = entityFields.get(0);
        }
        return new EntityMetadata<>(type, constructor, entityFields, primaryKey);
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
            throw new DatabaseException("Failed to resolve constructor for entity " + type.getName(), exception);
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
        return fields;
    }

    EntityField getPrimaryKey() {
        return primaryKey;
    }

    String columnList() {
        return columnList;
    }

    EntityField requireField(String name) {
        EntityField field = byName.get(name);
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

    T map(ResultSet resultSet) {
        Object[] values = new Object[fields.size()];
        try {
            for (int i = 0; i < fields.size(); i++) {
                values[i] = fields.get(i).read(resultSet);
            }
            return constructor.newInstance(values);
        } catch (ReflectiveOperationException | SQLException exception) {
            throw new DatabaseException("Failed to map result set for entity " + entityType.getName(), exception);
        }
    }
}
