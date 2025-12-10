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
    private final String title;
    private final GuiManager guiManager;
    private final int size;
    private final Map<Integer, GuiItem> items = new HashMap<>();
    private final Map<UUID, Inventory> openInventories = new HashMap<>();

    public Gui(JavaPlugin plugin, String title, GuiManager guiManager, int size) {
        this.plugin = plugin;
        this.title = title;
        this.guiManager = guiManager;
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

    public void refresh(Player player) {
        Inventory inventory = openInventories.get(player.getUniqueId());
        if (inventory == null) return;

        populate(inventory, player);
        player.updateInventory();
    }

    @EventHandler
    public void on(InventoryClickEvent e) {
        if (e.getClickedInventory() == null) return;
        if (!(e.getWhoClicked() instanceof Player player)) return;

        UUID uuid = player.getUniqueId();
        Inventory inventory = openInventories.get(uuid);
        if (inventory == null || e.getInventory() != inventory) return;



        e.setCancelled(true);
        GuiItem guiItem = items.get(e.getSlot());
        if (guiItem == null) return;

        if (e.isLeftClick()) {
            guiItem.getLeftClickActions().forEach(action -> action.execute(player));
        } else if (e.isRightClick()) {
            guiItem.getRightClickActions().forEach(action -> action.execute(player));
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        UUID uuid = player.getUniqueId();
        Inventory inventory = openInventories.get(uuid);
        if (inventory == null || e.getInventory() != inventory) return;

        openInventories.remove(uuid);
        guiManager.handleClose(player);
    }
}
