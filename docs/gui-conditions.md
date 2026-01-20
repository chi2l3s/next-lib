# GUI API и условия - Руководство

## Обзор

GUI API NextLib позволяет создавать интерактивные меню для игроков через декларативные YAML-файлы. Система поддерживает динамическое отображение предметов, условия видимости, плейсхолдеры и встроенные действия.

## Быстрый старт

### 1. Инициализация GuiManager

```java
public class MyPlugin extends JavaPlugin {
    private GuiManager guiManager;

    @Override
    public void onEnable() {
        guiManager = new GuiManager(this);
        guiManager.loadFromFolder(new File(getDataFolder(), "menus"));
    }
}
```

### 2. Структура папок

```
plugins/
  MyPlugin/
    menus/
      main.yml      # Главное меню
      shops.yml     # Меню магазина
      settings.yml  # Настройки
```

### 3. Базовый YAML-файл меню

```yaml
# menus/main.yml
id: main
title: "&8Главное меню"
size: 27

items:
  decoration:
    material: BLACK_STAINED_GLASS_PANE
    name: " "
    slots:
      - 0-8
      - 18-26

  shop:
    material: EMERALD
    slot: 11
    name: "&a&lМагазин"
    lore:
      - "&7Купи предметы"
      - "&7за игровую валюту"
    onLeftClick:
      - "opengui shops"
      - "playsound BLOCK_NOTE_BLOCK_PLING 1.0 1.0"

  settings:
    material: REDSTONE
    slot: 13
    name: "&c&lНастройки"
    lore:
      - "&7Измени параметры игры"
    onLeftClick:
      - "opengui settings"

  close:
    material: BARRIER
    slot: 15
    name: "&cЗакрыть"
    onLeftClick:
      - "close"
```

## Структура YAML

### Основные параметры меню

```yaml
id: unique_id          # Уникальный идентификатор меню
title: "&8Заголовок"   # Заголовок инвентаря (поддерживает цвета)
size: 54               # Размер инвентаря (9, 18, 27, 36, 45, 54)
items:                 # Список предметов
  # ...
```

### Параметры предмета

```yaml
item_name:
  material: DIAMOND           # Материал предмета
  amount: 1                   # Количество (опционально, по умолчанию 1)
  slot: 10                    # Одиночный слот
  slots:                      # Или множественные слоты
    - 0-8                     # Диапазон слотов
    - 18                      # Одиночный слот
    - 27-35                   # Ещё один диапазон

  name: "&bИмя предмета"      # Имя предмета
  lore:                       # Описание предмета
    - "&7Первая строка"
    - "&7Вторая строка"
    - ""
    - "&eНажмите для действия"

  # Условия
  visible_when:               # Когда предмет видим
    - "has_permission"
  hidden_when:                # Когда предмет скрыт
    - "is_banned"
  enchanted_when:             # Когда предмет зачарован (светится)
    - "is_selected"

  # Действия
  onLeftClick:                # При левом клике
    - "command say Hello"
  onRightClick:               # При правом клике
    - "console give %player% diamond 1"
  onShiftLeftClick:           # При Shift + левый клик
    - "message &aТы использовал Shift!"
  onShiftRightClick:          # При Shift + правый клик
    - "close"
```

## Слоты

### Одиночный слот

```yaml
item:
  slot: 13  # Центральный слот в инвентаре 27
```

### Множественные слоты

```yaml
item:
  slots:
    - 0      # Один слот
    - 2      # Ещё один слот
    - 5-8    # Диапазон слотов (5, 6, 7, 8)
```

### Примеры распределения слотов

```yaml
# Рамка вокруг инвентаря 27
decoration:
  slots:
    - 0-8    # Верхняя строка
    - 9      # Левый край
    - 17     # Правый край
    - 18-26  # Нижняя строка

# Центральная линия
items:
  slots:
    - 10
    - 13
    - 16
```

## Условия (Conditions)

Условия позволяют динамически изменять видимость и внешний вид предметов на основе состояния игрока.

### Встроенные условия

По умолчанию условия не реализованы - вы регистрируете их в своём плагине:

