package io.github.chi2l3s.nextlib.api.quests;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public final class Quest {
    private final String id;
    private final String name;
    private final String description;
    private final boolean repeatable;
    private final List<QuestObjective> objectives;

    public Quest(String id, String name, String description, List<QuestObjective> objectives) {
        this(id, name, description, objectives, false);
    }

    public Quest(String id, String name, String description, List<QuestObjective> objectives, boolean repeatable) {
        this.id = Objects.requireNonNull(id, "id");
        this.name = Objects.requireNonNull(name, "name");
        this.description = Objects.requireNonNull(description, "description");
        this.objectives = List.copyOf(Objects.requireNonNull(objectives, "objectives"));
        this.repeatable = repeatable;
    }
}
