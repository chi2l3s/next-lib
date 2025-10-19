package io.github.chi2l3s.nextlib.api.gui;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class GuiManager {
    private final JavaPlugin plugin;
    private final Map<String, Gui> guis = new HashMap<>();

    public GuiManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void loadFromFolder(File folder) {
        if (!folder.exists()) {
            folder.mkdirs();
            plugin.getLogger().info("Created menus folder: " + folder.getAbsolutePath());
        }

        File[] files = folder.listFiles((((dir, name) -> name.endsWith(".yml"))));
        if (files == null || files.length == 0) {
            plugin.getLogger().warning("No menus found in " + folder.getAbsolutePath());
            return;
        }

        for (File file: files) {
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

    public void openGui(Player player, String id) {
        Gui gui = guis.get(id.toLowerCase());
        if (gui != null) {
            gui.open(player);
        } else {
            plugin.getLogger().warning("GUI '" + id + "' not found!");
        }
    }

    public Map<String, Gui> getAllGuis() {
        return guis;
    }
}
