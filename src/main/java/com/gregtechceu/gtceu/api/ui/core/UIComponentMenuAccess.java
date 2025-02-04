package com.gregtechceu.gtceu.api.ui.core;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public interface UIComponentMenuAccess {

    AbstractContainerScreen<?> screen();

    UIAdapter<?> adapter();
}
