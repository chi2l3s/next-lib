package io.github.chi2l3s.nextlib.api.database.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Setter
@Getter
public final class FieldDefinition {
    private String name;
    private FieldType type;
    private boolean id;
    private boolean unique;
    private boolean nullable;
    private String columnName;
    private String defaultValue;

    public void validate() {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
    }
}