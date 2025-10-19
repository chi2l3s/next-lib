package io.github.chi2l3s.nextlib.api.item;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class ItemUtil {
    public static boolean hasPersistentData(ItemStack item, JavaPlugin plugin, String key) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.STRING)
                || meta.getPersistentDataContainer().has(namespacedKey, PersistentDataType.INTEGER);
    }

    public static String getStringData(ItemStack item, JavaPlugin plugin, String key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.STRING);
    }

    public static Integer getIntData(ItemStack item, JavaPlugin plugin, String key) {
        if (item == null || !item.hasItemMeta()) return null;
        ItemMeta meta = item.getItemMeta();
        NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
        return meta.getPersistentDataContainer().get(namespacedKey, PersistentDataType.INTEGER);
    }

    public static boolean isSimilar(ItemStack first, ItemStack second) {
        if (first == null || second == null) return false;
        ItemStack clone1 = first.clone();
        ItemStack clone2 = second.clone();
        clone1.setAmount(1);
        clone2.setAmount(1);
        return clone1.isSimilar(clone2);
    }
}
