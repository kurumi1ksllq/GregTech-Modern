package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;

import com.lowdragmc.lowdraglib.Platform;

import net.minecraft.Util;
import net.minecraft.network.chat.Component;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.render.EmiRenderable;
import dev.emi.emi.api.stack.EmiStack;

import java.util.function.Function;

public class GTRecipeEMICategory extends EmiRecipeCategory {

    public static final Function<GTRecipeCategory, GTRecipeEMICategory> CATEGORIES = Util
            .memoize(GTRecipeEMICategory::new);
    private final GTRecipeCategory category;

    private GTRecipeEMICategory(GTRecipeCategory category) {
        super(category.registryKey, toDrawable(category.getIcon(), 16, 16));
        this.category = category;
    }

    public static void registerDisplays(EmiRegistry registry) {
        for (GTRecipeCategory category : GTRegistries.RECIPE_CATEGORIES) {
            if (!category.shouldRegisterDisplays()) continue;
            var type = category.getRecipeType();
            if (category == type.getCategory()) type.buildRepresentativeRecipes();
            EmiRecipeCategory emiCategory = CATEGORIES.apply(category);
            type.getRecipesInCategory(category).stream()
                    .map(recipe -> new GTEMIRecipe(recipe, emiCategory))
                    .forEach(registry::addRecipe);
        }
    }

    public static void registerWorkStations(EmiRegistry registry) {
        for (MachineDefinition machine : GTRegistries.MACHINES) {
            if (machine.getRecipeTypes() == null) continue;
            for (GTRecipeType type : machine.getRecipeTypes()) {
                if (type == null) continue;
                for (GTRecipeCategory category : type.getCategories()) {
                    if (!category.isXEIVisible() && !GTCEu.isDev()) continue;
                    registry.addWorkstation(machineCategory(category), EmiStack.of(machine.asStack()));
                }
            }
        }
    }

    public static EmiRecipeCategory machineCategory(GTRecipeCategory category) {
        if (category == GTRecipeTypes.FURNACE_RECIPES.getCategory()) return VanillaEmiRecipeCategories.SMELTING;
        else return CATEGORIES.apply(category);
    }

    @Override
    public Component getName() {
        return Component.translatable(category.getLanguageKey());
    }

    static EmiRenderable toDrawable(UITexture guiTexture, int width, int height) {
        return (graphics, x, y, delta) -> {
            if (guiTexture == null) return;
            if (!(graphics instanceof UIGuiGraphics)) graphics = UIGuiGraphics.of(graphics);
            var uiGraphics = (UIGuiGraphics) graphics;
            guiTexture.draw(uiGraphics, 0, 0, x, y, width, height);
        };
    }
}
