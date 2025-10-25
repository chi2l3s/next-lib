# NextLib

[![](https://jitpack.io/v/chi2l3s/next-lib.svg)](https://jitpack.io/#chi2l3s/next-lib)
[![CodeFactor](https://www.codefactor.io/repository/github/chi2l3s/next-lib/badge)](https://www.codefactor.io/repository/github/chi2l3s/next-lib)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
![Java](https://img.shields.io/badge/Java-17-blue)
![Gradle](https://img.shields.io/badge/Gradle-8.7-green)
[![JitPack Downloads](https://jitpack.io/v/chi2l3s/next-lib/month.svg)](https://jitpack.io/#chi2l3s/next-lib)
![GitHub last commit](https://img.shields.io/github/last-commit/chi2l3s/next-lib)
![GitHub issues](https://img.shields.io/github/issues/chi2l3s/next-lib)
![GitHub pull requests](https://img.shields.io/github/issues-pr/chi2l3s/next-lib)

**NextLib** — это лёгкая библиотека для разработки Minecraft-плагинов на Paper/Spigot.
Она упрощает работу с командами, предметами, цветами и GUI-меню, включая загрузку меню из YAML файлов.

---

## Установка

### Gradle

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.chi2l3s:next-lib:1.0.0'
}
```

### Maven

```xml
<repositories>
    <repository>
        <id>jitpack</id>
        <url>https://jitpack.io/</url>
    </repository>
</repositories>

<dependency>
<groupId>com.github.chi2l3s</groupId>
<artifactId>next-lib</artifactId>
<version>1.0.0</version>
<scope>provided</scope>
</dependency>
```

---

## Возможности

* Color API — поддержка HEX и `&` цветовых кодов.
* Command API — система сабкоманд с автодополнением.
* Item API — быстрый билдер предметов (имя, лор, PDC, скины).
* Config Manager — удобная работа с YAML конфигами.
* GUI API — создание меню через YAML файлы в папке `menus/`.
* Database Manager — подключение к MySQL/PostgreSQL/SQLite и генератор репозиториев по YAML-схеме.

---

## Color API

```java
import ru.amixoldev.nextlib.color.ColorUtil;
import ru.amixoldev.nextlib.color.ColorUtilImpl;

ColorUtil color = new ColorUtilImpl();

player.sendMessage(color.formatMessage("&aHello &bWorld &#FF0000!"));
```

Поддерживает HEX (`&#RRGGBB`) и стандартные `&` коды.

---

## Command API

```java
public class ExampleCommand extends LongCommandExecutor {

    public ExampleCommand() {
        addSubCommand(new SubCommand() {
            @Override
            public void onExecute(CommandSender sender, String[] args) {
                sender.sendMessage("Hello from subcommand!");
            }

            @Override
            public List<String> onTabComplete(CommandSender sender, String[] args) {
                return List.of("one", "two", "three");
            }
        }, new String[]{"test", "t"}, new Permission("example.use"));
    }
}
```

Регистрируй команду в `onEnable()`:

```java
getCommand("example").setExecutor(new ExampleCommand());
```

---

## Item API

```java
ItemStack item = new ItemBuilder(Material.DIAMOND_SWORD)
        .setName("&bМеч силы")
        .setLore(List.of("&7Очень острый меч", "&e+10 к силе"))
        .addPersistentTag("power", PersistentDataType.INTEGER, 10)
        .build();
```

Поддержка:

* имени (`setName`)
* лора (`setLore`)
* `PersistentDataContainer` (`addPersistentTag`)
* скинов (`setSkullOwner`)

---

## Config Manager

```java
Config config = new Config(this);
config.reloadConfig();

String prefix = config.getPrefix();
int countdown = config.getCountdown();
```

Поддержка:

* Автосоздание конфига, если он отсутствует.
* Парсинг в Java-поля.
* Автоматическое обновление с помощью `ConfigUpdater`.

---

## GUI API

Меню хранятся в папке `plugins/YourPlugin/menus/`.
Каждый `.yml` файл = отдельное меню.

### main.yml

```yaml
id: "main"
title: "&0Главное меню"
size: 54
items:
  close:
    material: BARRIER
    slot: 53
    name: "&cЗакрыть"
    onLeftClick:
      - "close"

  diamond:
    material: DIAMOND
    slot: 0
    amount: 1
    name: "&bНаграда"
    lore:
      - "Нажмите, чтобы получить"
    onLeftClick:
      - "console give %player% diamond 64"
      - "message &aВы получили награду!"
      - "opengui shop"
```

### shop.yml

```yaml
id: "shop"
title: "&aМагазин"
size: 27
items:
  back:
    material: ARROW
    slot: 26
    name: "&7Назад"
    onLeftClick:
      - "opengui main"
```

### Использование в плагине

```java
GuiManager guiManager;

@Override
public void onEnable() {
    guiManager = new GuiManager(this);

    // Загружаем все меню из папки /menus
    File menusFolder = new File(getDataFolder(), "menus");
    guiManager.loadFromFolder(menusFolder);

    // Команда для открытия меню
    getCommand("menu").setExecutor((sender, cmd, label, args) -> {
        if (sender instanceof Player player) {
            String id = args.length > 0 ? args[0] : "main";
            guiManager.openGui(player, id);
        }
        return true;
    });
}
```

---

## Доступные действия в GUI

* `close` — закрыть меню.
* `command <cmd>` — выполнить команду от имени игрока.
* `console <cmd>` — выполнить команду от имени консоли.
* `message <msg>` — отправить сообщение игроку.
* `opengui <id>` — открыть другое меню.

### Регистрация собственных действий

Через `Actions.registerAction(name, factory)` можно добавить новое действие из любого другого плагина.

```java
import io.github.chi2l3s.nextlib.api.gui.Actions;
import io.github.chi2l3s.nextlib.api.gui.GuiManager;
import io.github.chi2l3s.nextlib.api.gui.GuiAction;
import org.bukkit.Location;

@Override
public void onEnable() {
    GuiManager guiManager = new GuiManager(this);

    Actions.registerAction("teleport", (manager, args) -> createTeleportAction(args));
}

private GuiAction createTeleportAction(String args) {
    String[] parts = args.split(" ");
    double x = Double.parseDouble(parts[0]);
    double y = Double.parseDouble(parts[1]);
    double z = Double.parseDouble(parts[2]);

    return player -> player.teleport(new Location(player.getWorld(), x, y, z));
}
```

В YAML меню можно использовать строку `teleport 0 80 0`, и зарегистрированная фабрика получит аргументы после названия действия.

Используйте `GuiManager`, переданный в фабрику, если действию нужно взаимодействовать с другими GUI (`manager.openGui(...)`).

---

## Database Manager & Schema Generator

Новая подсистема баз данных объединяет управление подключениями и автогенерацию Java-кода из YAML-схемы по аналогии с Prisma.

### Регистрация подключений

```java
import io.github.chi2l3s.nextlib.api.database.DatabaseConfig;
import io.github.chi2l3s.nextlib.api.database.DatabaseManager;
import io.github.chi2l3s.nextlib.api.database.DatabaseType;

DatabaseManager databases = new DatabaseManager();
databases.register("main", DatabaseConfig.builder(DatabaseType.MYSQL)
        .host("127.0.0.1")
        .port(3306)
        .database("nextlib")
        .username("user")
        .password("secret")
        .build());

// Поддерживаются также DatabaseType.POSTGRESQL и DatabaseType.SQLITE (через .file("plugins/MyPlugin/data.db"))
```

`DatabaseManager` возвращает `DatabaseClient`, который предоставляет вспомогательные методы `query`, `queryOne`, `execute` и `executeBatch` поверх `PreparedStatement`.

### Описание схемы и генерация

Создайте YAML-файл `database.schema.yml`:

```yaml
packageName: io.github.example.generated
datasource: main
tables:
  - name: User
    tableName: users
    fields:
      - name: id
        type: UUID
        id: true
      - name: username
        type: STRING
      - name: coins
        type: INT
      - name: createdAt
        type: TIMESTAMP
```

Запустите генератор в Gradle-задаче или в отдельном `main`:

```java
import io.github.chi2l3s.nextlib.api.database.schema.SchemaGenerator;

new SchemaGenerator().generate(
        Path.of("src/main/resources/database.schema.yml"),
        Path.of("build/generated/sources/nextlib"));
```

Подключите папку сгенерированных классов к `sourceSets` Gradle:

```gradle
sourceSets.main.java.srcDir("build/generated/sources/nextlib")
```

### Использование сгенерированных репозиториев

Генератор создаст `UserRecord` и `UserRepository` с методами `insert`, `findById`, `findAll`, `update`, `deleteById`:

```java
import io.github.example.generated.UserRecord;
import io.github.example.generated.UserRepository;

UserRepository users = UserRepository.using(databases);

users.insert(new UserRecord(
        UUID.randomUUID(),
        "Notch",
        100,
        Instant.now()
));

Optional<UserRecord> notch = users.findById(playerUuid);
notch.ifPresent(record -> {
    getLogger().info("Баланс игрока: " + record.getCoins());
});
```

Весь SQL генерируется автоматически, а репозитории используют именованный источник `datasource` из схемы, поэтому достаточно один раз зарегистрировать подключение.

---

## Roadmap

* [x] Command API
* [x] Item API
* [x] Color API
* [x] Config Manager
* [x] GUI API с конфигами
* [x] GUI Action Registry (кастомные действия)
* [ ] SQL/Redis API
* [ ] Event Utilities

---

## Лицензия

MIT License © 2025 NextGenTech

---