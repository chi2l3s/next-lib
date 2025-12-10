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
}
