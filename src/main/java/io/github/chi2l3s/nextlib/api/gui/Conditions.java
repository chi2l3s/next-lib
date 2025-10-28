package io.github.chi2l3s.nextlib.api.gui;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public final class Conditions {
    private static final Map<String, GuiConditionFactory> REGISTERED_CONDITIONS = new ConcurrentHashMap<>();

    static {
        registerCondition("always", (manager, args) -> player -> true);
        registerCondition("never", (manager, args) -> player -> false);
    }

    private Conditions() {
    }

    public static void registerCondition(String name, GuiConditionFactory factory) {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(factory, "factory");
        REGISTERED_CONDITIONS.put(name.toLowerCase(), factory);
    }

    public static GuiCondition create(String rawCondition, GuiManager manager) {
        if (rawCondition == null) {
            return null;
        }

        String trimmed = rawCondition.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        String conditionName;
        String arguments;

        int separatorIndex = trimmed.indexOf(' ');
        if (separatorIndex == -1) {
            conditionName = trimmed.toLowerCase();
            arguments = "";
        } else {
            conditionName = trimmed.substring(0, separatorIndex).toLowerCase();
            arguments = trimmed.substring(separatorIndex + 1).trim();
        }

        GuiConditionFactory factory = REGISTERED_CONDITIONS.get(conditionName);
        if (factory == null) {
            return null;
        }

        return factory.create(manager, arguments);
    }
}
