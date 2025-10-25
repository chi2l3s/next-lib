package io.github.chi2l3s.nextlib.api.gui;

import io.github.chi2l3s.nextlib.NextLib;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class Actions {
    private static final Map<String, GuiActionFactory> REGISTERED_ACTIONS = new ConcurrentHashMap<>();

    static {
        registerAction("close", (manager, args) -> close());
        registerAction("command", (manager, args) -> command(args));
        registerAction("console", (manager, args) -> console(args));
        registerAction("message", (manager, args) -> message(args));
        registerAction("opengui", (manager, args) -> openGui(manager, args));
    }

    private Actions() {
    }

    public static GuiAction close() {
        return Player::closeInventory;
    }

    public static GuiAction command(String cmd) {
        return player -> Bukkit.dispatchCommand(player, PlaceholderAPI.setPlaceholders(player, cmd.replace("%player%", player.getName())));
    }

    public static GuiAction console(String cmd) {
        return player -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), PlaceholderAPI.setPlaceholders(player, cmd.replace("%player%", player.getName())));
    }

    public static GuiAction message(String msg) {
        return player -> player.sendMessage(NextLib.c.formatMessage(msg));
    }

    public static GuiAction openGui(GuiManager manager, String guiId) {
        return player -> manager.openGui(player, guiId);
    }

    /**
     * Registers a new GUI action that can be referenced from YAML definitions.
     * <p>
     * The {@code name} is matched case-insensitively against the first word in the
     * configured action string. The remaining part of the string is passed to the
     * {@link GuiActionFactory} as {@code arguments} so the factory can parse
     * custom parameters.
     * </p>
     *
     * @param name    unique identifier of the action (e.g. {@code "teleport"})
     * @param factory factory that creates the {@link GuiAction} implementation
     */
    public static void registerAction(String name, GuiActionFactory factory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(factory, "factory");
        REGISTERED_ACTIONS.put(name.toLowerCase(), factory);
    }

    public static GuiAction create(String rawAction, GuiManager manager) {
        if (rawAction == null) {
            return null;
        }

        String trimmed = rawAction.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String actionName;
        String arguments;

        int separatorIndex = trimmed.indexOf(' ');
        if (separatorIndex == -1) {
            actionName = trimmed.toLowerCase();
            arguments = "";
        } else {
            actionName = trimmed.substring(0, separatorIndex).toLowerCase();
            arguments = trimmed.substring(separatorIndex + 1).trim();
        }

        GuiActionFactory factory = REGISTERED_ACTIONS.get(actionName);
        if (factory == null) {
            return null;
        }

        return factory.create(manager, arguments);
    }
}
