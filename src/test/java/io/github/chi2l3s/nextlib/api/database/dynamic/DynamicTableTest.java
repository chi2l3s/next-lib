package io.github.chi2l3s.nextlib.api.database.dynamic;

import io.github.chi2l3s.nextlib.api.database.DatabaseClient;
import io.github.chi2l3s.nextlib.api.database.DatabaseConfig;
import io.github.chi2l3s.nextlib.api.database.DatabaseManager;
import io.github.chi2l3s.nextlib.api.database.DatabaseType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DisplayName("DynamicTable Tests")
class DynamicTableTest {

    private DatabaseManager manager;
    private DatabaseClient client;
    private DynamicDatabase database;

    @AllArgsConstructor
    @Getter
    static class TestEntity {
        @PrimaryKey
        private final UUID id;
        private final String name;
        private final Integer age;
    }

    @BeforeEach
    void setUp() {
        manager = new DatabaseManager();
        DatabaseConfig config = DatabaseConfig.builder(DatabaseType.SQLITE)
                .file(":memory:")
                .build();
        client = manager.register("test", config);
        database = new DynamicDatabase(client);
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.close();
        }
    }

    @Test
    @DisplayName("Should create table and insert entity")
    void shouldCreateTableAndInsertEntity() {
        // Given
        DynamicTable<TestEntity> table = database.register(TestEntity.class);
        UUID id = UUID.randomUUID();
        TestEntity entity = new TestEntity(id, "John", 25);

        // When
        int result = table.create(entity);

        // Then
        assertThat(result).isEqualTo(1);
    }

    @Test
    @DisplayName("Should find entity by primary key")
    void shouldFindEntityByPrimaryKey() {
        // Given
        DynamicTable<TestEntity> table = database.register(TestEntity.class);
        UUID id = UUID.randomUUID();
        TestEntity entity = new TestEntity(id, "John", 25);
        table.create(entity);

        // When
        Optional<TestEntity> result = table.findFirst()
                .where("id", id)
                .execute();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("John");
        assertThat(result.get().getAge()).isEqualTo(25);
    }

    @Test
    @DisplayName("Should return empty Optional when entity not found")
    void shouldReturnEmptyWhenNotFound() {
        // Given
        DynamicTable<TestEntity> table = database.register(TestEntity.class);

        // When
        Optional<TestEntity> result = table.findFirst()
                .where("id", UUID.randomUUID())
                .execute();

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should find multiple entities")
    void shouldFindMultipleEntities() {
        // Given
        DynamicTable<TestEntity> table = database.register(TestEntity.class);
        table.create(new TestEntity(UUID.randomUUID(), "John", 25));
        table.create(new TestEntity(UUID.randomUUID(), "Jane", 30));
        table.create(new TestEntity(UUID.randomUUID(), "Bob", 25));

        // When
        List<TestEntity> result = table.findMany()
                .where("age", 25)
                .execute();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).extracting(TestEntity::getName)
                .containsExactlyInAnyOrder("John", "Bob");
    }

    @Test
    @DisplayName("Should update entity")
    void shouldUpdateEntity() {
        // Given
        DynamicTable<TestEntity> table = database.register(TestEntity.class);
        UUID id = UUID.randomUUID();
        TestEntity entity = new TestEntity(id, "John", 25);
        table.create(entity);

        // When
        int updated = table.update()
                .set("age", 26)
                .where("id", id)
                .execute();

        // Then
        assertThat(updated).isEqualTo(1);
        Optional<TestEntity> result = table.findFirst()
                .where("id", id)
                .execute();
        assertThat(result).isPresent();
        assertThat(result.get().getAge()).isEqualTo(26);
    }

    @Test
    @DisplayName("Should handle null values")
    void shouldHandleNullValues() {
        // Given
        DynamicTable<TestEntity> table = database.register(TestEntity.class);
        UUID id = UUID.randomUUID();
        TestEntity entity = new TestEntity(id, "John", null);

        // When
        table.create(entity);
        Optional<TestEntity> result = table.findFirst()
                .where("id", id)
                .execute();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getAge()).isNull();
    }

    @Test
    @DisplayName("Should find entity where field is null")
    void shouldFindEntityWhereFieldIsNull() {
        // Given
        DynamicTable<TestEntity> table = database.register(TestEntity.class);
        table.create(new TestEntity(UUID.randomUUID(), "John", null));
        table.create(new TestEntity(UUID.randomUUID(), "Jane", 30));

        // When
        List<TestEntity> result = table.findMany()
                .where("age", null)
                .execute();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("John");
    }

    @Test
    @DisplayName("Should use custom table name")
    void shouldUseCustomTableName() {
        // Given
        DynamicTable<TestEntity> table = database.register("custom_users", TestEntity.class);
        UUID id = UUID.randomUUID();
        TestEntity entity = new TestEntity(id, "John", 25);

        // When
        table.create(entity);
        Optional<TestEntity> result = table.findFirst()
                .where("id", id)
                .execute();

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("John");
    }
}
