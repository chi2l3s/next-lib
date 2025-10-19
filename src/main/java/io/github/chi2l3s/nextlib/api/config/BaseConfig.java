package io.github.chi2l3s.nextlib.api.config;

import com.tchristofferson.configupdater.ConfigUpdater;
import lombok.Getter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public abstract class BaseConfig {
    protected final JavaPlugin plugin;
    protected File file;
    @Getter
    protected FileConfiguration config;

    public BaseConfig(JavaPlugin plugin, String fileName) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), fileName);
    }

    public void reloadConfig() {
        if (!file.exists()) {
            plugin.saveResource(file.getName(), false);
        }
        config = YamlConfiguration.loadConfiguration(file);

        try {
            ConfigUpdater.update(plugin, file.getName(), file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to update config: " + e.getMessage());
        }

        loadValues();
    }

    protected abstract void loadValues();
}
