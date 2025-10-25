package io.github.chi2l3s.nextlib.api.database;

import java.sql.SQLException;

@FunctionalInterface
public interface SqlConsumer<T> {
    void accept(T value) throws SQLException;
}