```java
public class MyPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        // Регистрация условия
        Conditions.register("has_permission", (player, args) -> {
            String permission = args.isEmpty() ? "myplugin.use" : args.get(0);
            return player.hasPermission(permission);
        });

        Conditions.register("has_money", (player, args) -> {
            double required = args.isEmpty() ? 100 : Double.parseDouble(args.get(0));
            return getEconomy().getBalance(player) >= required;
        });

        Conditions.register("is_premium", (player, args) -> {
            return playerData.isPremium(player.getUniqueId());
        });
    }
}
```

### Использование условий

```yaml
premium_item:
  material: GOLD_BLOCK
  slot: 13
  name: "&6&lПремиум предмет"
  visible_when:
    - "is_premium"       # Видим только премиум игрокам
  onLeftClick:
    - "command premium shop"

locked_item:
  material: IRON_BARS
  slot: 14
  name: "&c&lЗаблокировано"
  visible_when:
    - "!has_permission myplugin.unlock"  # ! инвертирует условие
  lore:
    - "&7Требуется разрешение"

unlocked_item:
  material: DIAMOND_BLOCK
  slot: 14
  name: "&a&lРазблокировано"
  visible_when:
    - "has_permission myplugin.unlock"
  onLeftClick:
    - "command unlock special"
```

### Типы условий

#### visible_when
Предмет показывается только если условие истинно:

```yaml
item:
  visible_when:
    - "has_permission admin.panel"
    - "is_premium"  # AND - все условия должны быть истинны
```

#### hidden_when
Предмет скрывается если условие истинно:

```yaml
item:
  hidden_when:
    - "is_banned"
    - "in_combat"
```

#### enchanted_when
Предмет светится (имеет чары) если условие истинно:

```yaml
selected_skin:
  material: TRIPWIRE_HOOK
  slot: 10
  name: "&bЛедяная ловушка"
  enchanted_when:
    - "has_skin ice_trap"  # Светится если выбран этот скин
```

### Инверсия условий

Используйте `!` для инверсии:

```yaml
item:
  visible_when:
    - "!is_banned"        # НЕ забанен
    - "!in_cooldown"      # НЕ в кулдауне
```

### Условия с аргументами

```java
// Регистрация
Conditions.register("has_level", (player, args) -> {
    int required = Integer.parseInt(args.get(0));
    return player.getLevel() >= required;
});

Conditions.register("owns_item", (player, args) -> {
    String itemId = args.get(0);
    int amount = args.size() > 1 ? Integer.parseInt(args.get(1)) : 1;
    return playerInventory.hasItem(player, itemId, amount);
});
```

```yaml
item:
  visible_when:
    - "has_level 50"           # Уровень >= 50
    - "owns_item diamond 64"   # Имеет 64 алмаза
```

## Действия (Actions)

### Встроенные действия

#### close
Закрывает текущее меню:

```yaml
item:
  onLeftClick:
    - "close"
```

#### command
Выполняет команду от имени игрока:

```yaml
item:
  onLeftClick:
    - "command say Hello World"
    - "command spawn"
```

#### console
Выполняет команду от имени консоли:

```yaml
item:
  onLeftClick:
    - "console give %player% diamond 10"
    - "console eco give %player% 1000"
```

#### message
Отправляет сообщение игроку:

```yaml
item:
  onLeftClick:
    - "message &aТы получил награду!"
    - "message &7Спасибо за игру"
```

#### opengui
Открывает другое меню:

```yaml
item:
  onLeftClick:
    - "opengui shops"
    - "playsound BLOCK_NOTE_BLOCK_PLING 1.0 1.0"
```

#### update
Обновляет текущее меню (перезагружает предметы):

```yaml
item:
  onLeftClick:
    - "update"  # Полезно после изменения состояния игрока
```

#### playsound
Воспроизводит звук для игрока:

```yaml
item:
  onLeftClick:
    - "playsound ENTITY_EXPERIENCE_ORB_PICKUP 1.0 1.0"
    - "playsound BLOCK_NOTE_BLOCK_PLING 0.5 2.0"

# Формат: playsound <sound> <volume> <pitch>
# volume: 0.0 - 1.0 (громкость)
# pitch: 0.5 - 2.0 (высота тона)
```

### Множественные действия

Действия выполняются последовательно:

```yaml
item:
  onLeftClick:
    - "console give %player% diamond 10"
    - "message &aТы получил 10 алмазов!"
    - "playsound ENTITY_EXPERIENCE_ORB_PICKUP 1.0 1.0"
    - "close"
```

### Регистрация своих действий

