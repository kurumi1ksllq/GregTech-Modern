package com.gregtechceu.gtceu.api.mui.drawable;

import com.gregtechceu.gtceu.api.mui.base.drawable.INoContextDrawable;
import com.gregtechceu.gtceu.api.mui.theme.WidgetTheme;
import com.gregtechceu.gtceu.api.mui.widget.Widget;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.vertex.PoseStack;

public class SpriteDrawable implements INoContextDrawable {

    private final TextureAtlasSprite sprite;

    public SpriteDrawable(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void drawNoContext(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                              int x, int y, int width, int height, WidgetTheme widgetTheme) {
        GuiDraw.drawSprite(poseStack.last().pose(), this.sprite, x, y, width, height);
    }

    @Override
    public Widget<?> asWidget() {
        return INoContextDrawable.super.asWidget().size(this.sprite.contents().width(),
                this.sprite.contents().height());
    }

    @Override
    public Icon asIcon() {
        return INoContextDrawable.super.asIcon().size(this.sprite.contents().width(), this.sprite.contents().height());
    }
}
