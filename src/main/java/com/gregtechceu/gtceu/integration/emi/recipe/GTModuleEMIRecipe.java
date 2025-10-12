package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.item.module.ItemModule;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.recipe.type.EquipmentFoundryRecipe;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
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
        return 200;
    }

    @Override
    public int getDisplayHeight() {
        int height = 35;
        for (ItemModule module : recipe.getModules()) {
            height += Minecraft.getInstance().font.wordWrapHeight(module.getInfo(), getDisplayWidth() - 8);
        }
        return height;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        Font font = Minecraft.getInstance().font;
        Component appliedTo = Component.translatable("gtceu.equipment_foundry.gui.applied_to");
        Component moduleItem = Component.translatable("gtceu.equipment_foundry.gui.module_item");
        widgets.addText(appliedTo, 4, 8, 0xFFFFFFFF, true);
        widgets.addText(moduleItem, font.width(appliedTo) + 36, 8, 0xFFFFFFFF, true);
        widgets.addSlot(EmiIngredient.of(recipe.getEquipment()), 4 + font.width(appliedTo), 4);
        widgets.addSlot(EmiIngredient.of(recipe.getIngredient()), 40 + font.width(appliedTo) + font.width(moduleItem),
                4);
        int y = 30;
        for (ItemModule module : recipe.getModules()) {
            Component component = module.getInfo();
            for (FormattedCharSequence line : font.split(component, getDisplayWidth() - 8)) {
                widgets.addText(line, 4, y, 0xFFFFFFFF, true);
                y += 9;
            }
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
