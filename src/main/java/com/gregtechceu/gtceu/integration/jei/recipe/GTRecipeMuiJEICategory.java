package com.gregtechceu.gtceu.integration.jei.recipe;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.category.GTRecipeCategory;
import com.gregtechceu.gtceu.integration.recipeviewer.widgets.GTMuiRecipeWidget;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IJeiHelpers;
import mezz.jei.api.recipe.RecipeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Concrete JEI category for GT recipes using MUI2-based recipe widgets.
 */
public class GTRecipeMuiJEICategory extends GTRecipeJEICategory<GTRecipe, GTMuiRecipeWidget> {

    private final GTRecipeCategory category;
    private final IDrawable icon;
    private final int width;
    private final int height;

    public GTRecipeMuiJEICategory(GTRecipeCategory category, IJeiHelpers helpers) {
        super(GTMuiRecipeWidget::new, recipe -> recipe.id);
        this.category = category;

        // Build icon from recipe type's icon supplier
        GTRecipeType recipeType = category.getRecipeType();
        ItemStack iconStack = recipeType.getIconSupplier() != null ?
                recipeType.getIconSupplier().get() : Items.BARRIER.getDefaultInstance();
        this.icon = helpers.getGuiHelper().createDrawableItemStack(iconStack);

        // Calculate category dimensions from recipe type max IO
        int maxItemInputs = recipeType.getMaxInputs(ItemRecipeCapability.CAP);
        int maxFluidInputs = recipeType.getMaxInputs(FluidRecipeCapability.CAP);
        int maxItemOutputs = recipeType.getMaxOutputs(ItemRecipeCapability.CAP);
        int maxFluidOutputs = recipeType.getMaxOutputs(FluidRecipeCapability.CAP);

        int maxInputCols = Math.min(Math.max(maxItemInputs, maxFluidInputs), 3);
        int maxOutputCols = Math.min(Math.max(maxItemOutputs, maxFluidOutputs), 3);
        int slotWidth = (maxInputCols + maxOutputCols) * 18 + 28; // 20 progress + 8 padding

        int inputItemRows = (int) Math.ceil((float) maxItemInputs / 3);
        int inputFluidRows = (int) Math.ceil((float) maxFluidInputs / 3);
        int outputItemRows = (int) Math.ceil((float) maxItemOutputs / 3);
        int outputFluidRows = (int) Math.ceil((float) maxFluidOutputs / 3);
        int maxRows = Math.max(Math.max(inputItemRows + inputFluidRows, outputItemRows + outputFluidRows), 1);
        int slotHeight = maxRows * 18;

        // Add text area for recipe info (EU/t, duration, conditions, data info)
        int textLines = 3 + recipeType.getMinRecipeConditions() + recipeType.getDataInfos().size();
        int textHeight = textLines * GTMuiRecipeWidget.LINE_HEIGHT;

        this.width = Math.max(slotWidth, 150);
        this.height = slotHeight + textHeight + 5;
    }

    @Override
    @NotNull
    public RecipeType<GTRecipe> getRecipeType() {
        return TYPES.apply(category);
    }

    @Override
    @NotNull
    public Component getTitle() {
        return Component.translatable(category.getLanguageKey());
    }

    @Override
    @Nullable
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
