package io.github.chi2l3s.nextlib.api.quests.objectivies;

import io.github.chi2l3s.nextlib.api.quests.QuestObjective;
import io.github.chi2l3s.nextlib.api.quests.QuestObjectiveType;
import lombok.Getter;
import org.bukkit.entity.EntityType;

import java.util.Objects;

/**
 * Objective that tracks breeding animals.
 */
public final class BreedObjective implements QuestObjective {
    private final String id;
    private final String description;
    @Getter
    private final EntityType entityType;
    @Getter
    private final int amount;

    /**
     * @param entityType entity type to count, or {@code null} to count any bred entity
     */
    public BreedObjective(String id, String description, EntityType entityType, int amount) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        this.entityType = entityType;
        this.amount = amount;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public QuestObjectiveType getType() {
        return QuestObjectiveType.BREED_ENTITY;
    }

    @Override
    public double getTargetAmount() {
        return amount;
    }

    @Override
    public boolean matchesBreed(EntityType entityType) {
        return this.entityType == null || this.entityType == entityType;
    }

}
