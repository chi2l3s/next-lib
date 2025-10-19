package io.github.chi2l3s.nextlib.api.gui;

import io.github.chi2l3s.nextlib.api.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public class GuiLoader {
    public static Gui load(JavaPlugin plugin, GuiManager manager, ConfigurationSection section) {
        String title = section.getString("title", "Menu");
        int size = section.getInt("size", 36);

        Gui gui = new Gui(plugin, title, size);

        ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            for (String key: items.getKeys(false)) {
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

                for (String act: itemSec.getStringList("on_left_click")) {
                    if (act.equalsIgnoreCase("close")) guiItem.addLeftClickAction(Actions.close());
                    else if (act.startsWith("command ")) guiItem.addLeftClickAction(Actions.command(act.substring(8)));
                    else if (act.startsWith("console ")) guiItem.addLeftClickAction(Actions.console(act.substring(8)));
                    else if (act.startsWith("message ")) guiItem.addLeftClickAction(Actions.message(act.substring(8)));
                    else if (act.startsWith("opengui ")) guiItem.addLeftClickAction(Actions.openGui(manager, act.substring(8)));
                }

                for (String act : itemSec.getStringList("on_right_click")) {
                    if (act.equalsIgnoreCase("close")) guiItem.addRightClickAction(Actions.close());
                    else if (act.startsWith("command ")) guiItem.addRightClickAction(Actions.command(act.substring(8)));
                    else if (act.startsWith("console ")) guiItem.addRightClickAction(Actions.console(act.substring(8)));
                    else if (act.startsWith("message ")) guiItem.addRightClickAction(Actions.message(act.substring(8)));
                    else if (act.startsWith("opengui ")) guiItem.addRightClickAction(Actions.openGui(manager, act.substring(8)));
                }

                gui.addItem(guiItem);
            }

        }
        return gui;
    }
}
