package com.gregtechceu.gtceu.api.recipe.gui;

import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.layout.Flow;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

public class GTRecipeViewerWidget extends ParentWidget<GTRecipeViewerWidget> {

    private final GTRecipe recipe;

    public final Flow textComponents;

    public GTRecipeViewerWidget(GTRecipe recipe) {
        this.recipe = recipe;

        textComponents = Flow.col();

    }
}
