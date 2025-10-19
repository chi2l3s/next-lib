package io.github.chi2l3s.nextlib.api.gui;

import lombok.Getter;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GuiItem {
    private final ItemStack item;
    private final int slot;
    private final List<GuiAction> leftClickActions = new ArrayList<>();
    private final List<GuiAction> rightClickActions = new ArrayList<>();

    public GuiItem(ItemStack item, int slot) {
        this.item = item;
        this.slot = slot;
    }

    public void addLeftClickAction(GuiAction action) {
        leftClickActions.add(action);
    }

    public void addRightClickAction(GuiAction action) {
        rightClickActions.add(action);
    }
}
