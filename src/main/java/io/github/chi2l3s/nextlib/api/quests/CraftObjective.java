package io.github.chi2l3s.nextlib.api.quests;

import lombok.Getter;
import org.bukkit.Material;

import java.util.Objects;

public class CraftObjective implements QuestObjective {
    private final String id;
    private final String description;
    @Getter
    private final Material material;
    @Getter
    private final int amount;

    public CraftObjective(String id, String description, Material material, int amount) {
        this.id = Objects.requireNonNull(id, "id");
        this.description = Objects.requireNonNull(description, "description");
        this.material = Objects.requireNonNull(material, "material");
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
        return QuestObjectiveType.CRAFT_ITEM;
    }

    @Override
    public double getTargetAmount() {
        return amount;
    }

    @Override
    public boolean matchesCraft(Material material) {
        return this.material == material;
    }
}
