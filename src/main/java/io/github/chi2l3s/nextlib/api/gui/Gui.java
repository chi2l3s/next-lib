package io.github.chi2l3s.nextlib.api.gui;

import io.github.chi2l3s.nextlib.NextLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

public class Gui implements Listener {
    private final JavaPlugin plugin;
    private final String title;
    private final int size;
    private final Map<Integer, GuiItem> items = new HashMap<>();
    private Inventory inventory;

    public Gui(JavaPlugin plugin, String title, int size) {
        this.plugin = plugin;
        this.title = title;
        this.size = size;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void addItem(GuiItem item) {
        items.put(item.getSlot(), item);
    }

    public void open(Player player) {
        inventory = Bukkit.createInventory(null, size, NextLib.c.formatMessage(title));
        for (GuiItem item: items.values()) {
            inventory.setItem(item.getSlot(), item.getItem());
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void on(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        if (e.getInventory() != inventory) return;

        e.setCancelled(true);
        GuiItem guiItem = items.get(e.getSlot());
        if (guiItem == null) return;

        if (e.isLeftClick()) {
            guiItem.getLeftClickActions().forEach(action -> action.execute((Player) e.getWhoClicked()));
        } else if (e.isRightClick()) {
            guiItem.getRightClickActions().forEach(action -> action.execute((Player) e.getWhoClicked()));
        }
    }
}
