package com.gregtechceu.gtceu.client.mui.screen;

import com.gregtechceu.gtceu.api.mui.base.IMuiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

@SideOnly(Side.CLIENT)
public class ScreenWrapper extends Screen implements IMuiScreen {

    private final ModularScreen screen;

    public ScreenWrapper(ModularScreen screen) {
        super(Component.empty());
        this.screen = screen;
        this.screen.construct(this);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics) {
        handleDrawBackground(guiGraphics, super::renderBackground);
    }

    @Override
    public @NotNull ModularScreen getScreen() {
        return this.screen;
    }

    @Override
    public boolean isPauseScreen() {
        return this.screen == null || this.screen.isPauseScreen();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        IMuiScreen.super.setFocused(focused);
    }

}
