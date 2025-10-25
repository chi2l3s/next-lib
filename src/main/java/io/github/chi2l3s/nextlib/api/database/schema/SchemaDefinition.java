package io.github.chi2l3s.nextlib.api.database.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SchemaDefinition {
    private String packageName;
    private String datasource;
    private List<TableDefinition> tables = new ArrayList<>();

    public String getPackageName() {
        return packageName;
    }

    public String getDatasource() {
        return datasource;
    }

    public List<TableDefinition> getTables() {
        return tables;
    }

    public void validate() {
        Objects.requireNonNull(packageName, "packageName");
        Objects.requireNonNull(datasource, "datasource");
        if (tables == null || tables.isEmpty()) {
            throw new IllegalStateException("Schema must declare at least one table");
        }
        for (TableDefinition table : tables) {
            table.validate();
        }
    }
}