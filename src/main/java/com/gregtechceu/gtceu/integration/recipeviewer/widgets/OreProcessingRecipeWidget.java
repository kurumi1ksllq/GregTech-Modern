package com.gregtechceu.gtceu.integration.recipeviewer.widgets;

import brachy.modularui.drawable.GuiTextures;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.gui.ContentOverlay;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.integration.recipeviewer.RecipeSlotRole;
import brachy.modularui.integration.recipeviewer.entry.fluid.FluidEntryList;
import brachy.modularui.integration.recipeviewer.entry.item.ItemEntryList;
import brachy.modularui.integration.recipeviewer.handlers.fluid.CycleFluidEntryHandler;
import brachy.modularui.integration.recipeviewer.handlers.item.CycleItemEntryHandler;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.FluidDisplayWidget;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import it.unimi.dsi.fastutil.booleans.BooleanArrayList;
import it.unimi.dsi.fastutil.booleans.BooleanList;
import it.unimi.dsi.fastutil.ints.IntImmutableList;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.List;

public class OreProcessingRecipeWidget extends ParentWidget<OreProcessingRecipeWidget> {

    // XY positions of every item and fluid, in three enormous lists
    protected final static IntImmutableList ITEM_INPUT_LOCATIONS = IntImmutableList.of(
            3, 3,       // ore
            23, 3,      // furnace (direct smelt)
            3, 24,      // macerator (ore -> crushed)
            23, 71,     // macerator (crushed -> impure)
            50, 80,     // centrifuge (impure -> dust)
            24, 25,     // ore washer
            97, 71,     // thermal centrifuge
            70, 80,     // macerator (centrifuged -> dust)
            114, 48,    // macerator (crushed purified -> purified)
            133, 71,    // centrifuge (purified -> dust)
            3, 123,     // cauldron / simple washer (crushed)
            41, 145,    // cauldron (impure)
            102, 145,   // cauldron (purified)
            24, 48,     // chem bath
            155, 71,    // electro separator
            101, 25     // sifter
    );

    protected final static IntImmutableList ITEM_OUTPUT_LOCATIONS = IntImmutableList.of(
            46, 3,      // smelt result: 0
            3, 47,      // ore -> crushed: 2
            3, 65,      // byproduct: 4
            23, 92,     // crushed -> impure: 6
            23, 110,    // byproduct: 8
            50, 101,    // impure -> dust: 10
            50, 119,    // byproduct: 12
            64, 25,     // crushed -> crushed purified (wash): 14
            82, 25,     // byproduct: 16
            97, 92,     // crushed/crushed purified -> centrifuged: 18
            97, 110,    // byproduct: 20
            70, 101,    // centrifuged -> dust: 22
            70, 119,    // byproduct: 24
            137, 47,    // crushed purified -> purified: 26
            155, 47,    // byproduct: 28
            133, 92,    // purified -> dust: 30
            133, 110,   // byproduct: 32
            3, 105,     // crushed cauldron: 34
            3, 145,     // -> purified crushed: 36
            23, 145,    // impure cauldron: 38
            63, 145,    // -> dust: 40
            84, 145,    // purified cauldron: 42
            124, 145,   // -> dust: 44
            64, 48,     // crushed -> crushed purified (chem bath): 46
            82, 48,     // byproduct: 48
            155, 92,    // purified -> dust (electro separator): 50
            155, 110,   // byproduct 1: 52
            155, 128,   // byproduct 2: 54
            119, 3,     // sifter outputs... : 56
            137, 3,     // 58
            155, 3,     // 60
            119, 21,    // 62
            137, 21,    // 64
            155, 21     // 66
    );

    protected final static IntImmutableList FLUID_LOCATIONS = IntImmutableList.of(
            42, 25, // washer in
            42, 48  // chem bath in
    );

    // Used to set intermediates as both input and output
    protected final static IntSet FINAL_OUTPUT_INDICES = IntSet.of(
            0, 4, 8, 10, 12, 16, 20, 22, 24, 28, 30, 32, 40, 44, 48, 50, 52, 54, 56, 58, 60, 62, 64, 66);

    public OreProcessingRecipeWidget(Material material) {
        size(176, 166);
        setRecipe(new GTOreByProduct(material));
    }

