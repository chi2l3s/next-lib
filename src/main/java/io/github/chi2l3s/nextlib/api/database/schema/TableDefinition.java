package io.github.chi2l3s.nextlib.api.database.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class TableDefinition {
    private String name;
    private String tableName;
    private List<FieldDefinition> fields = new ArrayList<>();

    public String getName() {
        return name;
    }

    public String getTableName() {
        return tableName != null ? tableName : name.toLowerCase();
    }

    public List<FieldDefinition> getFields() {
        return fields;
    }

    public void validate() {
        Objects.requireNonNull(name, "name");
        if (fields == null || fields.isEmpty()) {
            throw new IllegalStateException("Table '" + name + "' must declare at least one field");
        }
        boolean hasPrimaryKey = false;
        for (FieldDefinition field : fields) {
            field.validate();
            if (field.isId()) {
                if (hasPrimaryKey) {
                    throw new IllegalStateException("Table '" + name + "' declares multiple primary keys");
                }
                hasPrimaryKey = true;
            }
        }
        if (!hasPrimaryKey) {
            throw new IllegalStateException("Table '" + name + "' must declare a primary key field");
        }
    }

    public FieldDefinition getPrimaryKey() {
        return fields.stream()
                .filter(FieldDefinition::isId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Table '" + name + "' missing primary key"));
    }
}