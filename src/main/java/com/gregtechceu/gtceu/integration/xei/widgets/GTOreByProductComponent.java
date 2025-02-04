package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.transfer.fluid.CustomFluidTank;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.integration.xei.entry.fluid.FluidEntryList;
import com.gregtechceu.gtceu.integration.xei.entry.item.ItemEntryList;
import com.gregtechceu.gtceu.integration.xei.handlers.fluid.CycleFluidEntryHandler;
import com.gregtechceu.gtceu.integration.xei.handlers.item.CycleItemEntryHandler;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.ArrayList;
import java.util.List;

public class GTOreByProductComponent extends StackLayout {

    // XY positions of every item and fluid, in three enormous lists
    protected final static ImmutableList<Integer> ITEM_INPUT_LOCATIONS = ImmutableList.of(
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

    protected final static ImmutableList<Integer> ITEM_OUTPUT_LOCATIONS = ImmutableList.of(
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

    protected final static ImmutableList<Integer> FLUID_LOCATIONS = ImmutableList.of(
            42, 25, // washer in
            42, 48  // chem bath in
    );

    // Used to set intermediates as both input and output
    protected final static ImmutableSet<Integer> FINAL_OUTPUT_INDICES = ImmutableSet.of(
            0, 4, 8, 10, 12, 16, 20, 22, 24, 28, 30, 32, 40, 44, 48, 50, 52, 54, 56, 58, 60, 62, 64, 66);

    public GTOreByProductComponent(Material material) {
        super(Sizing.fixed(176), Sizing.fixed(166));
        setRecipe(new GTOreByProduct(material));
    }

    public void setRecipe(GTOreByProduct recipeWrapper) {
        List<Boolean> itemOutputExists = new ArrayList<>();

        // only draw slot on inputs if it is the ore
        child(UIComponents.texture(GuiTextures.SLOT)
                .positioning(Positioning.absolute(ITEM_INPUT_LOCATIONS.get(0), ITEM_INPUT_LOCATIONS.get(1)))
                .sizing(Sizing.fixed(18)));
        boolean hasSifter = recipeWrapper.hasSifter();

        child(UIComponents.texture(GuiTextures.OREBY_BASE)
                .positioning(Positioning.absolute(0, 0))
                .sizing(Sizing.fixed(176), Sizing.fixed(166)));
        if (recipeWrapper.hasDirectSmelt()) {
            child(UIComponents.texture(GuiTextures.OREBY_SMELT)
                    .positioning(Positioning.absolute(0, 0))
                    .sizing(Sizing.fixed(176), Sizing.fixed(166)));
        }
        if (recipeWrapper.hasChemBath()) {
            child(UIComponents.texture(GuiTextures.OREBY_CHEM)
                    .positioning(Positioning.absolute(0, 0))
                    .sizing(Sizing.fixed(176), Sizing.fixed(166)));
        }
        if (recipeWrapper.hasSeparator()) {
            child(UIComponents.texture(GuiTextures.OREBY_SEP)
                    .positioning(Positioning.absolute(0, 0))
                    .sizing(Sizing.fixed(176), Sizing.fixed(166)));
        }
        if (hasSifter) {
            child(UIComponents.texture(GuiTextures.OREBY_SIFT)
                    .positioning(Positioning.absolute(0, 0))
                    .sizing(Sizing.fixed(176), Sizing.fixed(166)));
        }

        List<ItemEntryList> itemInputs = recipeWrapper.itemInputs;
        CycleItemEntryHandler itemInputsHandler = new CycleItemEntryHandler(itemInputs);
        StackLayout itemStackGroup = UIContainers.stack(Sizing.fill(), Sizing.fill());
        for (int i = 0; i < ITEM_INPUT_LOCATIONS.size(); i += 2) {
            final int finalI = i;
            itemStackGroup.child(UIComponents.slot(itemInputsHandler, i / 2)
                    .canInsert(false)
                    .canExtract(false)
                    .ingredientIO(IO.IN)
                    .backgroundTexture(null)
                    .tooltip((slot, tooltips) -> recipeWrapper.getTooltip(finalI / 2, tooltips))
                    .positioning(Positioning.absolute(ITEM_INPUT_LOCATIONS.get(i), ITEM_INPUT_LOCATIONS.get(i + 1))));
        }

        NonNullList<ItemStack> itemOutputs = recipeWrapper.itemOutputs;
        CustomItemStackHandler itemOutputsHandler = new CustomItemStackHandler(itemOutputs);
        for (int i = 0; i < ITEM_OUTPUT_LOCATIONS.size(); i += 2) {
            int slotIndex = i / 2;
            float xeiChance = 1.0f;
            Content chance = recipeWrapper.getChance(i / 2 + itemInputs.size());
            UITexture overlay = null;
            if (chance != null) {
                xeiChance = (float) chance.chance / chance.maxChance;
                overlay = chance.createOverlay(false, 0, 0, null);
            }
            if (itemOutputs.get(slotIndex).isEmpty()) {
                itemOutputExists.add(false);
                continue;
            }

            itemStackGroup.child(UIComponents.slot(itemOutputsHandler, slotIndex)
                    .canInsert(false)
                    .canExtract(false)
                    .ingredientIO(FINAL_OUTPUT_INDICES.contains(i) ? IngredientIO.OUTPUT : IngredientIO.BOTH)
                    .recipeViewerChance(xeiChance)
                    .backgroundTexture(null)
                    .overlayTexture(overlay)
                    .tooltip(
                            (slot, tooltips) -> recipeWrapper.getTooltip(slotIndex + itemInputs.size(), tooltips))
                    .positioning(Positioning.absolute(ITEM_OUTPUT_LOCATIONS.get(i), ITEM_OUTPUT_LOCATIONS.get(i + 1))));
            itemOutputExists.add(true);
        }

        List<FluidEntryList> fluidInputs = recipeWrapper.fluidInputs;
        CycleFluidEntryHandler fluidInputsHandler = new CycleFluidEntryHandler(fluidInputs);
        StackLayout fluidStackGroup = UIContainers.stack(Sizing.fill(), Sizing.fill());
        for (int i = 0; i < FLUID_LOCATIONS.size(); i += 2) {
            int slotIndex = i / 2;
            if (!fluidInputs.get(slotIndex).isEmpty()) {
                var tank = UIComponents.tank(new CustomFluidTank(fluidInputsHandler.getFluidInTank(slotIndex)))
                        .canInsert(false)
                        .canExtract(false)
                        .ingredientIO(IO.IN)
                        .backgroundTexture(GuiTextures.FLUID_SLOT)
                        .showAmount(false)
                        .positioning(Positioning.absolute(FLUID_LOCATIONS.get(i), FLUID_LOCATIONS.get(i + 1)));
                fluidStackGroup.child(tank);
            }
        }

        this.child(itemStackGroup);
        this.child(fluidStackGroup);

        for (int i = 0; i < ITEM_OUTPUT_LOCATIONS.size(); i += 2) {
            // stupid hack to show all sifter slots if the first one exists
            if (itemOutputExists.get(i / 2) || (i > 28 * 2 && itemOutputExists.get(28) && hasSifter)) {
                child(this.children().size() - 3, UIComponents.texture(GuiTextures.SLOT)
                        .positioning(
                                Positioning.absolute(ITEM_OUTPUT_LOCATIONS.get(i), ITEM_OUTPUT_LOCATIONS.get(i + 1)))
                        .sizing(Sizing.fixed(18)));
            }
        }
    }
}
