package io.github.chi2l3s.nextlib.api.item;

import io.github.chi2l3s.nextlib.NextLib;
import io.github.chi2l3s.nextlib.api.color.ColorUtil;
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

/**
 * Builder for creating customized ItemStacks with fluent API.
 * <p>
 * Supports dependency injection for ColorUtil, or uses default instance from NextLib.
 * </p>
 *
 * <p><strong>Example usage:</strong></p>
 * <pre>{@code
 * ItemStack item = new ItemBuilder(Material.DIAMOND)
 *     .setName("&bSpecial Diamond")
 *     .setLore("&7This is a special item")
 *     .setUnbreakable(true)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
public class ItemBuilder {
    private final ItemStack item;
    private final ItemMeta meta;
    private final ColorUtil colorUtil;

    /**
     * Creates ItemBuilder with default ColorUtil from NextLib.
     *
     * @param material the material type
     */
    public ItemBuilder(Material material) {
        this(material, 1, NextLib.c);
    }

    /**
     * Creates ItemBuilder with specified amount and default ColorUtil.
     *
     * @param material the material type
     * @param amount   the stack size
     */
    public ItemBuilder(Material material, int amount) {
        this(material, amount, NextLib.c);
    }

    /**
     * Creates ItemBuilder with dependency-injected ColorUtil.
     *
     * @param material  the material type
     * @param colorUtil the color formatter to use
     */
    public ItemBuilder(Material material, ColorUtil colorUtil) {
        this(material, 1, colorUtil);
    }

    /**
     * Creates ItemBuilder with specified amount and ColorUtil.
     *
     * @param material  the material type
     * @param amount    the stack size
     * @param colorUtil the color formatter to use
     */
    public ItemBuilder(Material material, int amount, ColorUtil colorUtil) {
        this.item = new ItemStack(material, amount);
        this.meta = item.getItemMeta();
        this.colorUtil = colorUtil;
    }

    /**
     * Sets the display name of the item with color formatting.
     *
     * @param name the display name (supports & color codes and HEX)
     * @return this builder for chaining
     */
    public ItemBuilder setName(String name) {
        if (meta != null) {
            meta.setDisplayName(colorUtil.formatMessage(name));
        }
        return this;
    }

    /**
     * Sets the lore (description) of the item with color formatting.
     *
     * @param lore the lore lines (supports & color codes and HEX)
     * @return this builder for chaining
     */
    public ItemBuilder setLore(String... lore) {
        return setLore(Arrays.asList(lore));
    }

    /**
     * Sets the lore (description) of the item with color formatting.
     *
     * @param lore the lore lines (supports & color codes and HEX)
     * @return this builder for chaining
     */
    public ItemBuilder setLore(List<String> lore) {
        if (meta != null) {
            List<String> l = lore.stream()
                    .map(line -> colorUtil.formatMessage(line))
                    .collect(Collectors.toList());
            meta.setLore(l);
        }
        return this;
    }

    /**
     * Adds an enchantment to the item.
     *
     * @param enchantment           the enchantment type
     * @param level                 the enchantment level
     * @param ignoreLevelRestriction whether to bypass max level restrictions
     * @return this builder for chaining
     */
    public ItemBuilder addEnchant(Enchantment enchantment, int level, boolean ignoreLevelRestriction) {
        if (meta != null) {
            meta.addEnchant(enchantment, level, ignoreLevelRestriction);
        }
        return this;
    }

    /**
     * Adds multiple enchantments to the item.
     *
     * @param enchants map of enchantments to their levels
     * @return this builder for chaining
     */
    public ItemBuilder addEnchants(Map<Enchantment, Integer> enchants) {
        enchants.forEach((ench, lvl) -> addEnchant(ench, lvl, true));
        return this;
    }

    /**
     * Sets the unbreakable flag for the item.
     *
     * @param unbreakable whether the item should be unbreakable
     * @return this builder for chaining
     */
    public ItemBuilder setUnbreakable(boolean unbreakable) {
        if (meta != null) {
            meta.setUnbreakable(unbreakable);
        }
        return this;
    }

    /**
     * Adds item flags to hide certain attributes.
     *
     * @param flags the flags to add
     * @return this builder for chaining
     */
    public ItemBuilder addFlags(ItemFlag... flags) {
        if (meta != null) {
            meta.addItemFlags(flags);
        }
        return this;
    }

    /**
     * Sets the skull owner for player heads.
     *
     * @param playerName the player name
     * @return this builder for chaining
     */
    public ItemBuilder setSkullOwner(String playerName) {
        if (meta instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(Bukkit.getOfflinePlayer(playerName));
        }
        return this;
    }

    /**
     * Sets persistent data on the item with String value.
     *
     * @param plugin the plugin instance
     * @param key    the data key
     * @param value  the string value
     * @return this builder for chaining
     */
    public ItemBuilder setPersistentData(JavaPlugin plugin, String key, String value) {
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(namespacedKey, PersistentDataType.STRING, value);
        }
        return this;
    }

    /**
     * Sets persistent data on the item with Integer value.
     *
     * @param plugin the plugin instance
     * @param key    the data key
     * @param value  the integer value
     * @return this builder for chaining
     */
    public ItemBuilder setPersistentData(JavaPlugin plugin, String key, int value) {
        if (meta != null) {
            NamespacedKey namespacedKey = new NamespacedKey(plugin, key);
            PersistentDataContainer container = meta.getPersistentDataContainer();
            container.set(namespacedKey, PersistentDataType.INTEGER, value);
        }
        return this;
    }

    /**
     * Builds and returns the final ItemStack.
     *
     * @return the constructed ItemStack
     */
    public ItemStack build() {
        if (meta != null) {
            item.setItemMeta(meta);
        }
        return item;
    }
}
