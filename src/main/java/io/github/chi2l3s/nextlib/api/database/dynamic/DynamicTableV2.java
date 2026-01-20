package io.github.chi2l3s.nextlib.api.database.dynamic;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;
import io.github.chi2l3s.nextlib.api.database.DatabaseException;
import io.github.chi2l3s.nextlib.api.database.SqlConsumer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Enhanced DynamicTable with support for embedded objects and relationships.
 *
 * @since 1.0.8
 */
public final class DynamicTableV2<T> {
    private final DatabaseClient client;
    private final String tableName;
    private final EntityMetadataV2<T> metadata;
    private final String columnList;
    private final String insertSql;

    private DynamicTableV2(DatabaseClient client, String tableName, EntityMetadataV2<T> metadata) {
        this.client = client;
        this.tableName = tableName;
        this.metadata = metadata;
        this.columnList = metadata.columnList();
        this.insertSql = buildInsertSql();
        createTable();
    }

    static <T> DynamicTableV2<T> create(DatabaseClient client, String tableName, Class<T> entityType) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(entityType, "entityType");
        EntityMetadataV2<T> metadata = EntityMetadataV2.inspect(entityType);
        return new DynamicTableV2<>(client, tableName, metadata);
    }

    public Class<T> getEntityType() {
        return metadata.getEntityType();
    }

    public FindOneQuery findFirst() {
        return new FindOneQuery();
    }

    public FindManyQuery findMany() {
        return new FindManyQuery();
    }

    public UpdateBuilder update() {
        return new UpdateBuilder();
    }

    public int create(T entity) {
        Objects.requireNonNull(entity, "entity");

        // Handle cascade PERSIST for relationships
        if (metadata.hasRelationships()) {
            cascadePersist(entity);
        }

        return client.execute(insertSql, statement -> bindEntity(statement, entity));
    }

    public int delete(T entity) {
        Objects.requireNonNull(entity, "entity");

        // Handle cascade REMOVE for relationships
        if (metadata.hasRelationships()) {
            cascadeRemove(entity);
        }

        EntityField pk = metadata.getPrimaryKey();
        Object pkValue = metadata.getValue(entity, pk);

        String sql = "DELETE FROM " + tableName + " WHERE " + pk.getColumnName() + " = ?";
        return client.execute(sql, stmt -> pk.bind(stmt, 1, pkValue));
    }

    private void createTable() {
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE IF NOT EXISTS ").append(tableName).append(" (");
        for (int i = 0; i < metadata.getFields().size(); i++) {
            EntityField field = metadata.getFields().get(i);
            sql.append(field.getColumnName()).append(' ').append(field.getSqlType());
            if (!field.isNullable()) {
                sql.append(" NOT NULL");
            }
            if (field == metadata.getPrimaryKey()) {
                sql.append(" PRIMARY KEY");
            }
            if (i < metadata.getFields().size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(')');
        client.execute(sql.toString(), null);
    }

    private String buildInsertSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(tableName).append('(');
        for (int i = 0; i < metadata.getFields().size(); i++) {
            EntityField field = metadata.getFields().get(i);
            sql.append(field.getColumnName());
            if (i < metadata.getFields().size() - 1) {
                sql.append(',');
                sql.append(' ');
            }
        }
        sql.append(") VALUES (");
        for (int i = 0; i < metadata.getFields().size(); i++) {
            sql.append('?');
            if (i < metadata.getFields().size() - 1) {
                sql.append(',');
                sql.append(' ');
            }
        }
        sql.append(')');
        return sql.toString();
    }

    private void bindEntity(PreparedStatement statement, T entity) throws SQLException {
        int index = 1;

        // Bind regular fields (non-embedded)
        for (EntityField field : metadata.getFields()) {
            boolean isEmbeddedField = false;

            // Check if this field belongs to an embedded object
            for (EmbeddedFieldHandler handler : metadata.getEmbeddedHandlers()) {
                if (handler.getFlattenedFields().contains(field)) {
                    isEmbeddedField = true;
                    break;
                }
            }

            if (!isEmbeddedField) {
                Object value = metadata.getValue(entity, field);
                field.bind(statement, index++, value);
            }
        }

        // Bind embedded fields
        for (EmbeddedFieldHandler handler : metadata.getEmbeddedHandlers()) {
            handler.bindEmbeddedValues(statement, index, entity);
            index += handler.getFlattenedFields().size();
        }
    }

    private T mapRow(ResultSet resultSet) throws SQLException {
        return mapRow(resultSet, false);
    }

    private T mapRow(ResultSet resultSet, boolean loadRelationships) throws SQLException {
        return metadata.map(resultSet, client, loadRelationships);
    }

    private SqlConsumer<PreparedStatement> binder(List<Criterion> criteria) {
        if (criteria.isEmpty()) {
            return null;
        }
        return statement -> {
            int index = 1;
            for (Criterion criterion : criteria) {
                criterion.bind(statement, index);
                index += criterion.getBindCount();
            }
        };
    }

    private String appendWhereClause(StringBuilder builder, List<Criterion> parameters, List<Criterion> criteria) {
        if (criteria.isEmpty()) {
            return builder.toString();
        }
        builder.append(" WHERE ");
        for (int i = 0; i < criteria.size(); i++) {
            Criterion criterion = criteria.get(i);
            criterion.appendCondition(builder, parameters);
            if (i < criteria.size() - 1) {
                builder.append(" AND ");
            }
        }
        return builder.toString();
    }

    private void cascadePersist(T entity) {
        for (RelationshipMetadata relationship : metadata.getRelationships()) {
            if (relationship.shouldCascade(CascadeType.PERSIST) ||
                relationship.shouldCascade(CascadeType.ALL)) {

                Object relatedValue = relationship.getValue(entity);
                if (relatedValue != null) {
                    // In a full implementation, this would recursively persist related entities
                    // For now, we skip to avoid circular dependencies
                    // This would require access to DynamicDatabase registry
                }
            }
        }
    }

    private void cascadeRemove(T entity) {
        for (RelationshipMetadata relationship : metadata.getRelationships()) {
            if (relationship.shouldCascade(CascadeType.REMOVE) ||
                relationship.shouldCascade(CascadeType.ALL)) {

                Object relatedValue = relationship.getValue(entity);
                if (relatedValue != null) {
                    // In a full implementation, this would recursively delete related entities
                    // For now, we skip to avoid circular dependencies
                    // This would require access to DynamicDatabase registry
                }
            }
        }
    }

    public final class FindOneQuery extends AbstractQuery<FindOneQuery> {
        private boolean loadRelationships = false;

        private FindOneQuery() {
            super();
        }

        public FindOneQuery withRelationships() {
            this.loadRelationships = true;
            return this;
        }

        public Optional<T> execute() {
            List<Criterion> parameterCriteria = new ArrayList<>();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(columnList).append(" FROM ").append(tableName);
            appendWhereClause(sql, parameterCriteria, criteria);
            sql.append(" LIMIT 1");
            return client.queryOne(sql.toString(), binder(parameterCriteria), rs -> mapRow(rs, loadRelationships));
        }
    }

    public final class FindManyQuery extends AbstractQuery<FindManyQuery> {
        private boolean loadRelationships = false;

        private FindManyQuery() {
            super();
        }

        public FindManyQuery withRelationships() {
            this.loadRelationships = true;
            return this;
        }

        public List<T> execute() {
            List<Criterion> parameterCriteria = new ArrayList<>();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(columnList).append(" FROM ").append(tableName);
            appendWhereClause(sql, parameterCriteria, criteria);
            return client.query(sql.toString(), binder(parameterCriteria), rs -> mapRow(rs, loadRelationships));
        }
    }

    public final class UpdateBuilder extends AbstractQuery<UpdateBuilder> {
        private final Map<EntityField, Object> updates = new LinkedHashMap<>();

        private UpdateBuilder() {
            super();
        }

        public UpdateBuilder set(String field, Object value) {
            EntityField entityField = metadata.requireField(field);
            updates.put(entityField, value);
            return this;
        }

        public int execute() {
            if (updates.isEmpty()) {
                throw new DatabaseException("No fields specified for update on table '" + tableName + "'");
            }
            List<Criterion> updateCriteria = new ArrayList<>();
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE ").append(tableName).append(" SET ");
            int i = 0;
            for (Map.Entry<EntityField, Object> entry : updates.entrySet()) {
                if (i++ > 0) {
                    sql.append(", ");
                }
                sql.append(entry.getKey().getColumnName()).append(" = ?");
                updateCriteria.add(new Criterion(entry.getKey(), entry.getValue()));
            }
            List<Criterion> whereCriteria = new ArrayList<>();
            appendWhereClause(sql, whereCriteria, criteria);
            SqlConsumer<PreparedStatement> binder = statement -> {
                int index = 1;
                for (Criterion criterion : updateCriteria) {
                    criterion.bind(statement, index++);
                }
                for (Criterion criterion : whereCriteria) {
                    criterion.bind(statement, index++);
                }
            };
            return client.execute(sql.toString(), binder);
        }
    }

    private abstract class AbstractQuery<Q extends AbstractQuery<Q>> {
        protected final List<Criterion> criteria = new ArrayList<>();

        private AbstractQuery() {
        }

        @SuppressWarnings("unchecked")
        public Q where(String field, Object value) {
            return where(field, QueryOperator.EQUALS, value);
        }

        @SuppressWarnings("unchecked")
        public Q where(String field, QueryOperator operator, Object value) {
            EntityField entityField = metadata.requireField(field);
            criteria.add(new Criterion(entityField, operator, value));
            return (Q) this;
        }

        @SuppressWarnings("unchecked")
        public Q whereLike(String field, String pattern) {
            return where(field, QueryOperator.LIKE, pattern);
        }

        @SuppressWarnings("unchecked")
        public Q whereIn(String field, Object... values) {
            EntityField entityField = metadata.requireField(field);
            criteria.add(new Criterion(entityField, QueryOperator.IN, values));
            return (Q) this;
        }

        @SuppressWarnings("unchecked")
        public Q whereBetween(String field, Object min, Object max) {
            EntityField entityField = metadata.requireField(field);
            criteria.add(new Criterion(entityField, QueryOperator.BETWEEN, new Object[]{min, max}));
            return (Q) this;
        }

        @SuppressWarnings("unchecked")
        public Q whereIsNull(String field) {
            EntityField entityField = metadata.requireField(field);
            criteria.add(new Criterion(entityField, QueryOperator.IS_NULL, null));
            return (Q) this;
        }

        @SuppressWarnings("unchecked")
        public Q whereIsNotNull(String field) {
            EntityField entityField = metadata.requireField(field);
            criteria.add(new Criterion(entityField, QueryOperator.IS_NOT_NULL, null));
            return (Q) this;
        }
    }

    private static final class Criterion {
        private final EntityField field;
        private final QueryOperator operator;
        private final Object value;

        private Criterion(EntityField field, Object value) {
            this(field, QueryOperator.EQUALS, value);
        }

        private Criterion(EntityField field, QueryOperator operator, Object value) {
            this.field = field;
            this.operator = operator;
            this.value = value;
        }

        private void appendCondition(StringBuilder builder, List<Criterion> parameters) {
            builder.append(field.getColumnName());

            switch (operator) {
                case IS_NULL:
                case IS_NOT_NULL:
                    builder.append(' ').append(operator.getSql());
                    break;

                case IN:
                case NOT_IN:
                    builder.append(' ').append(operator.getSql()).append(" (");
                    Object[] values = (Object[]) value;
                    for (int i = 0; i < values.length; i++) {
                        builder.append('?');
                        if (i < values.length - 1) {
                            builder.append(", ");
                        }
                    }
                    builder.append(')');
                    parameters.add(this);
                    break;

                case BETWEEN:
                    builder.append(' ').append(operator.getSql()).append(" ? AND ?");
                    parameters.add(this);
                    break;

                default:
                    if (value == null) {
                        builder.append(" IS NULL");
                    } else {
                        builder.append(' ').append(operator.getSql()).append(" ?");
                        parameters.add(this);
                    }
                    break;
            }
        }

        private void bind(PreparedStatement statement, int index) throws SQLException {
            if (operator == QueryOperator.IN || operator == QueryOperator.NOT_IN) {
                Object[] values = (Object[]) value;
                for (int i = 0; i < values.length; i++) {
                    field.bind(statement, index + i, values[i]);
                }
            } else if (operator == QueryOperator.BETWEEN) {
                Object[] values = (Object[]) value;
                field.bind(statement, index, values[0]);
                field.bind(statement, index + 1, values[1]);
            } else {
                field.bind(statement, index, value);
            }
        }

        private int getBindCount() {
            if (operator == QueryOperator.IN || operator == QueryOperator.NOT_IN) {
                return ((Object[]) value).length;
            } else if (operator == QueryOperator.BETWEEN) {
                return 2;
            } else if (operator == QueryOperator.IS_NULL || operator == QueryOperator.IS_NOT_NULL) {
                return 0;
            }
            return value == null ? 0 : 1;
        }
    }
}
