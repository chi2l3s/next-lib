# Динамическая база данных - Руководство

## Обзор

Модуль динамической базы данных NextLib предоставляет легковесную ORM-систему для работы с реляционными базами данных. Вместо ручного написания SQL-запросов, вы описываете сущности через обычные Java-классы, а библиотека автоматически создаёт таблицы и предоставляет Fluent API для CRUD-операций.

## Поддерживаемые базы данных

- **MySQL** - production-ready СУБД
- **PostgreSQL** - современная СУБД с расширенными возможностями
- **SQLite** - встроенная БД для небольших проектов

## Быстрый старт

### 1. Создание подключения

```java
// Регистрация менеджера базы данных
DatabaseManager manager = new DatabaseManager();

// MySQL конфигурация
DatabaseConfig mysqlConfig = DatabaseConfig.builder(DatabaseType.MYSQL)
    .host("localhost")
    .port(3306)
    .database("nexttraps")
    .username("root")
    .password("password")
    .property("maximumPoolSize", "10")
    .property("minimumIdle", "2")
    .property("connectionTimeout", "30000")
    .build();

DatabaseClient client = manager.register("main", mysqlConfig);

// SQLite конфигурация (для разработки)
DatabaseConfig sqliteConfig = DatabaseConfig.builder(DatabaseType.SQLITE)
    .file(plugin.getDataFolder() + "/database.db")
    .build();

DatabaseClient devClient = manager.register("dev", sqliteConfig);
```

### 2. Создание сущности

Создайте Java-класс с аннотацией `@PrimaryKey` для первичного ключа:

```java
import io.github.chi2l3s.nextlib.api.database.dynamic.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.UUID;

@AllArgsConstructor
@Getter
public class PlayerEntity {
    @PrimaryKey
    private final UUID playerId;
    private final String nickname;
    private final String trapSkinId;
    private final Integer coins;
    private final Long lastLogin;
}
```

**Важно:**
- Класс должен иметь конструктор, принимающий все поля в порядке объявления
- Поле с `@PrimaryKey` становится первичным ключом таблицы
- Если аннотация отсутствует, первое поле будет использовано как первичный ключ
- Используйте `final` поля для неизменяемых сущностей (рекомендуется)

### 3. Регистрация сущности

```java
DynamicDatabase database = new DynamicDatabase(client);

// Автоматическое имя таблицы: player_entitys
DynamicTable<PlayerEntity> players = database.register(PlayerEntity.class);

// Или с custom именем таблицы
DynamicTable<PlayerEntity> players = database.register("players", PlayerEntity.class);
```

При регистрации автоматически создаётся таблица, если её нет.

## CRUD операции

### Создание записи

```java
UUID playerId = player.getUniqueId();
PlayerEntity entity = new PlayerEntity(
    playerId,
    "chi2l3s",
    "default_trap",
    1000,
    System.currentTimeMillis()
);

int rows = players.create(entity);
// rows = 1 если успешно
```

### Поиск одной записи

```java
Optional<PlayerEntity> result = players.findFirst()
    .where("playerId", playerId)
    .execute();

result.ifPresent(entity -> {
    player.sendMessage("Welcome back, " + entity.getNickname());
    player.sendMessage("Coins: " + entity.getCoins());
});
```

### Поиск множества записей

```java
// Найти всех игроков с определённым скином
List<PlayerEntity> withTrap = players.findMany()
    .where("trapSkinId", "lava_trap")
    .execute();

// Найти всех богатых игроков
List<PlayerEntity> richPlayers = players.findMany()
    .where("coins", 10000)
    .execute();
```

### Обновление записей

```java
// Обновить количество монет
int updated = players.update()
    .set("coins", 1500)
    .where("playerId", playerId)
    .execute();

// Обновить несколько полей
int updated = players.update()
    .set("trapSkinId", "ice_trap")
    .set("coins", 2000)
    .set("lastLogin", System.currentTimeMillis())
    .where("playerId", playerId)
    .execute();
```

