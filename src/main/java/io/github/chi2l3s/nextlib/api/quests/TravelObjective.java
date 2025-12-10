package io.github.chi2l3s.nextlib.api.quests;

import lombok.Getter;

import java.util.Objects;

public class TravelObjective implements QuestObjective {
    private final String id;
    private final String description;
    @Getter
    private final double meters;

    public TravelObjective(String id, String description, double meters) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        this.meters = meters;
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
        return QuestObjectiveType.TRAVEL_DISTANCE;
    }

    @Override
    public double getTargetAmount() {
        return meters;
    }

    @Override
    public boolean matchesTravel() {
        return true;
    }
}
