package io.github.chi2l3s.nextlib.api.quests;

import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.function.Predicate;

public class QuestProgress {
    @Getter
    private final Quest quest;
    @Getter
    private final UUID playerId;
    private final Map<String, Double> progress;

    public QuestProgress(Quest quest, UUID playerId) {
        this(quest, playerId, new HashMap<>());
    }

    QuestProgress(Quest quest, UUID playerId, Map<String, Double> storedProgress) {
        this.quest = Objects.requireNonNull(quest, "quest");
        this.playerId = Objects.requireNonNull(playerId, "playerId");
        this.progress = new HashMap<>();
        quest.getObjectives().forEach(objective ->
                this.progress.put(objective.getId(), storedProgress.getOrDefault(objective.getId(), 0.0)));
    }

    public double getProgress(String objectiveId) {
        return progress.getOrDefault(objectiveId, 0.0);
    }

    public Map<String, Double> getAllProgress() {
        return Collections.unmodifiableMap(progress);
    }

    public boolean isObjectiveComplete(QuestObjective objective) {
        return getProgress(objective.getId()) >= objective.getTargetAmount();
    }

    public boolean isComplete() {
        for (QuestObjective objective : quest.getObjectives()) {
            if (!isObjectiveComplete(objective)) return false;
        }
        return true;
    }

    public boolean applyKill(EntityType entityType, boolean playerKill) {
        return applyIncrement(objective -> objective.matchesKill(entityType, playerKill), 1.0);
    }

    public boolean applyTravel(double meters) {
        if (meters < 0) return false;
        return applyIncrement(QuestObjective::matchesTravel, meters);
    }

    public boolean applyCraft(Material material, int amount) {
        if (amount <= 0) {
            return false;
        }
        return applyIncrement(objective -> objective.matchesCraft(material), amount);
    }

    public boolean applyPlaytime(double minutes) {
        if (minutes <= 0) {
            return false;
        }
        return applyIncrement(QuestObjective::matchesPlaytime, minutes);
    }

    public boolean applyBlockBreak(Material blockType, int amount) {
        if (amount <= 0) {
            return false;
        }
        return applyIncrement(objective -> objective.matchesBlockBreak(blockType), amount);
    }

    public boolean applyBlockPlace(Material blockType, int amount) {
        if (amount <= 0) {
            return false;
        }
        return applyIncrement(objective -> objective.matchesBlockPlace(blockType), amount);
    }

    public boolean applySmelt(Material resultType, int amount) {
        if (amount <= 0) {
            return false;
        }
        return applyIncrement(objective -> objective.matchesSmelt(resultType), amount);
    }

    public boolean applyBreed(EntityType entityType, int amount) {
        if (amount <= 0) {
            return false;
        }
        return applyIncrement(objective -> objective.matchesBreed(entityType), amount);
    }

    public boolean applyTame(EntityType entityType, int amount) {
        if (amount <= 0) {
            return false;
        }
        return applyIncrement(objective -> objective.matchesTame(entityType), amount);
    }

    public boolean applyFish(Material caughtType, int amount) {
        if (amount <= 0) {
            return false;
        }
        return applyIncrement(objective -> objective.matchesFish(caughtType), amount);
    }

    public boolean applyConsume(Material material, int amount) {
        if (amount <= 0) {
            return false;
        }
        return applyIncrement(objective -> objective.matchesConsume(material), amount);
    }

    public boolean applyCustom(String eventType, double amount, Object payload) {
        if (amount <= 0) {
            return false;
        }
        return applyIncrement(objective -> objective.matchesCustom(eventType, payload), amount);
    }

    private boolean applyIncrement(Predicate<QuestObjective> predicate, double amount) {
        boolean changed = false;
        for (QuestObjective objective: quest.getObjectives()) {
            if (!predicate.test(objective)) {
                continue;
            }
            double current = progress.getOrDefault(objective.getId(), 0.0);
            double target = objective.getTargetAmount();
            if (current >= target) {
                continue;
            }
            double updated = Math.min(target, current + amount);
            if (updated != current) {
                progress.put(objective.getId(), updated);
                changed = true;
            }
        }
        return changed;
    }
}
