package com.gregtechceu.gtceu.api.mui.drawable.text;

import com.gregtechceu.gtceu.api.mui.base.IThemeApi;
import com.gregtechceu.gtceu.api.mui.base.drawable.IHoverable;
import com.gregtechceu.gtceu.api.mui.base.drawable.IIcon;
import com.gregtechceu.gtceu.api.mui.base.drawable.ITextLine;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;
import net.minecraft.client.gui.Font;

import java.util.List;

public class ComposedLine implements ITextLine {

    private final List<Object> elements;
    private final int width;
    private final int height;

    private float lastX, lastY;

    public ComposedLine(List<Object> elements, int width, int height) {
        this.elements = elements;
        this.width = width;
        this.height = height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight(Font fr) {
        return height == fr.lineHeight ? height : height + 1;
    }

    @Override
    public void draw(GuiContext context, Font fr, float x, float y, int color, boolean shadow) {
        this.lastX = x;
        this.lastY = y;
        for (Object o : this.elements) {
            if (o instanceof String s) {
                float drawY = getHeight(fr) / 2f - fr.lineHeight / 2f;
                fr.drawString(s, x, (int) (y + drawY), color, shadow);
                x += fr.getStringWidth(s);
            } else if (o instanceof IIcon icon) {
                float drawY = getHeight(fr) / 2f - icon.getHeight() / 2f;
                icon.draw(context, (int) x, (int) (y + drawY), icon.getWidth(), icon.getHeight(), IThemeApi.get().getDefaultTheme().getFallback());
                if (icon instanceof IHoverable hoverable) {
                    hoverable.setRenderedAt((int) x, (int) (y + drawY));
                }
                x += icon.getWidth();
            }
        }
    }

    @Override
    public Object getHoveringElement(Font fr, int x, int y) {
        int h0 = getHeight(fr);
        if (y < lastY || y > lastY + h0) return null; // is not hovering vertically
        if (x < lastX || x > lastX + getWidth()) return Boolean.FALSE; // is not hovering horizontally
        float x0 = x - this.lastX; // origin to 0
        float x1 = 0;
        float y0 = y - this.lastY; // origin to 0
        for (Object o : this.elements) {
            float w, h;
            if (o instanceof String s) {
                w = fr.getStringWidth(s);
                h = fr.lineHeight;
            } else if (o instanceof IIcon icon) {
                w = icon.getWidth();
                h = icon.getWidth();
            } else continue;
            if (x0 > x1 && x0 < x1 + w) {
                // is inside horizontally
                if (h < h0) {
                    // is smaller than line height
                    int lower = (int) (h0 / 2f - h / 2);
                    int upper = (int) (h0 / 2f + h / 2 - 1f);
                    if (y0 < lower || y0 > upper) return Boolean.FALSE; // is outside vertically
                }
                return o; // found hovering
            }
            x1 += w; // move to next element
        }
        return null;
    }
}
