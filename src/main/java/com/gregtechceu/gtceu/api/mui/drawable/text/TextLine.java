package com.gregtechceu.gtceu.api.mui.drawable.text;

import com.gregtechceu.gtceu.api.mui.base.drawable.ITextLine;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;

public class TextLine implements ITextLine {

    private final Component text;
    @Getter
    private final int width;

    private float lastX, lastY;

    public TextLine(Component text, int width) {
        this.text = text;
        this.width = width;
    }

    @Override
    public int getHeight(Font font) {
        return font.lineHeight + 1;
    }

    @Override
    public void draw(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Font font,
                     float x, float y, int color, boolean shadow) {
        font.drawInBatch(this.text, x, y, color, shadow, poseStack.last().pose(), buffers,
                Font.DisplayMode.NORMAL, 0, 0xf000f0);
        this.lastX = x;
        this.lastY = y;
    }

    @Override
    public Object getHoveringElement(Font font, int x, int y) {
        if (y < lastY || y > lastY + getHeight(font)) return null;
        // not hovering, but we know that nothing else is hovered either
        if (x < lastX || x > lastX + getWidth()) return Boolean.FALSE;
        return this.text;
    }
}
