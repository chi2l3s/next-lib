package io.github.chi2l3s.nextlib.api.quests;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * Abstraction for persisting and loading quest progress.
 */
public interface QuestStore {
    void saveProgress(QuestProgress progress);

    Optional<QuestProgress> loadProgress(UUID playerId, Quest quest);

    List<QuestProgress> loadAll(UUID playerId, Function<String, Optional<Quest>> questResolver);
}