### Работа с NULL значениями

```java
// Найти игроков без скина
List<PlayerEntity> noSkin = players.findMany()
    .where("trapSkinId", null)
    .execute();

// Установить поле в NULL
players.update()
    .set("trapSkinId", null)
    .where("playerId", playerId)
    .execute();
```

## Поддерживаемые типы полей

| Java тип | SQL тип (MySQL) | SQL тип (PostgreSQL) | SQL тип (SQLite) |
|----------|----------------|---------------------|------------------|
| UUID | VARCHAR(36) | UUID | TEXT |
| String | TEXT | TEXT | TEXT |
| Integer | INT | INTEGER | INTEGER |
| Long | BIGINT | BIGINT | INTEGER |
| Double | DOUBLE | DOUBLE PRECISION | REAL |
| Boolean | BOOLEAN | BOOLEAN | INTEGER |
| byte[] | BLOB | BYTEA | BLOB |

## Продвинутые примеры

### Множественные условия WHERE

```java
// Все условия объединяются через AND
List<PlayerEntity> result = players.findMany()
    .where("trapSkinId", "lava_trap")
    .where("coins", 1000)
    .execute();
// SQL: SELECT * FROM players WHERE trapSkinId = ? AND coins = ?
```

### Работа с транзакциями

```java
client.withConnection(connection -> {
    connection.setAutoCommit(false);
    try {
        // Создание нескольких записей
        players.create(entity1);
        players.create(entity2);

        connection.commit();
        return true;
    } catch (Exception e) {
        connection.rollback();
        throw e;
    }
});
```

### Использование нескольких таблиц

```java
DynamicDatabase database = new DynamicDatabase(client);

DynamicTable<PlayerEntity> players = database.register(PlayerEntity.class);
DynamicTable<TrapEntity> traps = database.register(TrapEntity.class);
DynamicTable<SettingsEntity> settings = database.register(SettingsEntity.class);

// Получение зарегистрированной таблицы
DynamicTable<PlayerEntity> playersTable = database.get("player_entitys", PlayerEntity.class);
```

### HikariCP настройки

```java
DatabaseConfig config = DatabaseConfig.builder(DatabaseType.MYSQL)
    .host("localhost")
    .database("mydb")
    .username("user")
    .password("pass")
    // Пул соединений
    .property("maximumPoolSize", "20")      // Максимум соединений
    .property("minimumIdle", "5")           // Минимум idle соединений
    .property("connectionTimeout", "30000") // Таймаут подключения (мс)
    .property("idleTimeout", "600000")      // Таймаут idle (мс)
    .property("maxLifetime", "1800000")     // Максимальное время жизни (мс)
    .property("leakDetectionThreshold", "60000") // Детекция утечек
    // MySQL специфичные
    .property("cachePrepStmts", "true")
    .property("prepStmtCacheSize", "250")
    .property("prepStmtCacheSqlLimit", "2048")
    .build();
```

## Best Practices

### 1. Закрывайте ресурсы

```java
DatabaseManager manager = new DatabaseManager();
try {
    // Работа с БД
} finally {
    manager.close(); // Закрывает все пулы соединений
}
```

### 2. Используйте connection pooling

HikariCP уже настроен по умолчанию. Не создавайте новые соединения вручную:

```java
// ❌ Плохо
try (Connection conn = DriverManager.getConnection(...)) { }

// ✅ Хорошо
DatabaseClient client = manager.getDefault();
client.withConnection(connection -> {
    // Используйте connection
    return result;
});
```

### 3. Обрабатывайте Optional правильно

