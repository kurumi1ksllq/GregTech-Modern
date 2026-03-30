package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.GTMuiRecipeWidget;

import dev.emi.emi.api.recipe.EmiRecipeCategory;

/**
 * Concrete EMI recipe for GT recipes using MUI2-based recipe widgets.
 */
public class GTRecipeMuiEmiRecipe extends GTEmiRecipe2<GTRecipe, GTMuiRecipeWidget> {

    public GTRecipeMuiEmiRecipe(GTRecipe recipe, EmiRecipeCategory category) {
        super(recipe, category, () -> new GTMuiRecipeWidget(recipe));
    }
}
