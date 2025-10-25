package io.github.chi2l3s.nextlib.api.database.schema;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Setter
public final class TableDefinition {
    @Getter
    private String name;
    private String tableName;
    @Getter
    private List<FieldDefinition> fields = new ArrayList<>();

    public String getTableName() {
        return tableName != null ? tableName : name.toLowerCase();
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