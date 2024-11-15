package com.gregtechceu.gtceu.integration.rei.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.integration.GTRecipeWidget;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.rei.ModularDisplay;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.Optional;

public class GTRecipeDisplay extends ModularDisplay<WidgetGroup> {

    private final RecipeHolder<GTRecipe> recipe;

    public GTRecipeDisplay(RecipeHolder<GTRecipe> recipe) {
        super(() -> new GTRecipeWidget(recipe), GTRecipeREICategory.CATEGORIES.apply(recipe.recipeCategory));
        this.recipe = recipe;
    }

    public GTRecipeDisplay(RecipeHolder<GTRecipe> recipe, GTRecipeCategory category) {
        super(() -> new GTRecipeWidget(recipe), GTRecipeREICategory.CATEGORIES.apply(category));
        this.recipe = recipe;
    }

    @Override
    public Optional<ResourceLocation> getDisplayLocation() {
        return Optional.of(recipe.id());
    }
}
