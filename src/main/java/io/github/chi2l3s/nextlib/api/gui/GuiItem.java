package io.github.chi2l3s.nextlib.api.gui;

import lombok.Getter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

@Getter
public class GuiItem {
    private final ItemStack item;
    private final int slot;
    private final List<GuiAction> leftClickActions = new ArrayList<>();
    private final List<GuiAction> rightClickActions = new ArrayList<>();
    private final List<GuiCondition> enchantmentConditions = new ArrayList<>();

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

    public void addEnchantmentCondition(GuiCondition condition) {
        enchantmentConditions.add(condition);
    }

    public ItemStack createItemFor(Player player) {
        ItemStack displayItem = item.clone();
        if (!enchantmentConditions.isEmpty() && shouldApplyEnchantment(player)) {
            ItemMeta meta = displayItem.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                displayItem.setItemMeta(meta);
            }
        }
        return displayItem;
    }

    private boolean shouldApplyEnchantment(Player player) {
        for (GuiCondition condition : enchantmentConditions) {
            if (condition != null && condition.test(player)) {
                return true;
            }
        }
        return false;
    }
}
