package io.github.chi2l3s.nextlib.api.quests.objectivies;

import io.github.chi2l3s.nextlib.api.quests.QuestObjective;
import io.github.chi2l3s.nextlib.api.quests.QuestObjectiveType;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Objects;

/**
 * Objective that tracks fishing catches.
 */
public final class FishObjective implements QuestObjective {
    private final String id;
    private final String description;
    @Getter
    private final Material caughtType;
    @Getter
    private final int amount;

    /**
     * @param caughtType material to count, or {@code null} to count any catch
     */
    public FishObjective(String id, String description, Material caughtType, int amount) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        this.caughtType = caughtType;
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
        return QuestObjectiveType.FISH;
    }

    @Override
    public double getTargetAmount() {
        return amount;
    }

    @Override
    public boolean matchesFish(Material caughtType) {
        return this.caughtType == null || this.caughtType == caughtType;
    }
}