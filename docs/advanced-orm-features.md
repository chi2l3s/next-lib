# Advanced ORM Features

This guide covers the advanced ORM features in NextLib v1.0.7, including relationships, lazy loading, embedded objects, and database migrations.

## Table of Contents

1. [Embedded Objects](#embedded-objects)
2. [Entity Relationships](#entity-relationships)
3. [Lazy Loading](#lazy-loading)
4. [Cascade Operations](#cascade-operations)
5. [Database Migrations](#database-migrations)

---

## Embedded Objects

Embed complex objects into your entity tables without creating separate tables.

### Basic Usage

```java
import io.github.chi2l3s.nextlib.api.database.dynamic.*;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class Address {
    private final String street;
    private final String city;
    private final String zipCode;
}

@AllArgsConstructor
@Getter
public class Player {
    @PrimaryKey
    private final UUID id;
    private final String name;

    @Embedded
    private final Address address;
}
```

**Generated Table Structure:**
```sql
CREATE TABLE players (
    id TEXT PRIMARY KEY,
    name TEXT,
    address_street TEXT,
    address_city TEXT,
    address_zipCode TEXT
);
```

### Custom Prefix

```java
@Embedded(prefix = "home")
private final Address homeAddress;

@Embedded(prefix = "work")
private final Address workAddress;
```

**Result:**
```sql
-- Columns: home_street, home_city, home_zipCode
-- Columns: work_street, work_city, work_zipCode
```

---

## Entity Relationships

Define relationships between entities with full cascade and fetch control.

### @OneToOne

One-to-one relationship between two entities.

```java
@AllArgsConstructor
@Getter
public class Player {
    @PrimaryKey
    private final UUID id;
    private final String name;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "profile_id")
    private final PlayerProfile profile;
}

@AllArgsConstructor
@Getter
public class PlayerProfile {
    @PrimaryKey
    private final UUID id;
    private final String bio;
    private final String avatarUrl;
}
```

### @ManyToOne

Many entities reference one parent entity.

```java
@AllArgsConstructor
@Getter
public class Quest {
    @PrimaryKey
    private final UUID id;
    private final String name;
    private final String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id", nullable = false)
    private final Player player;
}
```

**Generated Schema:**
```sql
CREATE TABLE quests (
    id TEXT PRIMARY KEY,
    name TEXT,
    description TEXT,
    player_id TEXT NOT NULL  -- Foreign key
);
```

### @OneToMany

One parent entity has many child entities.

```java
@AllArgsConstructor
@Getter
public class Player {
    @PrimaryKey
    private final UUID id;
    private final String name;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final List<Quest> quests;
}
```

**Bidirectional Relationship:**
```java
// In Quest.java
@ManyToOne
@JoinColumn(name = "player_id")
private final Player player;

// In Player.java
@OneToMany(mappedBy = "player")  // References Quest.player field
private final List<Quest> quests;
```

---

## Lazy Loading

Delay loading of related entities until accessed.

### How It Works

Lazy loading uses Java Dynamic Proxies to delay database queries until the relationship is actually accessed.

```java
@AllArgsConstructor
@Getter
public class Player {
    @PrimaryKey
    private final UUID id;

    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY)  // Lazy!
    private final List<Quest> quests;
}

// Usage
Player player = playerTable.findById(playerId).orElseThrow();
// No query for quests yet!

List<Quest> quests = player.getQuests();
// Now queries quests from database
```

### Eager vs Lazy

**EAGER (default for @ManyToOne, @OneToOne):**
- Loads relationship immediately with parent entity
- Uses JOIN query
- Best for frequently accessed relationships

**LAZY (default for @OneToMany):**
- Loads relationship on first access
- Uses separate query
- Best for rarely accessed or large collections

### Check Load Status

```java
import io.github.chi2l3s.nextlib.api.database.dynamic.LazyLoadingHandler.LazyLoadable;

if (quests instanceof LazyLoadable loadable) {
    if (!loadable.isLoaded()) {
        System.out.println("Quests not loaded yet");
    }

    // Force immediate loading
    loadable.forceLoad();
}
```

---

## Cascade Operations

Automatically propagate operations to related entities.

### Cascade Types

```java
public enum CascadeType {
    PERSIST,  // Save related entities
    REMOVE,   // Delete related entities
    MERGE,    // Update related entities
    REFRESH,  // Reload related entities
    ALL       // All of the above
}
```

### Examples

**CASCADE.PERSIST:**
```java
@OneToMany(mappedBy = "player", cascade = CascadeType.PERSIST)
private final List<Quest> quests;

// Saving player also saves all quests
Player player = new Player(id, "John", List.of(
    new Quest(quest1Id, "Quest 1", "Description", player),
    new Quest(quest2Id, "Quest 2", "Description", player)
));

playerTable.create(player);
// Automatically creates quest records too!
```

**CASCADE.REMOVE:**
```java
@OneToMany(mappedBy = "player", cascade = CascadeType.REMOVE)
private final List<Quest> quests;

// Deleting player also deletes all quests
playerTable.delete(player);
// All player's quests are automatically deleted!
```

**CASCADE.ALL:**
```java
@OneToOne(cascade = CascadeType.ALL)
@JoinColumn(name = "profile_id")
private final PlayerProfile profile;

// All operations cascade: save, update, delete, refresh
```

---

## Database Migrations

Manage database schema changes with version tracking and rollback support.

### Setup

```java
import io.github.chi2l3s.nextlib.api.database.migration.*;

DatabaseClient client = manager.getDefault();
MigrationManager migrations = new MigrationManager(client);

// Initialize migration tracking table
migrations.init();
```

### Creating Migrations

**Manually:**
```java
Migration m1 = Migration.builder("001")
    .description("Add email column to players")
    .addColumn("players", "email", "TEXT")
    .build();

migrations.apply(m1);
```

**With Builder Methods:**
```java
Migration m2 = Migration.builder("002")
    .description("Create achievements table")
    .createTable("achievements", List.of(
        "id TEXT PRIMARY KEY",
        "player_id TEXT NOT NULL",
        "name TEXT NOT NULL",
        "earned_at TIMESTAMP"
    ))
    .build();

migrations.apply(m2);
```

**Custom SQL:**
```java
Migration m3 = Migration.builder("003")
    .description("Add index on player_id")
    .addUp("CREATE INDEX idx_quests_player ON quests(player_id)")
    .addDown("DROP INDEX idx_quests_player")
    .build();

migrations.apply(m3);
```

### Auto-Generated Migrations

Use `SchemaDiffer` to detect changes:

```java
SchemaDiffer differ = new SchemaDiffer(client);

Map<String, SchemaDiffer.ColumnDefinition> expectedColumns = Map.of(
    "id", new SchemaDiffer.ColumnDefinition("TEXT", false, true),
    "name", new SchemaDiffer.ColumnDefinition("TEXT", false, false),
    "email", new SchemaDiffer.ColumnDefinition("TEXT", true, false)
);

Migration migration = differ.generateMigration("players", expectedColumns);
if (migration != null) {
    migrations.apply(migration);
}
```

### Rollback

```java
// Rollback a specific migration
migrations.rollback("002");

// Check applied migrations
List<MigrationManager.AppliedMigration> applied = migrations.getAppliedMigrations();
for (var m : applied) {
    System.out.println(m.getVersion() + ": " + m.getDescription());
    System.out.println("Applied at: " + m.getAppliedAt());
    System.out.println("Execution time: " + m.getExecutionTimeMs() + "ms");
}
```

### Migration Best Practices

1. **Version Naming:**
   ```java
   "001_initial_schema"
   "002_add_email_column"
   "003_create_achievements"
   ```

2. **Always Test Rollback:**
   ```java
   migrations.apply(migration);
   // Test functionality
   migrations.rollback(migration.getVersion());
   // Test rollback worked
   migrations.apply(migration);
   ```

3. **Avoid Destructive Operations:**
   ```java
   // Instead of DROP COLUMN, mark as deprecated
   Migration.builder("004")
       .addColumn("players", "legacy_field_deprecated", "TEXT")
       .build();
   ```

4. **Use Transactions:**
   - Migrations automatically run in transactions
   - Rollback on failure
   - All-or-nothing execution

---

## Complete Example

Putting it all together:

```java
// Define entities with relationships
@AllArgsConstructor
@Getter
public class Player {
    @PrimaryKey
    private final UUID id;
    private final String name;

    @Embedded(prefix = "addr")
    private final Address address;

    @OneToMany(mappedBy = "player", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final List<Quest> quests;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "stats_id")
    private final PlayerStats stats;
}

@AllArgsConstructor
@Getter
public class Quest {
    @PrimaryKey
    private final UUID id;
    private final String name;
    private final String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "player_id")
    private final Player player;
}

@AllArgsConstructor
@Getter
public class PlayerStats {
    @PrimaryKey
    private final UUID id;
    private final int level;
    private final long experience;
    private final int questsCompleted;
}

@AllArgsConstructor
@Getter
public class Address {
    private final String street;
    private final String city;
    private final String country;
}

// Setup database
DatabaseManager manager = new DatabaseManager();
manager.register("main", DatabaseConfig.builder(DatabaseType.SQLITE)
    .file("database.db")
    .build());

DatabaseClient client = manager.getDefault();
DynamicDatabase db = new DynamicDatabase(client);

// Setup migrations
MigrationManager migrations = new MigrationManager(client);
migrations.init();

// Register tables
DynamicTable<Player> players = db.register(Player.class);
DynamicTable<Quest> quests = db.register(Quest.class);
DynamicTable<PlayerStats> stats = db.register(PlayerStats.class);

// Create data
PlayerStats playerStats = new PlayerStats(
    UUID.randomUUID(), 10, 1500L, 5
);

Address address = new Address(
    "123 Main St", "Springfield", "USA"
);

Player player = new Player(
    UUID.randomUUID(),
    "JohnDoe",
    address,
    new ArrayList<>(),  // Quests loaded lazily
    playerStats
);

players.create(player);  // Cascades to PlayerStats

// Create quests
Quest quest1 = new Quest(
    UUID.randomUUID(),
    "First Quest",
    "Complete the tutorial",
    player
);

quests.create(quest1);

// Query with relationships
Player loaded = players.findById(player.getId()).orElseThrow();

// Embedded fields are loaded
System.out.println(loaded.getAddress().getCity());  // "Springfield"

// Stats are eagerly loaded
System.out.println(loaded.getStats().getLevel());  // 10

// Quests are lazy-loaded
List<Quest> playerQuests = loaded.getQuests();  // Triggers query now
System.out.println(playerQuests.size());  // 1
```

---

## Performance Tips

1. **Use LAZY for Collections:**
   ```java
   @OneToMany(fetch = FetchType.LAZY)  // Don't load unless needed
   ```

2. **Use EAGER for Required Data:**
   ```java
   @ManyToOne(fetch = FetchType.EAGER)  // Always needed
   ```

3. **Limit Cascade Operations:**
   ```java
   @OneToMany(cascade = CascadeType.PERSIST)  // Only cascade saves
   ```

4. **Index Foreign Keys:**
   ```java
   Migration.builder("add_index")
       .addUp("CREATE INDEX idx_player_id ON quests(player_id)")
       .build();
   ```

5. **Monitor Migration Performance:**
   ```java
   for (var m : migrations.getAppliedMigrations()) {
       if (m.getExecutionTimeMs() > 1000) {
           System.out.println("Slow migration: " + m.getVersion());
       }
   }
   ```

---

## Limitations

Current implementation has these limitations:

1. **@ManyToMany:** Not yet supported (requires join tables)
2. **Circular References:** May cause infinite loops with eager loading
3. **Composite Keys:** Only single-field primary keys supported
4. **Query Optimization:** JOIN queries not fully optimized yet

These will be addressed in future versions.

---

## Migration from v1.0.6

If upgrading from v1.0.6, your existing code will continue to work. New features are opt-in:

```java
// Old code still works
DynamicTable<Player> players = db.register(Player.class);
players.create(player);

// New features are optional
@Embedded  // Add when you want embedded objects
@OneToMany // Add when you want relationships
```

No breaking changes!
