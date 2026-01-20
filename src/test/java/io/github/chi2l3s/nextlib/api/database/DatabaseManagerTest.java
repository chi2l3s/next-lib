package io.github.chi2l3s.nextlib.api.database;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DatabaseManager Tests")
class DatabaseManagerTest {

    private DatabaseManager manager;

    @BeforeEach
    void setUp() {
        manager = new DatabaseManager();
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.close();
        }
    }

    @Test
    @DisplayName("Should register database client with SQLite configuration")
    void shouldRegisterDatabaseClient() {
        // Given
        DatabaseConfig config = DatabaseConfig.builder(DatabaseType.SQLITE)
                .file(":memory:")
                .build();

        // When
        DatabaseClient client = manager.register("test", config);

        // Then
        assertThat(client).isNotNull();
        assertThat(manager.get("test")).isPresent();
    }

    @Test
    @DisplayName("Should set first registered client as default")
    void shouldSetFirstClientAsDefault() {
        // Given
        DatabaseConfig config = DatabaseConfig.builder(DatabaseType.SQLITE)
                .file(":memory:")
                .build();

        // When
        manager.register("test", config);
        DatabaseClient defaultClient = manager.getDefault();

        // Then
        assertThat(defaultClient).isNotNull();
        assertThat(manager.get("test")).isPresent();
    }

    @Test
    @DisplayName("Should throw exception when getting non-existent client")
    void shouldThrowExceptionForNonExistentClient() {
        // When & Then
        assertThatThrownBy(() -> manager.getOrThrow("nonexistent"))
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("No database client registered with name 'nonexistent'");
    }

    @Test
    @DisplayName("Should return empty Optional for non-existent client")
    void shouldReturnEmptyOptionalForNonExistentClient() {
        // When
        var result = manager.get("nonexistent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should unregister client and update default")
    void shouldUnregisterClient() {
        // Given
        DatabaseConfig config = DatabaseConfig.builder(DatabaseType.SQLITE)
                .file(":memory:")
                .build();
        manager.register("test1", config);
        manager.register("test2", config);

        // When
        manager.unregister("test1");

        // Then
        assertThat(manager.get("test1")).isEmpty();
        assertThat(manager.get("test2")).isPresent();
    }

    @Test
    @DisplayName("Should allow changing default client")
    void shouldAllowChangingDefaultClient() {
        // Given
        DatabaseConfig config = DatabaseConfig.builder(DatabaseType.SQLITE)
                .file(":memory:")
                .build();
        manager.register("test1", config);
        manager.register("test2", config);

        // When
        manager.setDefaultClient("test2");
        DatabaseClient defaultClient = manager.getDefault();

        // Then
        assertThat(defaultClient).isEqualTo(manager.getOrThrow("test2"));
    }

    @Test
    @DisplayName("Should throw exception when setting non-existent client as default")
    void shouldThrowExceptionWhenSettingNonExistentDefault() {
        // When & Then
        assertThatThrownBy(() -> manager.setDefaultClient("nonexistent"))
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("No database client registered with name 'nonexistent'");
    }

    @Test
    @DisplayName("Should close all data sources on manager close")
    void shouldCloseAllDataSourcesOnClose() {
        // Given
        DatabaseConfig config = DatabaseConfig.builder(DatabaseType.SQLITE)
                .file(":memory:")
                .build();
        manager.register("test", config);

        // When
        manager.close();

        // Then
        assertThatThrownBy(() -> manager.getDefault())
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("No database clients have been registered");
    }

    @Test
    @DisplayName("Should create working connection from registered client")
    void shouldCreateWorkingConnection() throws SQLException {
        // Given
        DatabaseConfig config = DatabaseConfig.builder(DatabaseType.SQLITE)
                .file(":memory:")
                .build();
        DatabaseClient client = manager.register("test", config);

        // When
        try (Connection connection = client.openConnection()) {
            // Then
            assertThat(connection).isNotNull();
            assertThat(connection.isClosed()).isFalse();
        }
    }

    @Test
    @DisplayName("Should handle HikariCP properties")
    void shouldHandleHikariProperties() {
        // Given
        DatabaseConfig config = DatabaseConfig.builder(DatabaseType.SQLITE)
                .file(":memory:")
                .property("maximumPoolSize", "5")
                .property("minimumIdle", "2")
                .property("connectionTimeout", "30000")
                .build();

        // When
        DatabaseClient client = manager.register("test", config);

        // Then
        assertThat(client).isNotNull();
        assertThat(manager.get("test")).isPresent();
    }
}
