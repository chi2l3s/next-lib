package io.github.chi2l3s.nextlib.api.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class LongCommandExecutor implements TabExecutor {
    private final List<SubCommandWrapper> subCommands = new ArrayList<>();

    protected void addSubCommand(SubCommand command, String[] aliases, Permission permission) {
        this.subCommands.add(new SubCommandWrapper(command, aliases, permission));
    }

    @Nullable
    protected SubCommandWrapper getWrapperFromLabel(String label) {
        for (SubCommandWrapper wrapper : this.subCommands) {
            for (String alias : wrapper.aliases) {
                if (alias.equalsIgnoreCase(label)) {
                    return wrapper;
                }
            }
        }
        return null;
    }

    protected List<String> getFirstAliases() {
        List<String> result = new ArrayList<>();
        for (SubCommandWrapper wrapper : this.subCommands) {
            result.add(wrapper.aliases[0]);
        }
        return result;
    }

    protected List<SubCommandWrapper> getSubCommands() {
        return this.subCommands;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (args.length < 1) return false;
        final SubCommandWrapper wrapper = getWrapperFromLabel(args[0]);
        if (wrapper == null) return false;

        if (!sender.hasPermission(wrapper.getPermission())) {
            sender.sendMessage(command.getPermissionMessage());
            return true;
        }

        wrapper.getCommand().onExecute(sender, args);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return getFirstAliases();
        }
        final SubCommandWrapper wrapper = getWrapperFromLabel(args[0]);
        if (wrapper == null) return null;

        return wrapper.getCommand().onTabComplete(sender, args);
    }

    public static class SubCommandWrapper {
        private final SubCommand command;
        private final String[] aliases;
        private final Permission permission;

        public SubCommandWrapper(SubCommand command, String[] aliases, Permission permission) {
            this.command = command;
            this.aliases = aliases;
            this.permission = permission;
        }

        public SubCommand getCommand() {
            return this.command;
        }

        public String[] getAliases() {
            return this.aliases;
        }

        public Permission getPermission() {
            return this.permission;
        }
    }
}
