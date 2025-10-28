package io.github.chi2l3s.nextlib.api.gui;

import io.github.chi2l3s.nextlib.NextLib;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Gui implements Listener {
    private final JavaPlugin plugin;
    private final GuiManager manager;
    private final String title;
    private final int size;
    private final Map<Integer, GuiItem> items = new HashMap<>();
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public Gui(JavaPlugin plugin, GuiManager manager, String title, int size) {
        this.plugin = plugin;
        this.manager = manager;
        this.title = title;
        this.size = size;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void addItem(GuiItem item) {
        items.put(item.getSlot(), item);
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(null, size, NextLib.c.formatMessage(title));
        populate(inventory, player);
        openInventories.put(player.getUniqueId(), inventory);
        player.openInventory(inventory);
    }

    private void populate(Inventory inventory, Player player) {
        for (GuiItem item : items.values()) {
            inventory.setItem(item.getSlot(), item.createItemFor(player));
        }
    }

    @EventHandler
    public void on(InventoryClickEvent event) {
        if (event.getClickedInventory() == null) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        Inventory inventory = openInventories.get(uuid);
        if (inventory == null || event.getInventory() != inventory) return;

        event.setCancelled(true);
        GuiItem guiItem = items.get(event.getSlot());
        if (guiItem == null) return;

        if (event.isLeftClick()) {
            guiItem.getLeftClickActions().forEach(action -> action.execute(player));
        } else if (event.isRightClick()) {
            guiItem.getRightClickActions().forEach(action -> action.execute(player));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        Inventory inventory = openInventories.get(uuid);
        if (inventory == null || event.getInventory() != inventory) return;

        openInventories.remove(uuid);
        manager.handleClose(player);
    }
}
