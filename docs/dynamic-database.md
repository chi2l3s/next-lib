# Dynamic database usage guide

This document shows how to use the runtime database registration API that ships with NextLib. The feature lets you model your data as plain Java classes and then have NextLib create tables and helper queries on the fly when your plugin boots.

## 1. Describe your data as Java classes

Create an immutable class that lists every column you need. The field names are used for the SQL column names, so prefer concise, lowercase identifiers. By default the first declared field becomes the primary key, but you can mark a different one explicitly with `@PrimaryKey`.

```java
package cloud.nextgentech.nexttraps.types;

import io.github.chi2l3s.nextlib.api.database.dynamic.PrimaryKey;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.UUID;

@AllArgsConstructor
@Getter
@ToString
public class Player {
    @PrimaryKey
    private final UUID playerId;
    private final String nickname;
    private final String trapSkinId;
}
```

### Constructor requirements

The dynamic mapper invokes the constructor that matches all declared fields, in declaration order. Lombok's `@AllArgsConstructor` (as in the example) or a manually written constructor satisfies this requirement. All non-static fields are persisted. Supported field types right now are:

* `String`
* `UUID`
* primitive numeric types and their boxed counterparts (`int`, `long`, `double`, etc.)
* `boolean` / `Boolean`
* `Instant`

## 2. Bootstrap the dynamic database

You can attach the dynamic layer to any `DatabaseClient`. The most common path inside a plugin is to plug in the `DatabaseManager` that your platform exposes:

```java
import io.github.chi2l3s.nextlib.api.database.DatabaseManager;
import io.github.chi2l3s.nextlib.api.database.dynamic.DynamicDatabase;
import io.github.chi2l3s.nextlib.api.database.dynamic.DynamicTable;

public final class NextTrapsPlugin extends JavaPlugin {
    private DynamicTable<Player> players;

    @Override
    public void onEnable() {
        DatabaseManager manager = getService(DatabaseManager.class); // however you obtain it

        DynamicDatabase database = DynamicDatabase.using(manager);
        // Table name defaults to the simple class name converted to snake_case and pluralised ("Player" -> "players")
        players = database.register(Player.class);
    }
}
```

If you prefer a custom table name, call `database.register("player_profiles", Player.class)` instead.

The first time you register a class the library will create the backing table automatically (using `CREATE TABLE IF NOT EXISTS`).

## 3. Run queries with the fluent helpers

Every registered table exposes helper builders for the basic CRUD operations.

### Insert rows

```java
players.create(new Player(playerId, nickname, "default"));
```

### Fetch a single row

```java
Optional<Player> found = players.findFirst()
    .where("nickname", "chi2l3s")
    .execute();
```

The `where` clauses reference the Java field names, not SQL column names. All comparisons are equality checks; passing `null` produces `IS NULL` in SQL.

### Fetch multiple rows

```java
List<Player> neonUsers = players.findMany()
    .where("trapSkinId", "neon")
    .execute();
```

You can chain multiple `where` calls to AND the predicates together.

### Update existing rows

```java
int updated = players.update()
    .set("trapSkinId", "midnight")
    .where("playerId", playerId)
    .execute();
```

Every `set` and `where` argument uses the entity field names. Fields that you do not mention remain untouched.

## 4. Putting it together

Below is a minimal end-to-end example that registers the `Player` entity when your plugin starts and then performs a few operations in response to commands or events.

```java
public final class NextTrapsPlugin extends JavaPlugin {
    private DynamicTable<Player> players;

    @Override
    public void onEnable() {
        DatabaseManager manager = getService(DatabaseManager.class);
        DynamicDatabase database = DynamicDatabase.using(manager);
        players = database.register(Player.class);

        // seed one record on first boot
        players.create(new Player(UUID.randomUUID(), "chi2l3s", "default"));
    }

    public Optional<Player> lookupByNickname(String nickname) {
        return players.findFirst()
            .where("nickname", nickname)
            .execute();
    }

    public List<Player> listWithSkin(String skinId) {
        return players.findMany()
            .where("trapSkinId", skinId)
            .execute();
    }

    public int changeSkin(UUID playerId, String newSkin) {
        return players.update()
            .set("trapSkinId", newSkin)
            .where("playerId", playerId)
            .execute();
    }
}
```

This pattern scales to any number of entity classes. Register each class once during startup and reuse the returned `DynamicTable` instance wherever you need to interact with the database.

## 5. Connection pooling with HikariCP

`DatabaseManager` now powers every registered client with a dedicated [HikariCP](https://github.com/brettwooldridge/HikariCP) pool. The integration keeps your code unchanged—`DynamicDatabase.using(manager)` simply reuses the pooled connections under the hood—but you can still tweak pool behaviour through `DatabaseConfig` properties when you register a client.

```java
DatabaseConfig mysqlConfig = DatabaseConfig.builder(DatabaseType.MYSQL)
        .host("127.0.0.1")
        .port(3306)
        .database("nexttraps")
        .username("next")
        .password("secret")
        // HikariCP overrides
        .property("maximumPoolSize", "20")
        .property("minimumIdle", "5")
        .property("cachePrepStmts", "true")
        .build();

DatabaseManager manager = new DatabaseManager();
manager.register("mysql", mysqlConfig);
```

Pool-oriented keys such as `maximumPoolSize`, `minimumIdle`, `connectionTimeout`, or `idleTimeout` map directly onto the corresponding Hikari setters. Any other properties are forwarded to the underlying JDBC driver (for example MySQL's `cachePrepStmts`). This gives you fast connection reuse out of the box while still letting you fine-tune performance for production deployments.
