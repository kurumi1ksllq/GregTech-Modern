package com.gregtechceu.gtceu.api.mui.base.drawable;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;

import com.mojang.blaze3d.vertex.PoseStack;

public interface ITextLine {

    int getWidth();

    int getHeight(Font font);

    void draw(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Font font,
              float x, float y, int color, boolean shadow);

    Object getHoveringElement(Font font, int x, int y);
}
