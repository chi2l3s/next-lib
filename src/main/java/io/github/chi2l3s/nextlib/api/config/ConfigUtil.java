package io.github.chi2l3s.nextlib.api.config;

import io.github.chi2l3s.nextlib.api.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class ConfigUtil {
    public static World getWorld(ConfigurationSection section, String path, String def) {
        String worldName = section.getString(path, def);
        return Bukkit.getWorld(worldName);
    }

    public static int parseTime(ConfigurationSection section, String path, int def) {
        String raw = section.getString(path);
        if (raw == null) return def;
        return TimeUtil.parseTime(raw);
    }
}
