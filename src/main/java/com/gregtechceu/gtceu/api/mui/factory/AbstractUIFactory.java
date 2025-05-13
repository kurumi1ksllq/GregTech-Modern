package com.gregtechceu.gtceu.api.mui.factory;

import com.gregtechceu.gtceu.api.mui.base.IGuiHolder;
import com.gregtechceu.gtceu.api.mui.base.UIFactory;
import com.gregtechceu.gtceu.client.mui.screen.ModularPanel;
import com.gregtechceu.gtceu.client.mui.screen.ModularScreen;
import com.gregtechceu.gtceu.client.mui.screen.UISettings;
import com.gregtechceu.gtceu.api.mui.value.sync.PanelSyncManager;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class AbstractUIFactory<T extends GuiData> implements UIFactory<T> {

    private final String name;

    protected AbstractUIFactory(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public final @NotNull String getFactoryName() {
        return this.name;
    }

    @NotNull
    public abstract IGuiHolder<T> getGuiHolder(T data);

    @Override
    public ModularPanel createPanel(T guiData, PanelSyncManager syncManager, UISettings settings) {
        IGuiHolder<T> guiHolder = Objects.requireNonNull(getGuiHolder(guiData), "Gui holder must not be null!");
        return guiHolder.buildUI(guiData, syncManager, settings);
    }

    @Override
    public ModularScreen createScreen(T guiData, ModularPanel mainPanel) {
        IGuiHolder<T> guiHolder = Objects.requireNonNull(getGuiHolder(guiData), "Gui holder must not be null!");
        return guiHolder.createScreen(guiData, mainPanel);
    }

    @SuppressWarnings("unchecked")
    protected IGuiHolder<T> castGuiHolder(Object o) {
        if (!(o instanceof IGuiHolder)) return null;
        try {
            return (IGuiHolder<T>) o;
        } catch (ClassCastException e) {
            return null;
        }
    }
}
