package com.gregtechceu.gtceu.api.mui.base.drawable;

import com.gregtechceu.gtceu.api.mui.theme.WidgetTheme;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.PoseStack;

public interface INoContextDrawable extends IDrawable {

    @OnlyIn(Dist.CLIENT)
    @Override
    default void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        drawNoContext(context.getGraphics().pose(), context.getGraphics().bufferSource(),
                x, y, width, height, widgetTheme);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    void drawNoContext(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                       int x, int y, int width, int height, WidgetTheme widgetTheme);
}
