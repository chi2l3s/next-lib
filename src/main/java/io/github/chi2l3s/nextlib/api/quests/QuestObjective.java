package io.github.chi2l3s.nextlib.api.quests;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public interface QuestObjective {
    String getId();

    String getDescription();

    QuestObjectiveType getType();

    double getTargetAmount();

    /**
     * Returns {@code true} if the objective should progress from a kill event.
     */
    default boolean matchesKill(EntityType entityType, boolean playerKill) {
        return false;
    }

    /**
     * Returns {@code true} if the objective should progress from a travel event.
     */
    default boolean matchesTravel() {
        return false;
    }

    /**
     * Returns {@code true} if the objective should progress from a crafting event.
     */
    default boolean matchesCraft(Material material) {
        return false;
    }

    /**
     * Returns {@code true} if the objective should progress from a playtime tick.
     */
    default boolean matchesPlaytime() {
        return false;
    }

    /**
     * Returns {@code true} if the objective should progress from a block break event.
     */
    default boolean matchesBlockBreak(Material blockType) {
        return false;
    }

    /**
     * Returns {@code true} if the objective should progress from a block place event.
     */
    default boolean matchesBlockPlace(Material blockType) {
        return false;
    }

    /**
     * Returns {@code true} if the objective should progress from a smelt event with the given result item.
     */
    default boolean matchesSmelt(Material resultType) {
        return false;
    }

    /**
     * Returns {@code true} if the objective should progress from breeding the given entity type.
     */
    default boolean matchesBreed(EntityType entityType) {
        return false;
    }

    /**
     * Returns {@code true} if the objective should progress from taming the given entity type.
     */
    default boolean matchesTame(EntityType entityType) {
        return false;
    }

    /**
     * Returns {@code true} if the objective should progress from a fishing catch of the given item type.
     * Pass {@code null} to indicate any catch.
     */
    default boolean matchesFish(Material caughtType) {
        return false;
    }

    /**
     * Returns {@code true} if the objective should progress from consuming the given item type.
     */
    default boolean matchesConsume(Material material) {
        return false;
    }

    /**
     * Returns {@code true} if the objective should progress from a custom event type.
     * The {@code payload} object can contain any context your implementation needs (for example, a Bukkit event instance
     * or simple DTO). Return {@code false} to ignore the event.
     */
    default boolean matchesCustom(String eventType, Object payload) {
        return false;
    }
}
