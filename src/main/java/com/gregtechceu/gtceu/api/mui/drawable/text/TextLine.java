package com.gregtechceu.gtceu.api.mui.drawable.text;

import com.gregtechceu.gtceu.api.mui.base.drawable.ITextLine;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;
import net.minecraft.client.gui.Font;

public class TextLine implements ITextLine {

    private final String text;
    private final int width;

    private float lastX, lastY;

    public TextLine(String text, int width) {
        this.text = text;
        this.width = width;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight(Font font) {
        return font.lineHeight + 1;
    }

    @Override
    public void draw(GuiContext context, Font font, float x, float y, int color, boolean shadow) {
        font.drawString(this.text, x, y, color, shadow);
        this.lastX = x;
        this.lastY = y;
    }

    @Override
    public Object getHoveringElement(Font font, int x, int y) {
        if (y < lastY || y > lastY + getHeight(font)) return null;
        if (x < lastX || x > lastX + getWidth()) return Boolean.FALSE; // not hovering, but we know that nothing else is hovered either
        return this.text;
    }
}
