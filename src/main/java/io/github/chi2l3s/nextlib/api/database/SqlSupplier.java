package io.github.chi2l3s.nextlib.api.database;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlSupplier<T> {
    T get() throws SQLException;
}