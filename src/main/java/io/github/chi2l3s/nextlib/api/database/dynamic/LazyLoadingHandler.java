package io.github.chi2l3s.nextlib.api.database.dynamic;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;
import io.github.chi2l3s.nextlib.api.database.DatabaseException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates lazy-loading proxies for entity relationships.
 * Delays database queries until the relationship is actually accessed.
 *
 * @since 1.0.7
 */
final class LazyLoadingHandler<T> implements InvocationHandler {
    private final DatabaseClient client;
    private final Class<T> entityType;
    private final Object foreignKey;
    private final String joinColumn;
    private T realEntity;
    private boolean loaded = false;

    private LazyLoadingHandler(DatabaseClient client, Class<T> entityType, Object foreignKey, String joinColumn) {
        this.client = client;
        this.entityType = entityType;
        this.foreignKey = foreignKey;
        this.joinColumn = joinColumn;
    }

    /**
     * Creates a lazy-loading proxy for a single entity.
     */
    @SuppressWarnings("unchecked")
    static <T> T createProxy(DatabaseClient client, Class<T> entityType, Object foreignKey, String joinColumn) {
        LazyLoadingHandler<T> handler = new LazyLoadingHandler<>(client, entityType, foreignKey, joinColumn);
        return (T) Proxy.newProxyInstance(
                entityType.getClassLoader(),
                new Class<?>[]{entityType, LazyLoadable.class},
                handler
        );
    }

    /**
     * Creates a lazy-loading proxy for a collection of entities.
     */
    @SuppressWarnings("unchecked")
    static <T> List<T> createCollectionProxy(DatabaseClient client, Class<T> entityType,
                                              Object foreignKey, String joinColumn) {
        return (List<T>) Proxy.newProxyInstance(
                List.class.getClassLoader(),
                new Class<?>[]{List.class, LazyLoadable.class},
                new LazyCollectionHandler<>(client, entityType, foreignKey, joinColumn)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Handle LazyLoadable interface methods
        if (method.getDeclaringClass() == LazyLoadable.class) {
            if (method.getName().equals("isLoaded")) {
                return loaded;
            }
            if (method.getName().equals("forceLoad")) {
                loadEntity();
                return null;
            }
        }

        // Load entity if not already loaded
        if (!loaded) {
            loadEntity();
        }

        // Delegate to real entity
        return method.invoke(realEntity, args);
    }

    private void loadEntity() {
        if (loaded) {
            return;
        }

        // Use EntityMetadataV2 to load and map the entity
        EntityMetadataV2<T> metadata = EntityMetadataV2.inspect(entityType);
        String tableName = getTableName();
        String columnList = metadata.columnList();

        String sql = "SELECT " + columnList + " FROM " + tableName + " WHERE " + joinColumn + " = ?";

        List<T> results = client.query(sql, stmt -> stmt.setObject(1, foreignKey), rs -> {
            return metadata.map(rs, client, false);
        });

        if (results.isEmpty()) {
            throw new DatabaseException("Lazy loading failed: entity not found with " + joinColumn + " = " + foreignKey);
        }

        realEntity = results.get(0);
        loaded = true;
    }

    private String getTableName() {
        // Convert class name to table name (simplified)
        String className = entityType.getSimpleName();
        return className.toLowerCase() + "s";
    }

    /**
     * Marker interface for lazy-loadable entities.
     */
    public interface LazyLoadable {
        boolean isLoaded();
        void forceLoad();
    }

    /**
     * Handler for lazy-loading collections.
     */
    private static class LazyCollectionHandler<T> implements InvocationHandler {
        private final DatabaseClient client;
        private final Class<T> entityType;
        private final Object foreignKey;
        private final String joinColumn;
        private List<T> realCollection;
        private boolean loaded = false;

        LazyCollectionHandler(DatabaseClient client, Class<T> entityType, Object foreignKey, String joinColumn) {
            this.client = client;
            this.entityType = entityType;
            this.foreignKey = foreignKey;
            this.joinColumn = joinColumn;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // Handle LazyLoadable interface methods
            if (method.getDeclaringClass() == LazyLoadable.class) {
                if (method.getName().equals("isLoaded")) {
                    return loaded;
                }
                if (method.getName().equals("forceLoad")) {
                    loadCollection();
                    return null;
                }
            }

            // Load collection if not already loaded
            if (!loaded) {
                loadCollection();
            }

            // Delegate to real collection
            return method.invoke(realCollection, args);
        }

        private void loadCollection() {
            if (loaded) {
                return;
            }

            // Use EntityMetadataV2 to load and map the collection
            EntityMetadataV2<T> metadata = EntityMetadataV2.inspect(entityType);
            String tableName = entityType.getSimpleName().toLowerCase() + "s";
            String columnList = metadata.columnList();

            String sql = "SELECT " + columnList + " FROM " + tableName + " WHERE " + joinColumn + " = ?";

            realCollection = client.query(sql, stmt -> stmt.setObject(1, foreignKey), rs -> {
                return metadata.map(rs, client, false);
            });

            if (realCollection == null) {
                realCollection = new ArrayList<>();
            }

            loaded = true;
        }
    }
}
