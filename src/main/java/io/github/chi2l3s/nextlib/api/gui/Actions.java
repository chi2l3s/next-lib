package io.github.chi2l3s.nextlib.api.gui;

import io.github.chi2l3s.nextlib.NextLib;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class Actions {
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
}
