package io.github.chi2l3s.nextlib.api.item;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Basic tests for ItemBuilder structure and builder pattern.
 * Note: Full functionality tests require Bukkit mock setup.
 */
@DisplayName("ItemBuilder Tests")
class ItemBuilderTest {

    @Test
    @DisplayName("Should create ItemBuilder with material")
    void shouldCreateItemBuilderWithMaterial() {
        // When
        ItemBuilder builder = new ItemBuilder(Material.DIAMOND);

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("Should create ItemBuilder with material and amount")
    void shouldCreateItemBuilderWithMaterialAndAmount() {
        // When
        ItemBuilder builder = new ItemBuilder(Material.DIAMOND, 64);

        // Then
        assertThat(builder).isNotNull();
    }

    @Test
    @DisplayName("Should build ItemStack")
    void shouldBuildItemStack() {
        // Given
        ItemBuilder builder = new ItemBuilder(Material.DIAMOND);

        // When
        ItemStack item = builder.build();

        // Then
        assertThat(item).isNotNull();
        assertThat(item.getType()).isEqualTo(Material.DIAMOND);
    }

    @Test
    @DisplayName("Should build ItemStack with correct amount")
    void shouldBuildItemStackWithCorrectAmount() {
        // Given
        ItemBuilder builder = new ItemBuilder(Material.DIAMOND, 32);

        // When
        ItemStack item = builder.build();

        // Then
        assertThat(item).isNotNull();
        assertThat(item.getAmount()).isEqualTo(32);
    }

    @Test
    @DisplayName("Should support fluent builder pattern")
    void shouldSupportFluentBuilderPattern() {
        // When
        ItemBuilder result = new ItemBuilder(Material.DIAMOND)
                .setUnbreakable(true);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isInstanceOf(ItemBuilder.class);
    }

    @Test
    @DisplayName("Should handle null meta gracefully")
    void shouldHandleNullMetaGracefully() {
        // Given
        ItemBuilder builder = new ItemBuilder(Material.AIR);

        // When & Then
        assertThatCode(() -> {
            builder.setUnbreakable(true);
            builder.build();
        }).doesNotThrowAnyException();
    }
}