    public void setRecipe(GTOreByProduct recipeWrapper) {
        BooleanList itemOutputExists = new BooleanArrayList();

        // only draw slot on inputs if it is the ore
        child(GuiTextures.SLOT_ITEM.asWidget()
                .pos(ITEM_INPUT_LOCATIONS.getInt(0), ITEM_INPUT_LOCATIONS.getInt(1)));
        boolean hasSifter = recipeWrapper.hasSifter();

        child(GTGuiTextures.OREBY_BASE.asWidget());
        if (recipeWrapper.hasDirectSmelt()) {
            child(GTGuiTextures.OREBY_SMELT.asWidget());
        }
        if (recipeWrapper.hasChemBath()) {
            child(GTGuiTextures.OREBY_CHEM.asWidget());
        }
        if (recipeWrapper.hasSeparator()) {
            child(GTGuiTextures.OREBY_SEP.asWidget());
        }
        if (hasSifter) {
            child(GTGuiTextures.OREBY_SIFT.asWidget());
        }

        List<ItemEntryList> itemInputs = recipeWrapper.itemInputs;
        CycleItemEntryHandler itemInputsHandler = new CycleItemEntryHandler(itemInputs);
        ParentWidget<?> itemStackGroup = new ParentWidget<>().sizeRel(1f);
        for (int i = 0; i < ITEM_INPUT_LOCATIONS.size(); i += 2) {
            itemStackGroup.child(new ItemSlot().slot(new ModularSlot(itemInputsHandler, i / 2)
                    .accessibility(false, false))
                    .recipeRole(RecipeSlotRole.INPUT)
                    .pos(ITEM_INPUT_LOCATIONS.getInt(i), ITEM_INPUT_LOCATIONS.getInt(i + 1))
                    .tooltipBuilder(recipeWrapper.getTooltip(i / 2)));
        }

        NonNullList<ItemStack> itemOutputs = recipeWrapper.itemOutputs;
        CustomItemStackHandler itemOutputsHandler = new CustomItemStackHandler(itemOutputs);
        for (int i = 0; i < ITEM_OUTPUT_LOCATIONS.size(); i += 2) {
            int slotIndex = i / 2;
            float xeiChance = 1.0f;
            Content chance = recipeWrapper.getChance(i / 2 + itemInputs.size());
            IDrawable overlay = null;
            if (chance != null) {
                xeiChance = (float) chance.chance / chance.maxChance;
                overlay = new ContentOverlay(chance, false, 0, 0, null);
            }
            if (itemOutputs.get(slotIndex).isEmpty()) {
                itemOutputExists.add(false);
                continue;
            }

            itemStackGroup.child(new ItemSlot()
                    .slot(new ModularSlot(itemOutputsHandler, slotIndex).accessibility(false, false))
                    .pos(ITEM_OUTPUT_LOCATIONS.getInt(i), ITEM_OUTPUT_LOCATIONS.getInt(i + 1))
                    .recipeRole(RecipeSlotRole.OUTPUT)
                    .tooltip(recipeWrapper.getTooltip(slotIndex + itemInputs.size()))
                    .overlay(overlay));
            itemOutputExists.add(true);
        }

        List<FluidEntryList> fluidInputs = recipeWrapper.fluidInputs;
        CycleFluidEntryHandler fluidInputsHandler = new CycleFluidEntryHandler(fluidInputs);
        ParentWidget<?> fluidStackGroup = new ParentWidget<>().sizeRel(1f);
        for (int i = 0; i < FLUID_LOCATIONS.size(); i += 2) {
            int slotIndex = i / 2;
            if (!fluidInputs.get(slotIndex).isEmpty()) {
                fluidStackGroup.child(new FluidDisplayWidget().value(fluidInputsHandler.getFluidInTank(slotIndex))
                        .recipeSlotRole(RecipeSlotRole.INPUT)
                        .pos(FLUID_LOCATIONS.getInt(i), FLUID_LOCATIONS.getInt(i + 1)));
            }
        }

        child(itemStackGroup);
        child(fluidStackGroup);

        for (int i = 0; i < ITEM_OUTPUT_LOCATIONS.size(); i += 2) {
            // stupid hack to show all sifter slots if the first one exists
            if (itemOutputExists.getBoolean(i / 2) || (i > 28 * 2 && itemOutputExists.getBoolean(28) && hasSifter)) {
                child(getChildren().size() - 3, brachy.modularui.drawable.GuiTextures.SLOT_ITEM.asWidget()
                        .pos(ITEM_INPUT_LOCATIONS.getInt(i), ITEM_INPUT_LOCATIONS.getInt(i + 1)));
            }
        }
    }
}
