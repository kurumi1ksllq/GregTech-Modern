package com.gregtechceu.gtceu.api.ui;

import com.gregtechceu.gtceu.api.ui.base.BaseContainerScreen;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import org.jetbrains.annotations.NotNull;

public class UIContainerScreen extends BaseContainerScreen<StackLayout, UIContainerMenu<?>> {

    public UIContainerScreen(UIContainerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @SuppressWarnings({ "unchecked", "DataFlowIssue", "rawtypes" })
    @Override
    protected @NotNull UIAdapter<StackLayout> createAdapter() {
        return ((UIContainerMenu) menu).getFactory().createAdapter(menu.player(), menu.getHolder(), this);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected void build(StackLayout rootComponent) {
        ((UIContainerMenu) menu).getFactory().loadClientUI(menu.player(), this.uiAdapter, menu.getHolder());
    }
}
