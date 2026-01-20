package io.github.chi2l3s.nextlib.api.database.dynamic;

import io.github.chi2l3s.nextlib.api.database.DatabaseException;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.UUID;

final class EntityField {
    private final Field field;
    private final String fieldName;
    private final String columnName;
    private final Class<?> type;
    private final boolean primaryKey;

    // Package-private constructor for internal use
    EntityField(Field field, String columnName, boolean primaryKey) {
        this.field = field;
        this.fieldName = field.getName();
        this.columnName = columnName;
        this.type = field.getType();
        this.primaryKey = primaryKey;
    }

    static EntityField from(Field field) {
        String column = field.getName();
        boolean primaryKey = field.isAnnotationPresent(PrimaryKey.class);
        return new EntityField(field, column, primaryKey);
    }

    /**
     * Creates an EntityField with custom column name for embedded fields.
     */
    static EntityField forEmbedded(Field field, String columnName) {
        return new EntityField(field, columnName, false);
    }

    String getFieldName() {
        return fieldName;
    }

    String getColumnName() {
        return columnName;
    }

    String getSqlType() {
        if (type == String.class || type == UUID.class) {
            return "TEXT";
        }
        if (type == int.class || type == Integer.class || type == short.class || type == Short.class) {
            return "INTEGER";
        }
        if (type == long.class || type == Long.class) {
            return "BIGINT";
        }
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            return "DOUBLE";
        }
        if (type == boolean.class || type == Boolean.class) {
            return "BOOLEAN";
        }
        if (type == Instant.class) {
            return "TIMESTAMP";
        }
        throw new DatabaseException("Unsupported field type " + type.getName() + " for column '" + columnName + "'");
    }

    boolean isNullable() {
        return !type.isPrimitive();
    }

    boolean isPrimaryKey() {
        return primaryKey;
    }

    Object getValue(Object instance) throws IllegalAccessException {
        return field.get(instance);
    }

    Object read(ResultSet resultSet) throws SQLException {
        if (type == String.class) {
            return resultSet.getString(columnName);
        }
        if (type == UUID.class) {
            String value = resultSet.getString(columnName);
            return value != null ? UUID.fromString(value) : null;
        }
        if (type == int.class) {
            return resultSet.getInt(columnName);
        }
        if (type == Integer.class) {
            int value = resultSet.getInt(columnName);
            return resultSet.wasNull() ? null : value;
        }
        if (type == short.class) {
            return resultSet.getShort(columnName);
        }
        if (type == Short.class) {
            short value = resultSet.getShort(columnName);
            return resultSet.wasNull() ? null : value;
        }
        if (type == long.class) {
            return resultSet.getLong(columnName);
        }
        if (type == Long.class) {
            long value = resultSet.getLong(columnName);
            return resultSet.wasNull() ? null : value;
        }
        if (type == double.class) {
            return resultSet.getDouble(columnName);
        }
        if (type == Double.class) {
            double value = resultSet.getDouble(columnName);
            return resultSet.wasNull() ? null : value;
        }
        if (type == float.class) {
            return resultSet.getFloat(columnName);
        }
        if (type == Float.class) {
            float value = resultSet.getFloat(columnName);
            return resultSet.wasNull() ? null : value;
        }
        if (type == boolean.class) {
            return resultSet.getBoolean(columnName);
        }
        if (type == Boolean.class) {
            boolean value = resultSet.getBoolean(columnName);
            return resultSet.wasNull() ? null : value;
        }
        if (type == Instant.class) {
            Timestamp timestamp = resultSet.getTimestamp(columnName);
            return timestamp != null ? timestamp.toInstant() : null;
        }
        throw new DatabaseException("Unsupported field type " + type.getName() + " for column '" + columnName + "'");
    }

    void bind(PreparedStatement statement, int index, Object value) throws SQLException {
        if (value == null) {
            statement.setNull(index, sqlTypeForNull());
            return;
        }
        if (type == String.class) {
            statement.setString(index, value.toString());
            return;
        }
        if (type == UUID.class) {
            statement.setString(index, ((UUID) value).toString());
            return;
        }
        if (type == int.class || type == Integer.class) {
            statement.setInt(index, ((Number) value).intValue());
            return;
        }
        if (type == short.class || type == Short.class) {
            statement.setShort(index, ((Number) value).shortValue());
            return;
        }
        if (type == long.class || type == Long.class) {
            statement.setLong(index, ((Number) value).longValue());
            return;
        }
        if (type == double.class || type == Double.class) {
            statement.setDouble(index, ((Number) value).doubleValue());
            return;
        }
        if (type == float.class || type == Float.class) {
            statement.setFloat(index, ((Number) value).floatValue());
            return;
        }
        if (type == boolean.class || type == Boolean.class) {
            statement.setBoolean(index, (Boolean) value);
            return;
        }
        if (type == Instant.class) {
            if (value instanceof Instant instant) {
                statement.setTimestamp(index, Timestamp.from(instant));
            } else {
                throw new DatabaseException("Expected Instant for column '" + columnName + "' but received "
                        + value.getClass().getName());
            }
            return;
        }
        throw new DatabaseException("Unsupported field type " + type.getName() + " for column '" + columnName + "'");
    }

    private int sqlTypeForNull() {
        if (type == String.class || type == UUID.class) {
            return Types.VARCHAR;
        }
        if (type == int.class || type == Integer.class || type == short.class || type == Short.class) {
            return Types.INTEGER;
        }
        if (type == long.class || type == Long.class) {
            return Types.BIGINT;
        }
        if (type == double.class || type == Double.class || type == float.class || type == Float.class) {
            return Types.DOUBLE;
        }
        if (type == boolean.class || type == Boolean.class) {
            return Types.BOOLEAN;
        }
        if (type == Instant.class) {
            return Types.TIMESTAMP;
        }
        throw new DatabaseException("Unsupported field type " + type.getName() + " for null binding");
    }
}