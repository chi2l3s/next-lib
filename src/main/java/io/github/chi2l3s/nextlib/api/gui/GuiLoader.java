package io.github.chi2l3s.nextlib.api.gui;

import io.github.chi2l3s.nextlib.api.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.function.Consumer;

public class GuiLoader {
    public static Gui load(JavaPlugin plugin, GuiManager manager, ConfigurationSection section) {
        String title = section.getString("title", "Menu");
        int size = section.getInt("size", 36);

        Gui gui = new Gui(plugin, title, size);

        ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSec = items.getConfigurationSection(key);
                if (itemSec == null) continue;

                Material material = Material.matchMaterial(itemSec.getString("material", "STONE"));
                int amount = itemSec.getInt("amount", 1);
                int slot = itemSec.getInt("slot", 0);
                String name = itemSec.getString("name", "");
                List<String> lore = itemSec.getStringList("lore");

                GuiItem guiItem = new GuiItem(
                        new ItemBuilder(material, amount).setName(name).setLore(lore).build(),
                        slot
                );

                applyActions(plugin, manager, itemSec.getStringList("on_left_click"), guiItem::addLeftClickAction, key, "left");
                applyActions(plugin, manager, itemSec.getStringList("on_right_click"), guiItem::addRightClickAction, key, "right");

                gui.addItem(guiItem);
            }

        }
        return gui;
    }

    private static void applyActions(JavaPlugin plugin, GuiManager manager, List<String> rawActions, Consumer<GuiAction> target, String itemKey, String clickType) {
        for (String rawAction : rawActions) {
            GuiAction action = Actions.create(rawAction, manager);
            if (action != null) {
                target.accept(action);
            } else {
                plugin.getLogger().warning("Unknown GUI action '" + rawAction + "' for item '" + itemKey + "' on " + clickType + " click.");
            }
        }
    }
}
