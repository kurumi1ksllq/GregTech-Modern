package com.gregtechceu.gtceu.api.recipe.gui;

import brachy.modularui.value.sync.DoubleSyncValue;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ProgressWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.layout.Flow;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;

public class GTRecipeViewerUILayout {

    public final GTRecipeTypeUILayout layout;

    private ParentWidget<? extends ParentWidget<?>> overrideWidget;

    public GTRecipeViewerUILayout(GTRecipeTypeUILayout layout) {
        this.layout = layout;
    }

    public GTRecipeViewerUILayout overrideWidget(ParentWidget<? extends ParentWidget<?>> overrideWidget) {
        this.overrideWidget = overrideWidget;
        return this;
    }

    public ParentWidget<?> getRecipeWidget() {
        return new ParentWidget<>();
    }
}
