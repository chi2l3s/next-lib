# GUI conditions quickstart

This guide shows how to attach conditional behaviour to GUI items and reuse the
same logic across different menus. A condition is simply a named predicate
registered during plugin start. Menu definitions can then reference the name in
`enchanted_when` (or any future condition-aware property) to control how each
item renders for a specific player.

## 1. Register your condition

Register conditions inside your plugin bootstrap. Each condition receives the
`GuiManager` and a raw argument string, and must produce a `GuiCondition`
predicate that runs for every viewer.

```java
public final class NextTrapsPlugin extends JavaPlugin {
    private TrapSelections trapSelections;

    @Override
    public void onEnable() {
        DatabaseManager databaseManager = resolveDatabaseManager();
        trapSelections = new TrapSelections(databaseManager);

        Conditions.registerCondition("selected", (manager, rawArgs) -> {
            String trapId = rawArgs.trim();
            if (trapId.isEmpty()) {
                throw new IllegalArgumentException("selected condition expects trap id");
            }

            return player -> trapSelections.isSelected(player.getUniqueId(), trapId);
        });
    }

    private DatabaseManager resolveDatabaseManager() {
        // Fetch from your platform's service locator or dependency injector
        return getService(DatabaseManager.class);
    }
}
```

Two helpers (`always` and `never`) are registered by default for quick testing.

## 2. Back conditions with your database

The predicate can use any data source. The snippet above delegates to
`TrapSelections`, which in turn uses NextLib's dynamic database module. The
following service keeps track of the skin that each user selected.

```java
public final class TrapSelections {
    private final DynamicTable<PlayerSelection> table;

    public TrapSelections(DatabaseManager databaseManager) {
        DynamicDatabase database = DynamicDatabase.using(databaseManager);
        this.table = database.register("trap_selections", PlayerSelection.class);
    }

    public boolean isSelected(UUID playerId, String trapId) {
        return table.findFirst()
                .where("playerId", playerId)
                .where("trapId", trapId)
                .execute()
                .isPresent();
    }

    public void select(UUID playerId, String trapId) {
        int updated = table.update()
                .set("trapId", trapId)
                .where("playerId", playerId)
                .execute();

        if (updated == 0) {
            table.create(new PlayerSelection(playerId, trapId));
        }
    }
}
```

And the entity used by the table:

```java
@AllArgsConstructor
@Getter
public final class PlayerSelection {
    @PrimaryKey
    private final UUID playerId;
    private final String trapId;
}
```

The dynamic database guide (`docs/dynamic-database.md`) contains a deeper dive
into `DynamicDatabase`/`DynamicTable`.

## 3. Reference the condition in GUI YAML

Conditions are specified as raw strings. The first token identifies the
condition name, subsequent text is passed to the factory as the argument. With
our `selected` registration, the player's selected trap item glows when the
predicate evaluates to `true`.

Each item can target a single slot via `slot`, or multiple positions by
providing a `slots` list. The list accepts individual numbers or ranges like
`0-8`.

```yaml
items:
  poison_trap:
    material: POTION
    slot: 10
    name: "&aPoison trap"
    enchanted_when:
      - "selected poison"
  fire_trap:
    material: BLAZE_POWDER
    slots:
      - 11
      - 20-24
    name: "&cFire trap"
    enchanted_when:
      - "selected fire"
```

When a player opens the menu, `GuiItem#createItemFor` runs every configured
condition until one returns `true`. When that happens the item receives a hidden
`DURABILITY` enchantment, giving the familiar enchanted glow.

## 4. Combine multiple predicates

You may supply multiple condition names in the list. The first predicate that
returns `true` wins, so you can chain fallbacks:

```yaml
enchanted_when:
  - "selected poison"
  - "always"
```

This makes the item glow for everyone, but keeps the intent explicit by
highlighting the primary condition you care about.

Whenever your condition mutates player state (for example after calling
`TrapSelections#select`), trigger the built-in `update` action to close and
re-open the current menu on the next tick:

```yaml
on_left_click:
  - "command traps select poison"
  - "update"
```

Combined with conditional glows, this gives immediate visual feedback after a
selection is saved to the database.
