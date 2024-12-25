package com.gregtechceu.gtceu.api.ui.core;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Consumer;

public interface UIComponentMenuAccess {

    AbstractContainerScreen<?> screen();

    UIAdapter<?> adapter();
}
