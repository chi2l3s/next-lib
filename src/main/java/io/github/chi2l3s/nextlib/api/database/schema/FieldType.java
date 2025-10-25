package io.github.chi2l3s.nextlib.api.database.schema;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Supported column types for the code generator.
 */
public enum FieldType {
    STRING("VARCHAR(255)", ClassName.get(String.class)),
    TEXT("TEXT", ClassName.get(String.class)),
    INT("INTEGER", TypeName.INT),
    LONG("BIGINT", TypeName.LONG),
    BOOLEAN("BOOLEAN", TypeName.BOOLEAN),
    DOUBLE("DOUBLE PRECISION", TypeName.DOUBLE),
    DECIMAL("DECIMAL(19,4)", ClassName.get(BigDecimal.class)),
    TIMESTAMP("TIMESTAMP", ClassName.get(Instant.class)),
    DATE("DATE", ClassName.get(LocalDate.class)),
    UUID("UUID", ClassName.get(UUID.class)),
    JSON("JSON", ClassName.get(String.class));

    private final String sqlType;
    private final TypeName javaType;

    FieldType(String sqlType, TypeName javaType) {
        this.sqlType = sqlType;
        this.javaType = javaType;
    }

    public String getSqlType() {
        return sqlType;
    }

    public TypeName getJavaType(boolean nullable) {
        if (nullable) {
            return javaType.box();
        }
        return javaType;
    }
}