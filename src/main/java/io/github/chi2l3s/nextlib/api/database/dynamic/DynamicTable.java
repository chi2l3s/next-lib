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

public final class DynamicTable<T> {
    private final DatabaseClient client;
    private final String tableName;
    private final EntityMetadata<T> metadata;
    private final String columnList;
    private final String insertSql;

    private DynamicTable(DatabaseClient client, String tableName, EntityMetadata<T> metadata) {
        this.client = client;
        this.tableName = tableName;
        this.metadata = metadata;
        this.columnList = metadata.columnList();
        this.insertSql = buildInsertSql();
        createTable();
    }

    static <T> DynamicTable<T> create(DatabaseClient client, String tableName, Class<T> entityType) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(entityType, "entityType");
        EntityMetadata<T> metadata = EntityMetadata.inspect(entityType);
        return new DynamicTable<>(client, tableName, metadata);
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
        return client.execute(insertSql, statement -> bindEntity(statement, entity));
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
        for (int i = 0; i < metadata.getFields().size(); i++) {
            EntityField field = metadata.getFields().get(i);
            Object value = metadata.getValue(entity, field);
            field.bind(statement, i + 1, value);
        }
    }

    private T mapRow(ResultSet resultSet) throws SQLException {
        return metadata.map(resultSet);
    }

    private SqlConsumer<PreparedStatement> binder(List<Criterion> criteria) {
        if (criteria.isEmpty()) {
            return null;
        }
        return statement -> {
            int index = 1;
            for (Criterion criterion : criteria) {
                criterion.bind(statement, index++);
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

    public final class FindOneQuery extends AbstractQuery<FindOneQuery> {
        private FindOneQuery() {
            super();
        }

        public Optional<T> execute() {
            List<Criterion> parameterCriteria = new ArrayList<>();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(columnList).append(" FROM ").append(tableName);
            appendWhereClause(sql, parameterCriteria, criteria);
            sql.append(" LIMIT 1");
            return client.queryOne(sql.toString(), binder(parameterCriteria), DynamicTable.this::mapRow);
        }
    }

    public final class FindManyQuery extends AbstractQuery<FindManyQuery> {
        private FindManyQuery() {
            super();
        }

        public List<T> execute() {
            List<Criterion> parameterCriteria = new ArrayList<>();
            StringBuilder sql = new StringBuilder();
            sql.append("SELECT ").append(columnList).append(" FROM ").append(tableName);
            appendWhereClause(sql, parameterCriteria, criteria);
            return client.query(sql.toString(), binder(parameterCriteria), DynamicTable.this::mapRow);
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
            EntityField entityField = metadata.requireField(field);
            criteria.add(new Criterion(entityField, value));
            return (Q) this;
        }
    }

    private static final class Criterion {
        private final EntityField field;
        private final Object value;

        private Criterion(EntityField field, Object value) {
            this.field = field;
            this.value = value;
        }

        private void appendCondition(StringBuilder builder, List<Criterion> parameters) {
            builder.append(field.getColumnName());
            if (value == null) {
                builder.append(" IS NULL");
            } else {
                builder.append(" = ?");
                parameters.add(this);
            }
        }

        private void bind(PreparedStatement statement, int index) throws SQLException {
            field.bind(statement, index, value);
        }
    }
}