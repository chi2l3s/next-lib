package io.github.chi2l3s.nextlib.api.quests.objectivies;

import io.github.chi2l3s.nextlib.api.quests.QuestObjective;
import io.github.chi2l3s.nextlib.api.quests.QuestObjectiveType;
import lombok.Getter;
import org.bukkit.Material;

import java.util.Objects;

/**
 * Objective that tracks breaking blocks.
 */
public final class BlockBreakObjective implements QuestObjective {
    private final String id;
    private final String description;
    @Getter
    private final Material blockType;
    @Getter
    private final int amount;

    /**
     * @param blockType block is material to count, or {@code null} to count any broken block.
     */
    public BlockBreakObjective(String id, String description, Material blockType, int amount) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        this.blockType = blockType;
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
        return QuestObjectiveType.BREAK_BLOCK;
    }

    @Override
    public double getTargetAmount() {
        return amount;
    }

    @Override
    public boolean matchesBlockBreak(Material blockType) {
        return this.blockType == null || this.blockType == blockType;
    }
}
