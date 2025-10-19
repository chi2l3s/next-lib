package io.github.chi2l3s.nextlib.api.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {
    void onExecute(CommandSender sender, String[] args);
    List<String> onTabComplete(CommandSender sender, String[] args);
}
