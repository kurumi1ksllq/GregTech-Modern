package com.gregtechceu.gtceu.api.mui.drawable;

import com.gregtechceu.gtceu.api.mui.base.drawable.IDrawable;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;
import com.gregtechceu.gtceu.api.mui.theme.WidgetTheme;
import com.gregtechceu.gtceu.api.mui.widget.Widget;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public class SpriteDrawable implements IDrawable {

    private final TextureAtlasSprite sprite;

    public SpriteDrawable(TextureAtlasSprite sprite) {
        this.sprite = sprite;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        GuiDraw.drawSprite(this.sprite, x, y, width, height);
    }

    @Override
    public Widget<?> asWidget() {
        return IDrawable.super.asWidget().size(this.sprite.getIconWidth(), this.sprite.getIconHeight());
    }

    @Override
    public Icon asIcon() {
        return IDrawable.super.asIcon().size(this.sprite.getIconWidth(), this.sprite.getIconHeight());
    }
}
