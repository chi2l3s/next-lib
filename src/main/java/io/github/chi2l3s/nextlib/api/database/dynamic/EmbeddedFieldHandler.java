package io.github.chi2l3s.nextlib.api.database.dynamic;

import io.github.chi2l3s.nextlib.api.database.DatabaseException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles flattening and unflattening of embedded objects.
 *
 * @since 1.0.7
 */
final class EmbeddedFieldHandler {
    private final Field embeddedField;
    private final String prefix;
    private final Class<?> embeddedType;
    private final Constructor<?> constructor;
    private final List<EntityField> flattenedFields;

    private EmbeddedFieldHandler(Field embeddedField, String prefix, Class<?> embeddedType,
                                 Constructor<?> constructor, List<EntityField> flattenedFields) {
        this.embeddedField = embeddedField;
        this.prefix = prefix;
        this.embeddedType = embeddedType;
        this.constructor = constructor;
        this.flattenedFields = flattenedFields;
    }

    static EmbeddedFieldHandler from(Field field) {
        Embedded embedded = field.getAnnotation(Embedded.class);
        String prefix = embedded.prefix().isEmpty()
                ? field.getName() + "_"
                : embedded.prefix() + "_";

        Class<?> embeddedType = field.getType();
        List<Field> embeddedFields = collectFields(embeddedType);

        if (embeddedFields.isEmpty()) {
            throw new DatabaseException("Embedded type " + embeddedType.getName() + " has no fields");
        }

        List<EntityField> flattenedFields = new ArrayList<>();
        for (Field embeddedField : embeddedFields) {
            String columnName = prefix + embeddedField.getName();
            flattenedFields.add(createFlattenedField(embeddedField, columnName));
        }

        Constructor<?> constructor = resolveConstructor(embeddedType, embeddedFields);

        if (!field.canAccess(null)) {
            field.setAccessible(true);
        }

        return new EmbeddedFieldHandler(field, prefix, embeddedType, constructor, flattenedFields);
    }

    private static EntityField createFlattenedField(Field field, String columnName) {
        // Use reflection to create EntityField with custom column name
        try {
            java.lang.reflect.Constructor<EntityField> constructor =
                EntityField.class.getDeclaredConstructor(Field.class, String.class, boolean.class);
            constructor.setAccessible(true);
            return constructor.newInstance(field, columnName, false);
        } catch (ReflectiveOperationException e) {
            throw new DatabaseException("Failed to create flattened field", e);
        }
    }

    private static List<Field> collectFields(Class<?> type) {
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

    private static Constructor<?> resolveConstructor(Class<?> type, List<Field> fields) {
        List<Class<?>> parameterTypes = new ArrayList<>();
        for (Field field : fields) {
            parameterTypes.add(field.getType());
        }
        try {
            Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes.toArray(new Class<?>[0]));
            if (!constructor.canAccess(null)) {
                constructor.setAccessible(true);
            }
            return constructor;
        } catch (ReflectiveOperationException e) {
            throw new DatabaseException("Failed to resolve constructor for embedded type " + type.getName(), e);
        }
    }

    List<EntityField> getFlattenedFields() {
        return flattenedFields;
    }

    String getPrefix() {
        return prefix;
    }

    Object extractEmbedded(Object parentInstance) {
        try {
            return embeddedField.get(parentInstance);
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Failed to extract embedded field " + embeddedField.getName(), e);
        }
    }

    void setEmbedded(Object parentInstance, Object embeddedInstance) {
        try {
            embeddedField.set(parentInstance, embeddedInstance);
        } catch (IllegalAccessException e) {
            throw new DatabaseException("Failed to set embedded field " + embeddedField.getName(), e);
        }
    }

    Object reconstructEmbedded(ResultSet resultSet) throws SQLException {
        Object[] values = new Object[flattenedFields.size()];
        for (int i = 0; i < flattenedFields.size(); i++) {
            values[i] = flattenedFields.get(i).read(resultSet);
        }
        try {
            return constructor.newInstance(values);
        } catch (ReflectiveOperationException e) {
            throw new DatabaseException("Failed to reconstruct embedded object of type " + embeddedType.getName(), e);
        }
    }

    void bindEmbeddedValues(PreparedStatement statement, int startIndex, Object parentInstance) throws SQLException {
        Object embedded = extractEmbedded(parentInstance);
        if (embedded == null) {
            // Bind all null values
            for (int i = 0; i < flattenedFields.size(); i++) {
                flattenedFields.get(i).bind(statement, startIndex + i, null);
            }
            return;
        }

        for (int i = 0; i < flattenedFields.size(); i++) {
            EntityField field = flattenedFields.get(i);
            try {
                Object value = field.getValue(embedded);
                field.bind(statement, startIndex + i, value);
            } catch (IllegalAccessException e) {
                throw new DatabaseException("Failed to bind embedded field value", e);
            }
        }
    }

    boolean isEmbedded() {
        return true;
    }

    String getFieldName() {
        return embeddedField.getName();
    }
}
