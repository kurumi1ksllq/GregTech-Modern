package com.gregtechceu.gtceu.integration.recipeviewer.widgets;

import brachy.modularui.widgets.layout.Flow;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import brachy.modularui.widget.ParentWidget;

public class GTMuiRecipeWidget extends ParentWidget<GTMuiRecipeWidget> {

    private final GTRecipe recipe;

    public final Flow textComponents;

    public GTMuiRecipeWidget(GTRecipe recipe) {
        this.recipe = recipe;

        textComponents = Flow.col();

    }

    private void initializeWidgets() {}
}
