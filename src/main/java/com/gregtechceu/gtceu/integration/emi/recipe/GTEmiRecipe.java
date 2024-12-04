package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.integration.xei.widgets.GTRecipeWidget;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import org.jetbrains.annotations.Nullable;

public class GTEmiRecipe extends ModularEmiRecipe<WidgetGroup> {

    @Getter
    final EmiRecipeCategory category;
    final RecipeHolder<GTRecipe> recipe;

    public GTEmiRecipe(RecipeHolder<GTRecipe> recipe, EmiRecipeCategory category) {
        super(() -> new GTRecipeWidget(recipe));
        this.category = category;
        this.recipe = recipe;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return recipe.id();
    }
}
