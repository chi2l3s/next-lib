package io.github.chi2l3s.nextlib.api.quests.objectivies;

import io.github.chi2l3s.nextlib.api.quests.QuestObjective;
import io.github.chi2l3s.nextlib.api.quests.QuestObjectiveType;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Objects;

/**
 * Objective that tracks items smelted in furnaces, blast furnaces or smokers.
 */
public final class SmeltObjective implements QuestObjective {
    private final String id;
    private final String description;
    @Getter
    private final Material resultType;
    @Getter
    private final int amount;

    /**
     * @param resultType resulting item to track, or {@code null} to count any smelted output
     */
    public SmeltObjective(String id, String description, Material resultType, int amount) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        this.resultType = resultType;
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
        return QuestObjectiveType.SMELT_ITEM;
    }

    @Override
    public double getTargetAmount() {
        return amount;
    }

    @Override
    public boolean matchesSmelt(Material resultType) {
        return this.resultType == null || this.resultType == resultType;
    }
}
