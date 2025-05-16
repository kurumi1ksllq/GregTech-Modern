package com.gregtechceu.gtceu.api.mui.base;

import net.minecraft.client.gui.components.events.ContainerEventHandler;

public interface IScreenWithMuiScreen extends ContainerEventHandler, IMuiScreen {

    @Override
    default void setFocused(boolean focused) {
        ContainerEventHandler.super.setFocused(focused);
    }
}
