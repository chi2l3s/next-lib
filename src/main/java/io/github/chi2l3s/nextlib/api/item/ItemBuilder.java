package io.github.chi2l3s.nextlib.api.item;

import io.github.chi2l3s.nextlib.NextLib;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;

    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder(Material material, int amount) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
    }

    public ItemBuilder setName(String name) {
        if (meta != null) {
            meta.setDisplayName(NextLib.c.formatMessage(name));
        }
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        return setLore(Arrays.asList(lore));
    }

    public ItemBuilder setLore(List<String> lore) {
        if (meta != null) {
            List<String> l = lore.stream()
                    .map(line -> NextLib.c.formatMessage(line))
                    .collect(Collectors.toList());
            meta.setLore(l);
        }
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
        if (meta != null) {
            meta.addEnchant(enchantment, level, ignoreLevelRestriction);
        }
        return this;
    }

    public ItemBuilder addEnchants(Map<Enchantment, Integer> enchants) {
        enchants.forEach((ench, lvl) -> addEnchant(ench, lvl, true));
        return this;
    }

    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
        }
        return this;
    }

    public ItemBuilder addFlags(ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    public ItemBuilder setSkullOwner(String playerName) {
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        }
        return this;
    }

    public ItemBuilder setPersistentData(JavaPlugin plugin, String key, String value) {
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(namespacedKey, PersistentDataType.STRING, value);
        }
        return this;
    }

    public ItemBuilder setPersistentData(JavaPlugin plugin, String key, int value) {
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(namespacedKey, PersistentDataType.INTEGER, value);
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
