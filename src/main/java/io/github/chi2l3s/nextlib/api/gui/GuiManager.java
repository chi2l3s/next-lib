package io.github.chi2l3s.nextlib.api.gui;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GuiManager {
    private final JavaPlugin plugin;
    private final Map<String, Gui> guis = new HashMap<>();
    private final Map<UUID, String> openGuiIds = new HashMap<>();
    private File menusFolder;

    public GuiManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadFromFolder(File folder) {
        this.menusFolder = folder;
        reloadAll();
    }

    public void reloadAll() {
        if (menusFolder == null) {
            plugin.getLogger().warning("Menus folder not configured; call loadFromFolder first.");
            return;
        }

        if (!openGuiIds.isEmpty()) {
            for (UUID uuid: new ArrayList<>(openGuiIds.keySet())) {
                Player player = Bukkit.getPlayer(uuid);
                if (player != null) {
                    player.closeInventory();
                }
            }
        }

        guis.clear();

        if (!menusFolder.exists()) {
            menusFolder.mkdirs();
            plugin.getLogger().info("Created menus folder: " + menusFolder.getAbsolutePath());
        }

        File[] files = menusFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No menus found in " + menusFolder.getAbsolutePath());
            return;
        }

        for (File file : files) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(file);

            String id = file.getName().replace(".yml", "");

            Gui gui = GuiLoader.load(plugin, this, config);
            guis.put(id.toLowerCase(), gui);

            plugin.getLogger().info("Loaded GUI: " + id + " from " + file.getName());
        }

    }

    public Gui getGui(String id) {
        return guis.get(id.toLowerCase());
    }

    public Gui registerGui(String id, Gui gui) {
        String key = id.toLowerCase();
        Gui previous = guis.put(key, gui);
        if (previous != null) {
            plugin.getLogger().warning("GUI " + key + " was overwritten by registerGui.");
        }

        return gui;
    }

    public Gui createGui(String id, String title, int size) {
        return registerGui(id, new Gui(plugin, title, this, size));
    }

    public void openGui(Player player, String id) {
        Gui gui = guis.get(id.toLowerCase());
        if (gui != null) {
            gui.open(player);
            openGuiIds.put(player.getUniqueId(), id.toLowerCase());
        } else {
            plugin.getLogger().warning("GUI '" + id + "' not found!");
        }
    }

    public Map<String, Gui> getAllGuis() {
        return guis;
    }

    public void refresh(Player player) {
        UUID uuid = player.getUniqueId();
        String guiId = openGuiIds.get(uuid);
        if (guiId == null) {
            plugin.getLogger().warning("Player '" + player.getName() + "' does not have an open GUI to refresh.");
            return;
        }
        Gui gui = guis.get(guiId);
        if (gui == null) {
            plugin.getLogger().warning("GUI '" + guiId + "' not found while trying to refresh for player '" + player.getName() + "'.");
            return;
        }

        gui.refresh(player);
    }

    void handleClose(Player player) {
        openGuiIds.remove(player.getUniqueId());
    }
}