```java
GuiAction.register("teleport", (player, args) -> {
    if (args.isEmpty()) {
        player.sendMessage("§cНе указаны координаты!");
        return;
    }
    String[] coords = args.get(0).split(",");
    double x = Double.parseDouble(coords[0]);
    double y = Double.parseDouble(coords[1]);
    double z = Double.parseDouble(coords[2]);
    Location loc = new Location(player.getWorld(), x, y, z);
    player.teleport(loc);
});

GuiAction.register("buyskin", (player, args) -> {
    String skinId = args.get(0);
    int price = Integer.parseInt(args.get(1));

    if (economy.getBalance(player) < price) {
        player.sendMessage("§cНедостаточно средств!");
        return;
    }

    economy.withdrawPlayer(player, price);
    skinManager.unlockSkin(player, skinId);
    player.sendMessage("§aТы купил скин: " + skinId);
});
```

Использование:

```yaml
teleport_spawn:
  material: ENDER_PEARL
  slot: 10
  name: "&bТелепорт на спавн"
  onLeftClick:
    - "teleport 0,100,0"

buy_skin:
  material: LEATHER_CHESTPLATE
  slot: 11
  name: "&aЛедяной скин"
  lore:
    - "&7Цена: &e1000 монет"
  onLeftClick:
    - "buyskin ice_skin 1000"
    - "update"
```

## Плейсхолдеры

### PlaceholderAPI интеграция

NextLib автоматически поддерживает PlaceholderAPI:

```yaml
item:
  name: "&bПривет, %player_name%"
  lore:
    - "&7Уровень: &e%player_level%"
    - "&7Здоровье: &c%player_health%/20"
    - "&7Деньги: &a$%vault_eco_balance%"
```

### Кастомные плейсхолдеры

Для своих плейсхолдеров создайте PlaceholderExpansion:

```java
public class MyExpansion extends PlaceholderExpansion {
    @Override
    public String getIdentifier() {
        return "myplugin";
    }

    @Override
    public String getAuthor() {
        return "YourName";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";

        if (identifier.equals("coins")) {
            return String.valueOf(playerData.getCoins(player));
        }

        if (identifier.equals("skin")) {
            return skinManager.getCurrentSkin(player);
        }

        return null;
    }
}
```

Использование:

```yaml
item:
  name: "&bТвои монеты: &e%myplugin_coins%"
  lore:
    - "&7Текущий скин: &a%myplugin_skin%"
```

## Продвинутые примеры

### Пагинация (страницы)

```yaml
# menus/skins_page1.yml
id: skins_page1
title: "&8Скины - Страница 1"
size: 54

items:
  skin_1:
    material: TRIPWIRE_HOOK
    slot: 10
    name: "&bЛедяная ловушка"
    enchanted_when:
      - "has_skin ice_trap"
    onLeftClick:
      - "console selectskin %player% ice_trap"
      - "update"

  # ... остальные скины ...

  next_page:
    material: ARROW
    slot: 53
    name: "&aСледующая страница"
    onLeftClick:
      - "opengui skins_page2"

  previous_page:
    material: ARROW
    slot: 45
    name: "&cПредыдущая страница"
    hidden_when:
      - "true"  # Скрыт на первой странице
```

### Подтверждающие диалоги

```yaml
# menus/confirm_purchase.yml
id: confirm_purchase
title: "&8Подтвердить покупку?"
size: 27

items:
  info:
    material: GOLD_BLOCK
    slot: 13
    name: "&ePремиум статус"
    lore:
      - "&7Цена: &a$1000"
      - ""
      - "&7Подтвердить покупку?"

  confirm:
    material: LIME_WOOL
    slot: 11
    name: "&a&lПОДТВЕРДИТЬ"
    onLeftClick:
      - "console buypremium %player%"
      - "message &aПокупка успешна!"
      - "close"

  cancel:
    material: RED_WOOL
    slot: 15
    name: "&c&lОТМЕНИТЬ"
    onLeftClick:
      - "opengui shop"
```

### Динамическое меню с условиями

