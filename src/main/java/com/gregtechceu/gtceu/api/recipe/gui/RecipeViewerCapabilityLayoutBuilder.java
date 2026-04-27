package com.gregtechceu.gtceu.api.recipe.gui;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;

import brachy.modularui.api.widget.IWidget;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface RecipeViewerCapabilityLayoutBuilder {

    /**
     * Builds and attaches the UI for a specific capability in a recipe viewer UI.
     *
     * @param recipe The recipe this UI is for.
     * @param layout The {@link GTRecipeTypeUILayout} which holds UI layout data.
     * @param io     The IO mode widgets are being created for.
     */
    @Nullable
    IWidget createCapabilityUILayout(GTRecipe recipe, GTRecipeTypeUILayout layout, IO io);
}
