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
        registerAction("opengui", Actions::openGui);
    }

    private Actions() {}

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
     * Регистрирует новое действие графического интерфейса пользователя, на которое можно ссылаться из определений YAML.
     * <p>
     * {@code name} сопоставляется без учета регистра с первым словом в
     * сконфигурированной строке действия. Оставшаяся часть строки передается в
     * {@link GuiActionFactory} в качестве {@code arguments}, чтобы фабрика могла анализировать
     * пользовательские параметры.
     * </p>
     *
     * @param name - уникальный идентификатор действия (например, {@code "teleport"})
     * @param factory - фабрика, которая создает реализацию {@link GuiAction}.
     */
    public static void registerAction(String name, GuiActionFactory factory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(factory, "factory");
        REGISTERED_ACTIONS.put(name, factory);
    }

    public static GuiAction create(String rawAction, GuiManager manager) {
        if (rawAction == null) return null;

        String trimmed = rawAction.trim();
        if (trimmed.isEmpty()) return null;

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
        if (factory == null) return null;

        return factory.create(manager, arguments);
    }
}
