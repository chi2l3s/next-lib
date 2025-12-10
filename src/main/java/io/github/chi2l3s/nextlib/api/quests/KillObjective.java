package io.github.chi2l3s.nextlib.api.quests;

import lombok.Getter;
import org.bukkit.entity.EntityType;

import java.util.Objects;

public class KillObjective implements QuestObjective {
    private final String id;
    private final String description;
    @Getter
    private final EntityType targetType;
    private final int amount;
    private final boolean countPlayers;
    private final boolean countNonPlayers;

    public KillObjective(String id, String description, EntityType targetType, int amount) {
        this(id, description, targetType, amount, true, true);
    }

    public KillObjective(String id, String description, EntityType targetType, int amount, boolean countPlayers, boolean countNonPlayers) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        this.targetType = targetType;
        this.amount = amount;
        this.countPlayers = countPlayers;
        this.countNonPlayers = countNonPlayers;
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
        return QuestObjectiveType.KILL_ENTITY;
    }

    @Override
    public double getTargetAmount() {
        return amount;
    }

    @Override
    public boolean matchesKill(EntityType entityType, boolean playerKill) {
        if (playerKill && !countPlayers) {
            return false;
        }
        if (!playerKill && !countNonPlayers) {
            return false;
        }
        return targetType == null || targetType == entityType;
    }

    public boolean shouldCountPlayers() {
        return countPlayers;
    }

    public boolean shouldCountNonPlayers() {
        return countNonPlayers;
    }
}
