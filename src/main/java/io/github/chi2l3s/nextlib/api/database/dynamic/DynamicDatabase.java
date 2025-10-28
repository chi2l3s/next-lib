package io.github.chi2l3s.nextlib.api.database.dynamic;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;
import io.github.chi2l3s.nextlib.api.database.DatabaseException;
import io.github.chi2l3s.nextlib.api.database.DatabaseManager;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime database registry that inspects user defined entity classes and creates tables for them on demand.
 */
public final class DynamicDatabase {
    private final DatabaseClient client;
    private final Map<String, DynamicTable<?>> tables = new ConcurrentHashMap<>();

    public DynamicDatabase(DatabaseClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public static DynamicDatabase using(DatabaseManager manager) {
        Objects.requireNonNull(manager, "manager");
        return new DynamicDatabase(manager.getDefault());
    }

    public <T> DynamicTable<T> register(Class<T> entityType) {
        Objects.requireNonNull(entityType, "entityType");
        return register(defaultTableName(entityType), entityType);
    }

    public <T> DynamicTable<T> register(String tableName, Class<T> entityType) {
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(entityType, "entityType");
        return (DynamicTable<T>) tables.computeIfAbsent(tableName, name ->
                DynamicTable.create(client, name, entityType));
    }

    public DynamicTable<?> get(String tableName) {
        Objects.requireNonNull(tableName, "tableName");
        DynamicTable<?> table = tables.get(tableName);
        if (table == null) {
            throw new DatabaseException("No table registered with name '" + tableName + "'");
        }
        return table;
    }

    public <T> DynamicTable<T> get(String tableName, Class<T> type) {
        Objects.requireNonNull(type, "type");
        DynamicTable<?> table = get(tableName);
        if (!type.equals(table.getEntityType())) {
            throw new DatabaseException("Table '" + tableName + "' is registered with entity type "
                    + table.getEntityType().getName() + " but " + type.getName() + " was requested");
        }
        return (DynamicTable<T>) table;
    }

    private static String defaultTableName(Class<?> entityType) {
        String simpleName = entityType.getSimpleName();
        if (simpleName.isEmpty()) {
            throw new DatabaseException("Cannot determine table name for anonymous class " + entityType);
        }
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
}
