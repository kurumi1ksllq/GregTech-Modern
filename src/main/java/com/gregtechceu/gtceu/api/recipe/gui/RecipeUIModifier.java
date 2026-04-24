package com.gregtechceu.gtceu.api.recipe.gui;

import brachy.modularui.api.drawable.Text;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.GTMuiRecipeWidget;

@FunctionalInterface
public interface RecipeUIModifier {

    void buildRecipeUI(GTRecipe recipe, GTMuiRecipeWidget widget);

    /**
     * A recipe ui modifier that adds a line of the text to the recipe UI
     * @param text Text to add
     * @return Recipe ui modifier
     */
    static RecipeUIModifier textLine(Text text) {
        return (recipe, widget) -> {
            widget.textComponents.child(text.asWidget());
        };
    }

    default RecipeUIModifier then(RecipeUIModifier... modifiers) {
        return (recipe, widget) -> {
            buildRecipeUI(recipe, widget);
            for (var modifier: modifiers) {
                modifier.buildRecipeUI(recipe, widget);
            }
        };
    }

    static RecipeUIModifier all(RecipeUIModifier... modifiers) {
        return (recipe, widget) -> {
            for (var modifier: modifiers) {
                modifier.buildRecipeUI(recipe, widget);
            }
        };
    }
}
