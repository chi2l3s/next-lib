package io.github.chi2l3s.nextlib.api.gui;

@FunctionalInterface
public interface GuiActionFactory {
    GuiAction create(GuiManager manager, String arguments);
}
