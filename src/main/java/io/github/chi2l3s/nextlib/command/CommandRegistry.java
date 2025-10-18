package io.github.chi2l3s.nextlib.command;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class CommandRegistry {
    private final JavaPlugin plugin;

    public CommandRegistry(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerCommand(String commandName, LongCommandExecutor executor) {
        PluginCommand cmd = plugin.getCommand(commandName);
        if (cmd == null) {
            plugin.getLogger().warning("Команда '" + commandName + "' не найдена в plugin.yml");
            return;
        }
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);
    }
}
