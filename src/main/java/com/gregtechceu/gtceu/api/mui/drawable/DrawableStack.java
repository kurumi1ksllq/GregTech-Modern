package com.gregtechceu.gtceu.api.mui.drawable;

import com.gregtechceu.gtceu.api.mui.base.IJsonSerializable;
import com.gregtechceu.gtceu.api.mui.base.drawable.IDrawable;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;
import com.gregtechceu.gtceu.api.mui.theme.WidgetTheme;
import com.google.gson.JsonObject;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * A stack of {@link IDrawable} backed by an array which are drawn on top of each other.
 */
public class DrawableStack implements IDrawable, IJsonSerializable {

    public static final IDrawable[] EMPTY_BACKGROUND = {};

    private final IDrawable[] drawables;

    public DrawableStack(IDrawable... drawables) {
        this.drawables = drawables == null || drawables.length == 0 ? EMPTY_BACKGROUND : drawables;
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        for (IDrawable drawable : this.drawables) {
            drawable.draw(context, x, y, width, height, widgetTheme);
        }
    }

    @Override
    public boolean canApplyTheme() {
        for (IDrawable drawable : this.drawables) {
            if (drawable.canApplyTheme()) {
                return true;
            }
        }
        return false;
    }

    public IDrawable[] getDrawables() {
        return this.drawables;
    }

    @Override
    public boolean saveToJson(JsonObject json) {
        // serialized as special case
        // this method should never be called
        throw new IllegalStateException();
    }
}
