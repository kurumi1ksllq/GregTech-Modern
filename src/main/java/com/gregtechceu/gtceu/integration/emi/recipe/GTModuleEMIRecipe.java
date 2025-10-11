package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.item.module.ItemModule;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.recipe.type.EquipmentFoundryRecipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Ingredient;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GTModuleEMIRecipe implements EmiRecipe {

    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            GTCEu.id("equipment_foundry"),
            EmiIngredient.of(Ingredient.of(GTBlocks.EQUIPMENT_FOUNDRY)));

    private final EquipmentFoundryRecipe recipe;

    public GTModuleEMIRecipe(EquipmentFoundryRecipe recipe) {
        this.recipe = recipe;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(EmiIngredient.of(recipe.getEquipment()), EmiIngredient.of(recipe.getIngredient()));
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of();
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return List.of(EmiIngredient.of(Ingredient.of(GTBlocks.EQUIPMENT_FOUNDRY)));
    }

    @Override
    public int getDisplayWidth() {
        return 134;
    }

    @Override
    public int getDisplayHeight() {
        return 200;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(EmiIngredient.of(recipe.getEquipment()), 32, 16);
        widgets.addSlot(EmiIngredient.of(recipe.getIngredient()), 64, 16);
        int y = 32;
        for (ItemModule module : recipe.getModules()) {
            widgets.addText(module.getInfo(), 16, y, 0xFFFFFFFF, true);
            y += 9;
        }
    }

    public static void addRecipes(EmiRegistry registry) {
        registry.getRecipeManager()
                .getAllRecipesFor(GTRecipeTypes.EQUIPMENT_FOUNDRY_RECIPES.get())
                .stream()
                .map(GTModuleEMIRecipe::new)
                .forEach(registry::addRecipe);
    }
}