```yaml
# menus/dynamic_shop.yml
id: dynamic_shop
title: "&8Магазин"
size: 36

items:
  # Предмет для обычных игроков
  basic_sword:
    material: IRON_SWORD
    slot: 10
    name: "&7Железный меч"
    lore:
      - "&7Цена: &e100 монет"
    visible_when:
      - "!is_premium"
    onLeftClick:
      - "buyitem basic_sword 100"

  # Предмет для премиум игроков (со скидкой)
  premium_sword:
    material: DIAMOND_SWORD
    slot: 10
    name: "&bАлмазный меч &7(Premium)"
    lore:
      - "&7Цена: &e50 монет &m100"
      - "&aPремиум скидка 50%!"
    visible_when:
      - "is_premium"
    enchanted_when:
      - "is_premium"
    onLeftClick:
      - "buyitem premium_sword 50"

  # Заблокированный предмет
  locked:
    material: BARRIER
    slot: 11
    name: "&cЗаблокировано"
    lore:
      - "&7Требуется уровень 50"
    visible_when:
      - "!has_level 50"

  unlocked:
    material: NETHERITE_SWORD
    slot: 11
    name: "&5Незеритовый меч"
    lore:
      - "&7Цена: &e500 монет"
    visible_when:
      - "has_level 50"
    onLeftClick:
      - "buyitem netherite_sword 500"
```

## Методы GuiManager

### Открытие меню программно

```java
guiManager.openGui(player, "main");
```

### Обновление меню

```java
guiManager.refresh(player);
```

### Перезагрузка всех меню

```java
guiManager.reloadAll();
// Автоматически закрывает все открытые меню
```

### Создание GUI программно

```java
Gui customGui = guiManager.createGui("custom", "&8Кастомное меню", 27);

GuiItem item = new GuiItem(new ItemStack(Material.DIAMOND));
item.setName("&bАлмаз");
item.setLore(Arrays.asList("&7Нажми меня!"));
item.setClickAction(ClickType.LEFT, player -> {
    player.sendMessage("§aТы нажал на алмаз!");
});

customGui.setItem(13, item);
customGui.open(player);
```

## Best Practices

### 1. Организация файлов

```
menus/
  main.yml              # Главное меню
  shops/
    items.yml           # Магазин предметов
    skins.yml           # Магазин скинов
  admin/
    panel.yml           # Админ панель
    moderation.yml      # Модерация
```

### 2. Переиспользование

Создайте шаблоны для повторяющихся элементов:

```yaml
# Декорация (используйте во всех меню)
decoration:
  material: BLACK_STAINED_GLASS_PANE
  name: " "
  slots:
    - 0-8
    - 18-26

# Кнопка назад (используйте во всех подменю)
back:
  material: ARROW
  slot: 45
  name: "&7Назад"
  onLeftClick:
    - "opengui main"
```

### 3. Используйте звуки для feedback

```yaml
success_action:
  onLeftClick:
    - "console give %player% diamond 1"
    - "message &aУспешно!"
    - "playsound ENTITY_PLAYER_LEVELUP 1.0 1.0"

error_action:
  onLeftClick:
    - "message &cОшибка!"
    - "playsound ENTITY_VILLAGER_NO 1.0 1.0"
```

### 4. Информативные лоры

```yaml
item:
  lore:
    - "&7Описание предмета"
    - ""
    - "&aЦена: &e%price%"
    - "&aУровень: &e%required_level%"
    - ""
    - "&eНажмите чтобы купить"
    - "&7ПКМ для предпросмотра"
```

## Troubleshooting

### Меню не загружаются

1. Проверьте путь к папке menus
2. Убедитесь, что YAML файлы валидны (используйте YAML validator)
3. Проверьте консоль на ошибки
4. Убедитесь, что id меню уникальны

### Условия не работают

1. Убедитесь, что условие зарегистрировано через `Conditions.register()`
2. Проверьте регистр имени условия (case-sensitive)
3. Добавьте логирование в обработчик условия для отладки

### Действия не выполняются

1. Проверьте синтаксис действия
2. Убедитесь, что custom действия зарегистрированы
3. Проверьте права игрока (для команд)
4. Проверьте консоль на ошибки

### Плейсхолдеры не заменяются

1. Установите PlaceholderAPI
2. Установите нужные expansion'ы
3. Проверьте правильность синтаксиса плейсхолдера

## Полезные ссылки

- [Bukkit Material List](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html)
- [Minecraft Sound List](https://www.digminecraft.com/lists/sound_list_pc.php)
- [Color Codes](https://minecraft.tools/en/color-code.php)
- [PlaceholderAPI](https://github.com/PlaceholderAPI/PlaceholderAPI)
