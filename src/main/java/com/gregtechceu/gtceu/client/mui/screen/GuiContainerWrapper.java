package com.gregtechceu.gtceu.client.mui.screen;

import com.gregtechceu.gtceu.api.mui.base.IMuiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class GuiContainerWrapper extends AbstractContainerScreen<ModularContainerMenu> implements IMuiScreen {

    private final ModularScreen screen;

    public GuiContainerWrapper(ModularContainerMenu container, ModularScreen screen) {
        super(container, container.getPlayer().getInventory(), Component.empty());
        this.screen = screen;
        this.screen.construct(this);
    }

    @Override
    public void renderBackground(@NotNull GuiGraphics guiGraphics) {
        handleDrawBackground(guiGraphics, super::renderBackground);
    }

    @Override
    protected void renderBg(@NotNull GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {}

    @Override
    public @NotNull ModularScreen getScreen() {
        return this.screen;
    }

    @Override
    public boolean isPauseScreen() {
        return this.screen != null && this.screen.isPauseScreen();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        IMuiScreen.super.setFocused(focused);
    }
}
