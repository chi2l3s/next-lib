package io.github.chi2l3s.nextlib.api.gui;

import io.github.chi2l3s.nextlib.api.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class GuiLoader {
    public static Gui load(JavaPlugin plugin, GuiManager manager, ConfigurationSection section) {
        String title = section.getString("title", "Menu");
        int size = section.getInt("size", 36);

        Gui gui = new Gui(plugin, title, manager, size);

        ConfigurationSection items = section.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemSec = items.getConfigurationSection(key);
                if (itemSec == null) continue;

                Material material = Material.matchMaterial(itemSec.getString("material", "STONE"));
                int amount = itemSec.getInt("amount", 1);
                String name = itemSec.getString("name", "");
                List<String> lore = itemSec.getStringList("lore");

                List<Integer> slots = resolveSlots(plugin, itemSec, key, size);
                if (slots.isEmpty()) continue;

                ItemStack itemStack = new ItemBuilder(material, amount).setName(name).setLore(lore).build();

                for (int slot : slots) {
                    GuiItem guiItem = new GuiItem(itemStack, slot);

                    applyActions(plugin, manager, itemSec.getStringList("on_left_click"), guiItem::addLeftClickAction, key, "left");
                    applyActions(plugin, manager, itemSec.getStringList("on_right_click"), guiItem::addRightClickAction, key, "right");
                    applyConditions(plugin, manager, itemSec.getStringList("enchanted_when"), guiItem, key);

                    gui.addItem(guiItem);
                }
            }

        }
        return gui;
    }

    private static List<Integer> resolveSlots(JavaPlugin plugin, ConfigurationSection itemSec, String itemKey, int size) {
        Set<Integer> slots = new LinkedHashSet<>();

        List<String> configuredSlots = itemSec.getStringList("slots");
        if (!configuredSlots.isEmpty()) {
            for (String entry : configuredSlots) {
                parseSlotEntry(plugin, slots, entry, itemKey, size);
            }
        } else if (itemSec.contains("slot")) {
            int slot = itemSec.getInt("slot");
            if (validateSlot(plugin, slot, itemKey, size)) {
                slots.add(slot);
            }
        } else {
            plugin.getLogger().warning("GUI item '" + itemKey + "' is missing 'slot' or 'slots'. Skipping entry.");
        }

        return new ArrayList<>(slots);
    }

    private static void parseSlotEntry(JavaPlugin plugin, Set<Integer> slots, String entry, String itemKey, int size) {
        String trimmed = entry == null ? "" : entry.trim();
        if (trimmed.isEmpty()) {
            return;
        }

        String[] parts = trimmed.split("-");
        if (parts.length == 1) {
            try {
                int slot = Integer.parseInt(parts[0].trim());
                if (validateSlot(plugin, slot, itemKey, size)) {
                    slots.add(slot);
                }
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Invalid slot '" + trimmed + "' for GUI item '" + itemKey + "'.");
            }
            return;
        }

        if (parts.length == 2) {
            try {
                int start = Integer.parseInt(parts[0].trim());
                int end = Integer.parseInt(parts[1].trim());
                if (start > end) {
                    int tmp = start;
                    start = end;
                    end = tmp;
                }
                for (int slot = start; slot <= end; slot++) {
                    if (validateSlot(plugin, slot, itemKey, size)) {
                        slots.add(slot);
                    }
                }
            } catch (NumberFormatException ex) {
                plugin.getLogger().warning("Invalid slot range '" + trimmed + "' for GUI item '" + itemKey + "'.");
            }
            return;
        }

        plugin.getLogger().warning("Invalid slot range '" + trimmed + "' for GUI item '" + itemKey + "'.");
    }

    private static boolean validateSlot(JavaPlugin plugin, int slot, String itemKey, int size) {
        if (slot < 0 || slot >= size) {
            plugin.getLogger().warning("Slot " + slot + " for GUI item '" + itemKey + "' is outside the inventory size " + size + ".");
            return false;
        }
        return true;
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

    private static void applyConditions(JavaPlugin plugin, GuiManager manager, List<String> rawConditions, GuiItem item, String itemKey) {
        for (String rawCondition : rawConditions) {
            GuiCondition condition = Conditions.create(rawCondition, manager);
            if (condition != null) {
                item.addEnchantmentCondition(condition);
            } else {
                plugin.getLogger().warning("Unknown GUI condition '" + rawCondition + "' for item '" + itemKey + "'.");
            }
        }
    }
}
