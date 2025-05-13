package com.gregtechceu.gtceu.api.mui.theme;

import com.gregtechceu.gtceu.api.mui.base.IThemeApi;
import com.gregtechceu.gtceu.api.mui.base.drawable.IDrawable;
import com.gregtechceu.gtceu.api.mui.utils.Color;
import com.gregtechceu.gtceu.api.mui.utils.JsonHelper;
import com.google.gson.JsonObject;

public class WidgetSlotTheme extends WidgetTheme {

    private final int slotHoverColor;

    public WidgetSlotTheme(IDrawable background, int slotHoverColor) {
        super(background, null, Color.WHITE.main, 0xFF404040, false);
        this.slotHoverColor = slotHoverColor;
    }

    public WidgetSlotTheme(WidgetTheme parent, JsonObject json, JsonObject fallback) {
        super(parent, json, fallback);
        this.slotHoverColor = JsonHelper.getColorWithFallback(json, fallback, ((WidgetSlotTheme) parent).getSlotHoverColor(), IThemeApi.SLOT_HOVER_COLOR);
    }

    public int getSlotHoverColor() {
        return this.slotHoverColor;
    }
}
