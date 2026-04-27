package com.gregtechceu.gtceu.api.recipe.gui;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.layout.Flow;

public class GTRecipeViewerWidget extends ParentWidget<GTRecipeViewerWidget> {

    private final GTRecipe recipe;

    public final Flow textComponents;

    public GTRecipeViewerWidget(GTRecipe recipe) {
        this.recipe = recipe;

        textComponents = Flow.col();
    }
}
