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

**NextLib** — это модульная библиотека для плагинов Paper/Spigot, которая закрывает базовые задачи разработки: работу с конфигами, GUI, предметами и базами данных. Все модули ориентированы на декларативный стиль и удобную интеграцию в существующие проекты.

---

## Содержание

1. [Особенности](#особенности)
2. [Установка](#установка)
3. [Быстрый старт](#быстрый-старт)
4. [Основные модули](#основные-модули)
5. [Динамическая база данных](#динамическая-база-данных)
6. [GUI API и условия](#gui-api-и-условия)
7. [Работа с конфигами](#работа-с-конфигами)
8. [Полезные ссылки](#полезные-ссылки)
9. [Roadmap](#roadmap)
10. [Лицензия](#лицензия)

---

## Особенности

* **Динамическая база данных** — описывайте сущности через обычные Java-классы и аннотации, а библиотека сама создаёт таблицы и даёт удобный Fluent API для CRUD-операций.
* **Подключение к базе через HikariCP** — готовый пул соединений с настраиваемыми параметрами.
* **Гибкое GUI** — загрузка меню из YAML, условия для отображения и встроенные действия (`update`, `playsound`, `command`, `opengui` и др.).
* **Command API** — дерево сабкоманд с автодополнением.
* **Item API** — лаконичные билдеры предметов с поддержкой PDC, названий, лора и голов.
* **Color API** — форматирование HEX и `&` кодов без лишнего кода.
* **Config Manager** — декларативная загрузка YAML конфигов в Java-объекты.

---

## Установка

Добавьте JitPack-репозиторий и зависимость `1.0.5` в ваш build-скрипт.

### Gradle (Kotlin DSL)

```kotlin
repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("com.github.chi2l3s:next-lib:1.0.5")
}
```

### Gradle (Groovy DSL)

```gradle
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.chi2l3s:next-lib:1.0.5'
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
    <version>1.0.5</version>
    <scope>provided</scope>
</dependency>
```

---

## Быстрый старт

1. Скачайте библиотеку через JitPack и добавьте её как зависимость.
2. Создайте экземпляр `GuiManager` и загрузите меню из папки `menus/`.
3. Опишите сущности базы данных Java-классами, аннотируйте первичный ключ `@PrimaryKey` и зарегистрируйте их в `DynamicDatabase`.
4. Используйте предоставленные API для команд, предметов и конфигураций — весь функционал доступен из пространства имён `io.github.chi2l3s.nextlib.api`.

```java
public final class NextTrapsPlugin extends JavaPlugin {
    private DynamicDatabase database;
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        database = DatabaseManager.createDynamicDatabase(this, "jdbc:mysql://localhost:3306/nexttraps", config -> {
            config.setUsername("root");
            config.setPassword("password");
            config.setMaximumPoolSize(10);
        });

        database.register(PlayerEntity.class);

        guiManager = new GuiManager(this);
        guiManager.loadFromFolder(new File(getDataFolder(), "menus"));
    }
}
```

---

## Основные модули

### Command API

* Структурируйте команды через сабкоманды и permissions.
* Поддержка автодополнения и алиасов.
* Подключается одной строкой в `onEnable()`.

```java
getCommand("nextlib").setExecutor(new RootCommand());
```

### Item API

* Флюент-билдеры для `ItemStack` с настройкой имени, описания, флагов и `PersistentDataContainer`.
* Поддержка установки владельца головы и массовых изменений меты.

```java
ItemStack reward = new ItemBuilder(Material.DIAMOND)
        .setName("&bНаграда дня")
        .setLore(List.of("&7Нажми, чтобы получить"))
        .addPersistentTag("reward", PersistentDataType.STRING, "daily")
        .glow()
        .build();
```

### Color API

* Единый метод форматирования, который переводит `&` и HEX (`&#RRGGBB`) в цветные сообщения.

```java
player.sendMessage(color.format("&aДобро пожаловать в &#3498dbNextLib"));
```

### Config Manager

* Базовый класс `BaseConfig` автоматически создаёт и обновляет YAML-файлы.
* Данные загружаются в Java-поля или DTO.

---

## Динамическая база данных

* Определяйте сущность привычным Java-классом.
* Аннотация `@PrimaryKey` помечает поле первичного ключа.
* `DynamicTable` предоставляет методы `findFirst`, `findMany`, `create`, `update` и `delete`.

```java
@AllArgsConstructor
@Getter
public class PlayerEntity {
    @PrimaryKey
    private final UUID playerId;
    private final String nickname;
    private final String trapSkinId;
}

DynamicTable<PlayerEntity> players = database.table(PlayerEntity.class);

String trapSkinId = players.findFirst()
        .where("playerId", playerId)
        .execute()
        .map(PlayerEntity::getTrapSkinId)
        .orElse("fallback");
```

Подробнее — в отдельном руководстве [`docs/dynamic-database.md`](docs/dynamic-database.md).

---

## GUI API и условия

* Меню описываются YAML-файлами в `plugins/<ВашПлагин>/menus`.
* Поле `slot` принимает одиночное значение, а `slots` — список произвольных слотов или диапазонов `A-B`.
* Можно регистрировать собственные условия (`Conditions#register`) и использовать их для подсветки предметов или ограничения взаимодействия.
* Встроенные действия: `close`, `command`, `console`, `message`, `opengui`, `update`, `playsound`.

```yaml
id: traps
title: "&8Выбор ловушки"
size: 54
items:
  back:
    material: ARROW
    slot: 53
    name: "&7Назад"
    onLeftClick:
      - "opengui main"
  trap:
    material: TRIPWIRE_HOOK
    slots:
      - 0-8
      - 18
    name: "&b%trap_name%"
    lore:
      - "&7Стоимость: &e%price%"
    enchanted_when:
      - "selected"
    onLeftClick:
      - "update"
      - "playsound ENTITY_ENDER_DRAGON_AMBIENT 0.7 1.2"
```

Подробное руководство и примеры — в [`docs/gui-conditions.md`](docs/gui-conditions.md).

---

## Работа с конфигами

* Наследуйтесь от `BaseConfig`, чтобы получить автоматическое создание и обновление файлов.
* Используйте `loadValues()` для чтения данных и связывайте их с объектами вашего домена.

```java
public class TrapSkinsConfig extends BaseConfig {
    @Override
    protected void loadValues() {
        ConfigurationSection skins = config.getConfigurationSection("skins");
        // Преобразуйте YAML в ваши объекты TrapSkin
    }
}
```

---

## Полезные ссылки

* [Релизные заметки NextLib v1.0.5](docs/releases/NextLib-v1.0.5.md)
* [Руководство по динамической базе данных](docs/dynamic-database.md)
* [Руководство по GUI-условиям и действиям](docs/gui-conditions.md)

---

## Roadmap

- [x] Command API
- [x] Item API
- [x] Color API
- [x] Config Manager
- [x] GUI API с условиями и слот-диапазонами
- [x] Динамическая база данных с HikariCP
- [ ] Расширяемый реестр действий GUI
- [ ] Redis/Message Queue интеграции
- [ ] Утилиты для работы с событиями

---

## Лицензия

MIT License © 2025 NextGenTech

---
