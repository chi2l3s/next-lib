package io.github.chi2l3s.nextlib.api.quests.objectivies;

import io.github.chi2l3s.nextlib.api.quests.QuestObjective;
import io.github.chi2l3s.nextlib.api.quests.QuestObjectiveType;

import java.util.Objects;

public final class PlayTimeObjective implements QuestObjective {
    private final String id;
    private final String description;
    private final double targetMinutes;

    public PlayTimeObjective(String id, String description, double targetMinutes) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        if (targetMinutes <= 0) {
            throw new IllegalArgumentException("targetMinutes must be positive");
        }
        this.targetMinutes = targetMinutes;
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
        return QuestObjectiveType.PLAY_TIME;
    }

    @Override
    public double getTargetAmount() {
        return targetMinutes;
    }

    @Override
    public boolean matchesPlaytime() {
        return true;
    }
}
