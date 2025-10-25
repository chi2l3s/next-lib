package io.github.chi2l3s.nextlib.api.database;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlFunction<T, R> {
    R apply(T value) throws SQLException;
}