package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.util.RandomSource;
import net.minecraftforge.fluids.FluidStack;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IntProviderLinkedFluidIngredient extends IntProviderFluidIngredient implements IRangedIngredient {

    @Getter
    private List<String> symlinks;

    private List<IRangedIngredient> links = new ArrayList<>();

    @Getter
    private LinkedIngredientLinkMode mode;

    private IntProviderLinkedFluidIngredient(IntProviderFluidIngredient inner, LinkedIngredientLinkMode mode,
                                             List<String> links) {
        super(inner.getInner(), inner.getCountProvider());
        this.symlinks = links;
        this.mode = mode;
    }

    public static IntProviderLinkedFluidIngredient of(IntProviderFluidIngredient inner, String mode,
                                                      String... links) {
        return new IntProviderLinkedFluidIngredient(inner, LinkedIngredientLinkMode.getModeFromName(mode),
                Arrays.stream(links).toList());
    }

    public static IntProviderLinkedFluidIngredient of(IntProviderFluidIngredient inner, LinkedIngredientLinkMode mode,
                                                      String... links) {
        return new IntProviderLinkedFluidIngredient(inner, mode, Arrays.stream(links).toList());
    }

    public static IntProviderLinkedFluidIngredient of(IntProviderFluidIngredient inner, LinkedIngredientLinkMode mode,
                                                      List<String> links) {
        return new IntProviderLinkedFluidIngredient(inner, mode, links);
    }

    @Override
    public int getSampledCount(@NotNull RandomSource random) {
        if (!isRolled() && !links.isEmpty()) {
            double rollValue = 0;
            for (IRangedIngredient link : links) {
                rollValue += link.getSampledCountRatio();
            }
            double rollMultiplier = mode.getLinkMultiplier(rollValue, links.size());
            if (rollMultiplier != -1) {
                setSampledCount(getLinkedCount(rollMultiplier));
            }
        }
        return super.getSampledCount();
    }


    public FluidStack[] getStacks(GTRecipe recipe, IO io) {
        if (fluidStacks == null) {
            var fullcontents = new ArrayList<Content>();
            fullcontents.addAll(recipe.getInputContents(ItemRecipeCapability.CAP));
            fullcontents.addAll(recipe.getInputContents(FluidRecipeCapability.CAP));
            if (io == IO.OUT) {
                fullcontents.addAll(recipe.getOutputContents(ItemRecipeCapability.CAP));
                fullcontents.addAll(recipe.getOutputContents(FluidRecipeCapability.CAP));
            }

            for (Content c : fullcontents) {
                if (c.content instanceof IRangedIngredient ranged && ranged.hasMark() &&
                        symlinks.contains(ranged.getMark())) {
                    links.add(ranged);
                }
            }
            if (links.isEmpty() && ConfigHolder.INSTANCE.dev.debug) {
                GTCEu.LOGGER.warn("Recipe with Linked Ingredients contained no valid links! Recipe:" + recipe.getId());
            }

            getSampledCount();
        }
        return super.getStacks();
    }
}
