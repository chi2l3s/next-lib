package io.github.chi2l3s.nextlib.api.database.schema;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;
import io.github.chi2l3s.nextlib.api.database.DatabaseManager;
import io.github.chi2l3s.nextlib.api.database.DatabaseException;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public final class SchemaGenerator {
    private final SchemaParser parser = new SchemaParser();

    public void generate(Path schemaPath, Path outputDirectory) {
        SchemaDefinition schemaDefinition = parser.parse(schemaPath);
        generate(schemaDefinition, outputDirectory);
    }

    public void generate(SchemaDefinition schema, Path outputDirectory) {
        try {
            Files.createDirectories(outputDirectory);
            for (TableDefinition table : schema.getTables()) {
                TypeSpec recordClass = buildRecordClass(schema, table);
                JavaFile.builder(schema.getPackageName(), recordClass)
                        .build()
                        .writeTo(outputDirectory);

                TypeSpec repositoryClass = buildRepositoryClass(schema, table);
                JavaFile.builder(schema.getPackageName(), repositoryClass)
                        .build()
                        .writeTo(outputDirectory);
            }
        } catch (IOException exception) {
            throw new SchemaParseException("Failed to write generated sources", exception);
        }
    }

    private TypeSpec buildRecordClass(SchemaDefinition schema, TableDefinition table) {
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(table.getName() + "Record")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC);

        for (FieldDefinition field : table.getFields()) {
            String fieldName = field.getName();
            TypeName typeName = field.getType().getJavaType(true);
            typeBuilder.addField(FieldSpec.builder(typeName, fieldName, Modifier.PRIVATE, Modifier.FINAL).build());
            ParameterSpec parameter = ParameterSpec.builder(typeName, fieldName).build();
            constructorBuilder.addParameter(parameter);
            if (!field.isNullable()) {
                constructorBuilder.addStatement("this.$N = $T.requireNonNull($N, \"$N\")",
                        fieldName,
                        Objects.class,
                        fieldName,
                        fieldName);
            } else {
                constructorBuilder.addStatement("this.$N = $N", fieldName, fieldName);
            }
            typeBuilder.addMethod(MethodSpec.methodBuilder("get" + capitalize(fieldName))
                    .addModifiers(Modifier.PUBLIC)
                    .returns(typeName)
                    .addStatement("return this.$N", fieldName)
                    .build());
        }
        typeBuilder.addMethod(constructorBuilder.build());

        MethodSpec.Builder factoryMethod = MethodSpec.methodBuilder("fromResultSet")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(schema.getPackageName(), table.getName() + "Record"))
                .addParameter(ResultSet.class, "resultSet")
                .addException(SQLException.class);
        List<CodeBlock> fieldBlocks = new ArrayList<>();
        for (FieldDefinition field : table.getFields()) {
            String column = columnName(field);
            String fieldVar = field.getName();
            fieldBlocks.add(readFieldFromResultSet(field, column, fieldVar));
        }
        for (CodeBlock block : fieldBlocks) {
            factoryMethod.addCode(block);
        }
        CodeBlock.Builder constructorArguments = CodeBlock.builder();
        for (int i = 0; i < table.getFields().size(); i++) {
            FieldDefinition field = table.getFields().get(i);
            constructorArguments.add("$N", field.getName());
            if (i < table.getFields().size() - 1) {
                constructorArguments.add(", ");
            }
        }
        factoryMethod.addStatement("return new $T($L)",
                ClassName.get(schema.getPackageName(), table.getName() + "Record"),
                constructorArguments.build());
        typeBuilder.addMethod(factoryMethod.build());

        return typeBuilder.build();
    }

    private TypeSpec buildRepositoryClass(SchemaDefinition schema, TableDefinition table) {
        ClassName recordType = ClassName.get(schema.getPackageName(), table.getName() + "Record");
        TypeSpec.Builder typeBuilder = TypeSpec.classBuilder(table.getName() + "Repository")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        FieldSpec clientField = FieldSpec.builder(DatabaseClient.class, "client", Modifier.PRIVATE, Modifier.FINAL)
                .build();
        typeBuilder.addField(clientField);

        typeBuilder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(DatabaseClient.class, "client")
                .addStatement("this.$N = $T.requireNonNull(client, \"client\")", clientField, Objects.class)
                .build());

        typeBuilder.addMethod(MethodSpec.methodBuilder("using")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.get(schema.getPackageName(), table.getName() + "Repository"))
                .addParameter(DatabaseManager.class, "manager")
                .addStatement("return new $T(manager.getOrThrow($S))",
                        ClassName.get(schema.getPackageName(), table.getName() + "Repository"),
                        schema.getDatasource())
                .build());

        typeBuilder.addMethod(buildInsertMethod(table, recordType));
        typeBuilder.addMethod(buildFindByIdMethod(table, recordType));
        typeBuilder.addMethod(buildFindAllMethod(table, recordType));
        typeBuilder.addMethod(buildUpdateMethod(table, recordType));
        typeBuilder.addMethod(buildDeleteMethod(table));

        MethodSpec.Builder mapRow = MethodSpec.methodBuilder("mapRow")
                .addModifiers(Modifier.PRIVATE)
                .returns(recordType)
                .addParameter(ResultSet.class, "resultSet")
                .addException(SQLException.class);
        for (FieldDefinition field : table.getFields()) {
            mapRow.addCode(readFieldFromResultSet(field, columnName(field), field.getName()));
        }
        CodeBlock.Builder constructorArguments = CodeBlock.builder();
        for (int i = 0; i < table.getFields().size(); i++) {
            FieldDefinition field = table.getFields().get(i);
            constructorArguments.add("$N", field.getName());
            if (i < table.getFields().size() - 1) {
                constructorArguments.add(", ");
            }
        }
        mapRow.addStatement("return new $T($L)", recordType, constructorArguments.build());
        typeBuilder.addMethod(mapRow.build());

        return typeBuilder.build();
    }

    private MethodSpec buildInsertMethod(TableDefinition table, ClassName recordType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("insert")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(recordType, "record");
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ").append(table.getTableName()).append(" (");
        for (int i = 0; i < table.getFields().size(); i++) {
            FieldDefinition field = table.getFields().get(i);
            sql.append(columnName(field));
            if (i < table.getFields().size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(") VALUES (");
        for (int i = 0; i < table.getFields().size(); i++) {
            sql.append("?");
            if (i < table.getFields().size() - 1) {
                sql.append(", ");
            }
        }
        sql.append(")");
        builder.addStatement("final String sql = $S", sql.toString());
        builder.addStatement("client.execute(sql, $L)", buildStatementLambda(buildStatementBinder(table)));
        return builder.build();
    }

    private MethodSpec buildFindByIdMethod(TableDefinition table, ClassName recordType) {
        FieldDefinition idField = table.getPrimaryKey();
        TypeName idType = idField.getType().getJavaType(true);
        MethodSpec.Builder builder = MethodSpec.methodBuilder("findById")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(Optional.class), recordType))
                .addParameter(idType, "id")
                .addStatement("final String sql = $S",
                        "SELECT * FROM " + table.getTableName() + " WHERE " + columnName(idField) + " = ?")
                .addStatement("return client.queryOne(sql, $L, resultSet -> mapRow(resultSet))",
                        buildStatementLambda(buildSingleParameterBinder(idField, "id", 1)));
        return builder.build();
    }

    private MethodSpec buildFindAllMethod(TableDefinition table, ClassName recordType) {
        MethodSpec.Builder builder = MethodSpec.methodBuilder("findAll")
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), recordType))
                .addStatement("final String sql = $S", "SELECT * FROM " + table.getTableName())
                .addStatement("return client.query(sql, statement -> {}, this::mapRow)");
        return builder.build();
    }

    private MethodSpec buildUpdateMethod(TableDefinition table, ClassName recordType) {
        FieldDefinition idField = table.getPrimaryKey();
        MethodSpec.Builder builder = MethodSpec.methodBuilder("update")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(recordType, "record");
        List<FieldDefinition> updatableFields = table.getFields().stream()
                .filter(field -> !field.isId())
                .collect(Collectors.toList());
        if (updatableFields.isEmpty()) {
            throw new DatabaseException("Table '" + table.getName() + "' does not have mutable columns");
        }
        String assignments = updatableFields.stream()
                .map(field -> columnName(field) + " = ?")
                .collect(Collectors.joining(", "));
        builder.addStatement("final String sql = $S",
                "UPDATE " + table.getTableName() + " SET " + assignments +
                        " WHERE " + columnName(idField) + " = ?");
        builder.addStatement("client.execute(sql, $L)",
                buildStatementLambda(buildUpdateBinder(table)));
        return builder.build();
    }

    private MethodSpec buildDeleteMethod(TableDefinition table) {
        FieldDefinition idField = table.getPrimaryKey();
        TypeName idType = idField.getType().getJavaType(true);
        MethodSpec.Builder builder = MethodSpec.methodBuilder("deleteById")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(idType, "id")
                .addStatement("final String sql = $S",
                        "DELETE FROM " + table.getTableName() + " WHERE " + columnName(idField) + " = ?")
                .addStatement("client.execute(sql, $L)",
                        buildStatementLambda(buildSingleParameterBinder(idField, "id", 1)));
        return builder.build();
    }

    private CodeBlock buildStatementBinder(TableDefinition table) {
        CodeBlock.Builder builder = CodeBlock.builder();
        int parameterIndex = 1;
        for (FieldDefinition field : table.getFields()) {
            builder.add(buildFieldBinding(field, "record.get" + capitalize(field.getName()) + "()", parameterIndex++));
        }
        return builder.build();
    }

    private CodeBlock buildUpdateBinder(TableDefinition table) {
        CodeBlock.Builder builder = CodeBlock.builder();
        int parameterIndex = 1;
        for (FieldDefinition field : table.getFields()) {
            if (field.isId()) {
                continue;
            }
            builder.add(buildFieldBinding(field, "record.get" + capitalize(field.getName()) + "()", parameterIndex++));
        }
        FieldDefinition idField = table.getPrimaryKey();
        builder.add(buildFieldBinding(idField, "record.get" + capitalize(idField.getName()) + "()", parameterIndex));
        return builder.build();
    }

    private CodeBlock buildFieldBinding(FieldDefinition field, String accessor, int index) {
        CodeBlock.Builder builder = CodeBlock.builder();
        builder.addStatement("final $T value$L = $L", field.getType().getJavaType(true), index, accessor);
        String statement;
        switch (field.getType()) {
            case STRING:
            case TEXT:
            case JSON:
                statement = "statement.setString($L, value$L)";
                break;
            case INT:
                statement = "statement.setInt($L, value$L)";
                break;
            case LONG:
                statement = "statement.setLong($L, value$L)";
                break;
            case BOOLEAN:
                statement = "statement.setBoolean($L, value$L)";
                break;
            case DOUBLE:
                statement = "statement.setDouble($L, value$L)";
                break;
            case DECIMAL:
                statement = "statement.setBigDecimal($L, value$L)";
                break;
            case TIMESTAMP:
                builder.addStatement("final $T timestamp$L = value$L != null ? $T.from(value$L) : null",
                        Timestamp.class, index, index, Timestamp.class, index);
                statement = "statement.setTimestamp($L, timestamp$L)";
                break;
            case DATE:
                builder.addStatement("final $T date$L = value$L != null ? $T.valueOf(value$L) : null",
                        Date.class, index, index, Date.class, index);
                statement = "statement.setDate($L, date$L)";
                break;
            case UUID:
                statement = "statement.setObject($L, value$L)";
                break;
            default:
                throw new DatabaseException("Unsupported field type: " + field.getType());
        }
        if (field.isNullable()) {
            builder.beginControlFlow("if (value$L == null)", index)
                    .addStatement("statement.setNull($L, $L)", index, sqlTypeConstant(field))
                    .nextControlFlow("else")
                    .addStatement(statement, index, index)
                    .endControlFlow();
        } else {
            builder.addStatement(statement, index, index);
        }
        return builder.build();
    }

    private CodeBlock buildSingleParameterBinder(FieldDefinition field, String valueRef, int index) {
        return buildFieldBinding(field, valueRef, index);
    }

    private CodeBlock buildStatementLambda(CodeBlock body) {
        return CodeBlock.builder()
                .add("statement -> {\n")
                .indent()
                .add(body)
                .unindent()
                .add("}")
                .build();
    }

    private CodeBlock readFieldFromResultSet(FieldDefinition field, String columnName, String variableName) {
        CodeBlock.Builder builder = CodeBlock.builder();
        switch (field.getType()) {
            case STRING:
            case TEXT:
            case JSON:
                builder.addStatement("final $T $N = resultSet.getString($S)",
                        field.getType().getJavaType(true), variableName, columnName);
                break;
            case INT:
                builder.addStatement("final Integer $N = ($T) resultSet.getObject($S)",
                        variableName, Integer.class, columnName);
                break;
            case LONG:
                builder.addStatement("final Long $N = ($T) resultSet.getObject($S)",
                        variableName, Long.class, columnName);
                break;
            case BOOLEAN:
                builder.addStatement("final Boolean $N = ($T) resultSet.getObject($S)",
                        variableName, Boolean.class, columnName);
                break;
            case DOUBLE:
                builder.addStatement("final Double $N = ($T) resultSet.getObject($S)",
                        variableName, Double.class, columnName);
                break;
            case DECIMAL:
                builder.addStatement("final $T $N = resultSet.getBigDecimal($S)",
                        field.getType().getJavaType(true), variableName, columnName);
                break;
            case TIMESTAMP:
                builder.addStatement("final $T ts$N = resultSet.getTimestamp($S)", Timestamp.class, variableName, columnName)
                        .addStatement("final $T $N = ts$N != null ? ts$N.toInstant() : null",
                                field.getType().getJavaType(true), variableName, variableName, variableName);
                break;
            case DATE:
                builder.addStatement("final $T date$N = resultSet.getDate($S)", Date.class, variableName, columnName)
                        .addStatement("final $T $N = date$N != null ? date$N.toLocalDate() : null",
                                field.getType().getJavaType(true), variableName, variableName, variableName);
                break;
            case UUID:
                String rawVariable = variableName + "Raw";
                builder.addStatement("final Object $N = resultSet.getObject($S)", rawVariable, columnName)
                        .addStatement("final $T $N = $N == null ? null : ($N instanceof $T ? ($T) $N : $T.fromString($N.toString()))",
                                UUID.class, variableName, rawVariable, rawVariable, UUID.class, UUID.class, rawVariable, UUID.class, rawVariable);
                break;
            default:
                throw new DatabaseException("Unsupported field type: " + field.getType());
        }
        return builder.build();
    }

    private int sqlTypeConstant(FieldDefinition field) {
        switch (field.getType()) {
            case STRING:
            case TEXT:
            case JSON:
                return Types.VARCHAR;
            case INT:
                return Types.INTEGER;
            case LONG:
                return Types.BIGINT;
            case BOOLEAN:
                return Types.BOOLEAN;
            case DOUBLE:
                return Types.DOUBLE;
            case DECIMAL:
                return Types.DECIMAL;
            case TIMESTAMP:
                return Types.TIMESTAMP;
            case DATE:
                return Types.DATE;
            case UUID:
                return Types.OTHER;
            default:
                throw new DatabaseException("Unsupported field type: " + field.getType());
        }
    }

    private String columnName(FieldDefinition field) {
        return field.getColumnName() != null ? field.getColumnName() : field.getName();
    }

    private String capitalize(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}