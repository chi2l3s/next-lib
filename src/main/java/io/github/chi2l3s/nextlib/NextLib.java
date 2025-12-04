package io.github.chi2l3s.nextlib;

import io.github.chi2l3s.nextlib.api.color.ColorUtil;
import io.github.chi2l3s.nextlib.api.color.ColorUtilImpl;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class NextLib extends JavaPlugin {

    public static ColorUtil c = new ColorUtilImpl();

    @Override
    public void onEnable() {
        Bukkit.getLogger().info("[ NextLib ] Enabled");
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
