package io.github.chi2l3s.nextlib.api.gui;

@FunctionalInterface
public interface GuiConditionFactory {
    GuiCondition create(GuiManager manager, String arguments);
}