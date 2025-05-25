package com.gregtechceu.gtceu.api.mui.drawable.text;

import com.gregtechceu.gtceu.api.mui.base.drawable.IIcon;
import com.gregtechceu.gtceu.api.mui.base.drawable.INoContextDrawable;
import com.gregtechceu.gtceu.api.mui.theme.WidgetTheme;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Box;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;

public class TextIcon implements IIcon, INoContextDrawable {

    @Getter
    private final Component text;
    @Getter
    private final int width, height;
    private final float scale;
    private final Alignment alignment;
    private static final Box margin = new Box();

    public TextIcon(Component text, int width, int height, float scale, Alignment alignment) {
        this.text = text;
        this.width = width;
        this.height = height;
        this.scale = scale;
        this.alignment = alignment;
    }

    @Override
    public void drawNoContext(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                              int x, int y, int width, int height, WidgetTheme widgetTheme) {
        TextRenderer.SHARED.setPos(x, y);
        TextRenderer.SHARED.setAlignment(this.alignment, width);
        TextRenderer.SHARED.setScale(this.scale);
        TextRenderer.SHARED.drawSimple(poseStack, buffers, this.text);
    }

    @Override
    public Box getMargin() {
        return margin;
    }
}
