package io.github.chi2l3s.nextlib.api.database;

/**
 * Exception thrown when entity mapping or introspection fails.
 */
public class EntityMappingException extends DatabaseException {

    private final Class<?> entityType;

    public EntityMappingException(Class<?> entityType, String message) {
        super("Failed to map entity " + entityType.getName() + ": " + message);
        this.entityType = entityType;
    }

    public EntityMappingException(Class<?> entityType, String message, Throwable cause) {
        super("Failed to map entity " + entityType.getName() + ": " + message, cause);
        this.entityType = entityType;
    }

    public EntityMappingException(Class<?> entityType, Throwable cause) {
        super("Failed to map entity " + entityType.getName(), cause);
        this.entityType = entityType;
    }

    public Class<?> getEntityType() {
        return entityType;
    }
}
