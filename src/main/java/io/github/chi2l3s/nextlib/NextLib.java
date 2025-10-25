package io.github.chi2l3s.nextlib;

import io.github.chi2l3s.nextlib.api.color.ColorUtil;
import io.github.chi2l3s.nextlib.api.color.ColorUtilImpl;
import io.github.chi2l3s.nextlib.internal.Metrics;
import io.github.chi2l3s.nextlib.internal.util.UpdateChecker;
import org.bukkit.plugin.java.JavaPlugin;

public final class NextLib extends JavaPlugin {

    public static ColorUtil c = new ColorUtilImpl();

    @Override
    public void onEnable() {
        new UpdateChecker(this, "chi2l3s/next-lib").checkForUpdates();
        int pluginId = 27705;
        new Metrics(this, pluginId);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
