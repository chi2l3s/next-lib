package io.github.chi2l3s.nextlib.api.quests;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class QuestManager {
    private final QuestStore store;
    private final Map<String, Quest> quests = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, QuestProgress>> progressByPlayer = new ConcurrentHashMap<>();

    public QuestManager(QuestStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public Quest registerQuest(Quest quest) {
        Objects.requireNonNull(quest, "quest");
        quests.put(quest.getId(), quest);
        return quest;
    }

    public Optional<Quest> getQuest(String questId) {
        return Optional.ofNullable(quests.get(questId));
    }

    public Collection<Quest> getRegisteredQuests() {
        return Collections.unmodifiableCollection(quests.values());
    }

    public QuestProgress activateQuest(UUID playerId, String questId) {
        Quest quest = getQuest(questId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown quest id: " + questId));
        QuestProgress progress = store.loadProgress(playerId, quest)
                .orElseGet(() -> new QuestProgress(quest, playerId));
        progressByPlayer.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(questId, progress);
        return progress;
    }

    public Optional<QuestProgress> getActiveProgress(UUID playerId, String questId) {
        Map<String, QuestProgress> map = progressByPlayer.get(playerId);
        if (map == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(map.get(questId));
    }

    public void deactivateQuest(UUID playerId, String questId) {
        Map<String, QuestProgress> map = progressByPlayer.get(playerId);
        if (map != null) {
            map.remove(questId);
            if (map.isEmpty()) {
                progressByPlayer.remove(playerId);
            }
        }
    }

    public Collection<QuestProgress> restorePlayer(UUID playerId) {
        Map<String, QuestProgress> map = progressByPlayer.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>());
        store.loadAll(playerId, this::getQuest)
                .forEach(progress -> map.put(progress.getQuest().getId(), progress));
        return Collections.unmodifiableCollection(map.values());
    }

    public void recordKill(UUID playerId, EntityType entityType, boolean playerKill) {
        applyUpdate(playerId, progress -> progress.applyKill(entityType, playerKill));
    }

    public void recordTravel(UUID playerId, double meters) {
        applyUpdate(playerId, progress -> progress.applyTravel(meters));
    }

    public void recordCraft(UUID playerId, Material material, int amount) {
        applyUpdate(playerId, progress -> progress.applyCraft(material, amount));
    }

    public void recordPlaytime(UUID playerId, double minutes) {
        applyUpdate(playerId, progress -> progress.applyPlaytime(minutes));
    }

    public void recordBlockBreak(UUID playerId, Material blockType, int amount) {
        applyUpdate(playerId, progress -> progress.applyBlockBreak(blockType, amount));
    }

    public void recordBlockBreak(UUID playerId, Material blockType) {
        recordBlockBreak(playerId, blockType, 1);
    }

    public void recordBlockPlace(UUID playerId, Material blockType, int amount) {
        applyUpdate(playerId, progress -> progress.applyBlockPlace(blockType, amount));
    }

    public void recordBlockPlace(UUID playerId, Material blockType) {
        recordBlockPlace(playerId, blockType, 1);
    }

    public void recordSmelt(UUID playerId, Material resultType, int amount) {
        applyUpdate(playerId, progress -> progress.applySmelt(resultType, amount));
    }

    public void recordSmelt(UUID playerId, Material resultType) {
        recordSmelt(playerId, resultType, 1);
    }

    public void recordBreed(UUID playerId, EntityType entityType, int amount) {
        applyUpdate(playerId, progress -> progress.applyBreed(entityType, amount));
    }

    public void recordBreed(UUID playerId, EntityType entityType) {
        recordBreed(playerId, entityType, 1);
    }

    public void recordTame(UUID playerId, EntityType entityType, int amount) {
        applyUpdate(playerId, progress -> progress.applyTame(entityType, amount));
    }

    public void recordTame(UUID playerId, EntityType entityType) {
        recordTame(playerId, entityType, 1);
    }

    public void recordFish(UUID playerId, Material caughtType, int amount) {
        applyUpdate(playerId, progress -> progress.applyFish(caughtType, amount));
    }

    public void recordFish(UUID playerId, Material caughtType) {
        recordFish(playerId, caughtType, 1);
    }

    public void recordConsume(UUID playerId, Material material, int amount) {
        applyUpdate(playerId, progress -> progress.applyConsume(material, amount));
    }

    public void recordConsume(UUID playerId, Material material) {
        recordConsume(playerId, material, 1);
    }

    /**
     * Применяет пользовательское событие ко всем активным квестам игрока. {@code eventType} может быть любым идентификатором,
     * который использует ваш плагин (например "open-gift"), а {@code payload} - любой объект с контекстом события.
     */
    public void recordCustom(UUID playerId, String eventType, double amount, Object payload) {
        Objects.requireNonNull(eventType, "eventType");
        applyUpdate(playerId, progress -> progress.applyCustom(eventType, amount, payload));
    }

    public void recordCustom(UUID playerId, String eventType) {
        recordCustom(playerId, eventType, 1.0, null);
    }

    private void applyUpdate(UUID playerId, Function<QuestProgress, Boolean> updater) {
        Map<String, QuestProgress> progressMap = progressByPlayer.get(playerId);
        if (progressMap == null || progressMap.isEmpty()) {
            return;
        }
        for (QuestProgress progress : progressMap.values()) {
            boolean changed = Boolean.TRUE.equals(updater.apply(progress));
            if (changed) {
                store.saveProgress(progress);
            }
        }
    }
}
