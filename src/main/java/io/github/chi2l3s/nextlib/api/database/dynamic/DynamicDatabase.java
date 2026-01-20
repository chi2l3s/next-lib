package io.github.chi2l3s.nextlib.api.database.dynamic;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;
import io.github.chi2l3s.nextlib.api.database.DatabaseException;
import io.github.chi2l3s.nextlib.api.database.DatabaseManager;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Runtime database registry that inspects user-defined entity classes and creates tables for them on demand.
 * <p>
 * This class provides an ORM-like experience by automatically mapping Java classes to database tables.
 * Entity classes are introspected using reflection, and tables are created with appropriate columns
 * based on field types.
 * </p>
 *
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * @AllArgsConstructor
 * @Getter
 * public class PlayerEntity {
 *     @PrimaryKey
 *     private final UUID playerId;
 *     private final String nickname;
 *     private final Integer coins;
 * }
 *
 * DatabaseClient client = manager.getDefault();
 * DynamicDatabase database = new DynamicDatabase(client);
 *
 * // Register entity (creates table if not exists)
 * DynamicTable<PlayerEntity> players = database.register(PlayerEntity.class);
 *
 * // Create record
 * players.create(new PlayerEntity(uuid, "John", 1000));
 *
 * // Query
 * Optional<PlayerEntity> player = players.findFirst()
 *     .where("playerId", uuid)
 *     .execute();
 * }</pre>
 *
 * @see DynamicTable
 * @see PrimaryKey
 * @see DatabaseClient
 * @since 1.0.0
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

    /**
     * Registers an entity class and creates its table using auto-generated table name.
     * <p>
     * The table name is generated from the class name in snake_case with an 's' suffix.
     * For example, {@code PlayerEntity} becomes {@code player_entitys}.
     * </p>
     *
     * @param <T>        entity type
     * @param entityType the entity class to register (not null)
     * @return a DynamicTable for performing CRUD operations
     * @throws EntityMappingException if the entity class cannot be introspected
     * @throws NullPointerException   if entityType is null
     */
    public <T> DynamicTable<T> register(Class<T> entityType) {
        Objects.requireNonNull(entityType, "entityType");
        return register(defaultTableName(entityType), entityType);
    }

    /**
     * Registers an entity class with a custom table name.
     * <p>
     * If the table doesn't exist, it will be created automatically.
     * If a table with the same name is already registered, the existing table is returned.
     * </p>
     *
     * @param <T>        entity type
     * @param tableName  custom table name (not null)
     * @param entityType the entity class to register (not null)
     * @return a DynamicTable for performing CRUD operations
     * @throws EntityMappingException if the entity class cannot be introspected
     * @throws NullPointerException   if tableName or entityType is null
     */
    public <T> DynamicTable<T> register(String tableName, Class<T> entityType) {
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(entityType, "entityType");
        return (DynamicTable<T>) tables.computeIfAbsent(tableName, name ->
                DynamicTable.create(client, name, entityType));
    }

    /**
     * Retrieves a registered table by name.
     *
     * @param tableName the table name
     * @return the DynamicTable
     * @throws DatabaseException    if no table with the given name is registered
     * @throws NullPointerException if tableName is null
     */
    public DynamicTable<?> get(String tableName) {
        Objects.requireNonNull(tableName, "tableName");
        DynamicTable<?> table = tables.get(tableName);
        if (table == null) {
            throw new DatabaseException("No table registered with name '" + tableName + "'");
        }
        return table;
    }

    /**
     * Retrieves a registered table by name with type checking.
     *
     * @param <T>       entity type
     * @param tableName the table name
     * @param type      expected entity type for verification
     * @return the DynamicTable with the specified type
     * @throws DatabaseException    if the table doesn't exist or type doesn't match
     * @throws NullPointerException if tableName or type is null
     */
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