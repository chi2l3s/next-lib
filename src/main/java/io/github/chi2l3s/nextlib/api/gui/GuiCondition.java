package io.github.chi2l3s.nextlib.api.gui;

import org.bukkit.entity.Player;

@FunctionalInterface
public interface GuiCondition {
    boolean test(Player player);
}