```java
// ❌ Плохо
PlayerEntity entity = players.findFirst()
    .where("playerId", playerId)
    .execute()
    .get(); // Может бросить NoSuchElementException

// ✅ Хорошо
PlayerEntity entity = players.findFirst()
    .where("playerId", playerId)
    .execute()
    .orElseThrow(() -> new PlayerNotFoundException(playerId));

// Или
players.findFirst()
    .where("playerId", playerId)
    .execute()
    .ifPresent(entity -> {
        // Работа с entity
    });
```

### 4. Используйте immutable entities

```java
// ✅ Рекомендуется
@AllArgsConstructor
@Getter
public class PlayerEntity {
    private final UUID playerId; // final поля
    private final String nickname;
    private final Integer coins;
}

// ❌ Не рекомендуется
public class PlayerEntity {
    private UUID playerId; // mutable поля
    private String nickname;
    private Integer coins;

    // setters...
}
```

### 5. Индексируйте часто используемые поля

Хотя DynamicDatabase не создаёт индексы автоматически, вы можете создать их вручную:

```java
client.execute(
    "CREATE INDEX idx_player_nickname ON players(nickname)",
    null
);
```

## Ограничения

1. **Нет поддержки связей (relationships)** - ORM не поддерживает foreign keys и автоматические joins
2. **Только базовые WHERE условия** - поддерживается только `=` и `IS NULL`
3. **Нет автоматических миграций** - при изменении схемы нужно обновлять таблицы вручную
4. **Нет ленивой загрузки** - все данные загружаются сразу
5. **Только простые типы данных** - нет поддержки вложенных объектов

## Миграции схемы

При изменении структуры сущности:

```java
// 1. Создайте миграцию вручную
client.execute("ALTER TABLE players ADD COLUMN premium BOOLEAN DEFAULT FALSE", null);

// 2. Обновите сущность
@AllArgsConstructor
@Getter
public class PlayerEntity {
    @PrimaryKey
    private final UUID playerId;
    private final String nickname;
    private final Integer coins;
    private final Boolean premium; // Новое поле
}

// 3. Перерегистрируйте таблицу (опционально)
database.register(PlayerEntity.class);
```

## Troubleshooting

### Ошибка "Missing JDBC driver"

Убедитесь, что драйвер БД добавлен в зависимости:

```gradle
dependencies {
    implementation 'com.zaxxer:HikariCP:7.0.2'
    implementation 'mysql:mysql-connector-java:8.0.33'     // MySQL
    implementation 'org.postgresql:postgresql:42.6.0'      // PostgreSQL
    implementation 'org.xerial:sqlite-jdbc:3.43.0.0'       // SQLite
}
```

### Ошибка "Failed to resolve constructor"

Проверьте, что:
1. Класс имеет конструктор со всеми полями
2. Порядок параметров конструктора совпадает с порядком полей
3. Типы параметров совпадают с типами полей

```java
// ✅ Правильно
@AllArgsConstructor // Lombok генерирует конструктор
public class PlayerEntity {
    private final UUID id;
    private final String name;
}

// ❌ Неправильно - порядок не совпадает
public class PlayerEntity {
    private final UUID id;
    private final String name;

    public PlayerEntity(String name, UUID id) { // Неверный порядок!
        this.id = id;
        this.name = name;
    }
}
```

### Медленные запросы

1. Добавьте индексы на часто используемые поля
2. Увеличьте размер пула соединений
3. Используйте batch операции для массовых вставок

```java
// Batch insert
List<SqlConsumer<PreparedStatement>> binders = entities.stream()
    .map(entity -> stmt -> bindEntity(stmt, entity))
    .collect(Collectors.toList());

client.executeBatch(insertSql, binders);
```

## Полезные ссылки

- [HikariCP Configuration](https://github.com/brettwooldridge/HikariCP#configuration-knobs-baby)
- [MySQL JDBC Driver](https://dev.mysql.com/doc/connector-j/8.0/en/)
- [PostgreSQL JDBC Driver](https://jdbc.postgresql.org/documentation/)
- [SQLite JDBC Driver](https://github.com/xerial/sqlite-jdbc)
