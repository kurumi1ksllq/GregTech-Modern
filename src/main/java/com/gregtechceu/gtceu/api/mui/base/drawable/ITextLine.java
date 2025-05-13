package com.gregtechceu.gtceu.api.mui.base.drawable;

import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;

import net.minecraft.client.gui.Font;

public interface ITextLine {

    int getWidth();

    int getHeight(Font fr);

    void draw(GuiContext context, Font fr, float x, float y, int color, boolean shadow);

    Object getHoveringElement(Font fr, int x, int y);

}
