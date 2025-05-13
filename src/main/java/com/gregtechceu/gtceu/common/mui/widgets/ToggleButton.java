package com.gregtechceu.gtceu.common.mui.widgets;

import com.gregtechceu.gtceu.api.mui.base.ITheme;
import com.gregtechceu.gtceu.api.mui.base.drawable.IDrawable;
import com.gregtechceu.gtceu.api.mui.base.value.IBoolValue;
import com.gregtechceu.gtceu.client.mui.screen.RichTooltip;
import com.gregtechceu.gtceu.api.mui.theme.WidgetTheme;
import com.gregtechceu.gtceu.api.mui.theme.WidgetThemeSelectable;

import java.util.function.Consumer;

public class ToggleButton extends AbstractCycleButtonWidget<ToggleButton> {

    private boolean invert = false;

    public ToggleButton() {
        stateCount(2);
    }

    @Override
    public WidgetTheme getWidgetThemeInternal(ITheme theme) {
        WidgetThemeSelectable widgetTheme = theme.getToggleButtonTheme();
        return isValueSelected() ^ invertSelected() ? widgetTheme.getSelected() : widgetTheme;
    }

    public boolean isValueSelected() {
        return getState() == 1;
    }

    public ToggleButton value(IBoolValue<?> boolValue) {
        return super.value(boolValue);
    }

    public ToggleButton selectedBackground(IDrawable... selectedBackground) {
        return background(true, selectedBackground);
    }

    public ToggleButton selectedHoverBackground(IDrawable... selectedHoverBackground) {
        return hoverBackground(true, selectedHoverBackground);
    }

    @Override
    public ToggleButton background(IDrawable... selectedBackground) {
        return background(false, selectedBackground);
    }

    @Override
    public ToggleButton hoverBackground(IDrawable... selectedHoverBackground) {
        return hoverBackground(false, selectedHoverBackground);
    }

    public ToggleButton background(boolean selected, IDrawable... background) {
        this.background = addToArray(this.background, background, selected ? 1 : 0);
        return this;
    }

    public ToggleButton overlay(boolean selected, IDrawable... overlay) {
        this.overlay = addToArray(this.overlay, overlay, selected ? 1 : 0);
        return this;
    }

    public ToggleButton hoverBackground(boolean selected, IDrawable... background) {
        this.hoverBackground = addToArray(this.hoverBackground, background, selected ? 1 : 0);
        return this;
    }

    public ToggleButton hoverOverlay(boolean selected, IDrawable... overlay) {
        this.hoverOverlay = addToArray(this.hoverOverlay, overlay, selected ? 1 : 0);
        return this;
    }

    public ToggleButton addTooltip(boolean selected, String tooltip) {
        return super.addTooltip(selected ? 1 : 0, tooltip);
    }

    public ToggleButton addTooltip(boolean selected, IDrawable tooltip) {
        return super.addTooltip(selected ? 1 : 0, tooltip);
    }

    public ToggleButton tooltip(boolean selected, Consumer<RichTooltip> builder) {
        return super.tooltip(selected ? 1 : 0, builder);
    }

    public ToggleButton tooltipBuilder(boolean selected, Consumer<RichTooltip> builder) {
        return super.tooltipBuilder(selected ? 1 : 0, builder);
    }

    public ToggleButton invertSelected(boolean invert) {
        this.invert = invert;
        return getThis();
    }

    public boolean invertSelected() {
        return this.invert;
    }
}
