package com.gregtechceu.gtceu.common.cover.data;

import com.gregtechceu.gtceu.api.gui.widget.EnumSelectorWidget;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;

public enum DistributionMode implements EnumSelectorWidget.SelectableEnum {

    EQUALIZED("equalized"),
    ROUND_ROBIN("round_robin"),
    FLOOD("flood");

    public static final DistributionMode[] VALUES = values();
    private static final float OFFSET = 1.0f / VALUES.length;

    public final String localeName;

    DistributionMode(String localeName) {
        this.localeName = localeName;
    }

    @Override
    public String getTooltip() {
        return "cover.generic.distribution." + localeName;
    }

    @Override
    public IGuiTexture getIcon() {
        return new ResourceTexture("gtceu:textures/gui/icon/distribution_mode/" + localeName + ".png");
    }
}
