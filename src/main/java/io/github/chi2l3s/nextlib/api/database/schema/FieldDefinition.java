package io.github.chi2l3s.nextlib.api.database.schema;

import java.util.Objects;

public final class FieldDefinition {
    private String name;
    private FieldType type;
    private boolean id;
    private boolean unique;
    private boolean nullable;
    private String columnName;
    private String defaultValue;

    public String getName() {
        return name;
    }

    public FieldType getType() {
        return type;
    }

    public boolean isId() {
        return id;
    }

    public boolean isUnique() {
        return unique;
    }

    public boolean isNullable() {
        return nullable;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void validate() {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }
}
