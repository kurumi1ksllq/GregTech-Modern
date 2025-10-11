package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.util.RandomSource;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface ILinkedIngredient extends IRangedIngredient {

    List<IRangedIngredient> getLinks();

    List<String> getSymLinks();

    LinkedIngredientLinkMode getMode();

    default int getSampledCount(GTRecipe recipe) {
        return getSampledCount(GTValues.RNG, recipe);
    }

    default int getSampledCount(@NotNull RandomSource random, GTRecipe recipe) {
        if (getLinks().isEmpty()) addLinks(recipe);
        if (!isRolled()) {
            double rollValue = 0;
            for (IRangedIngredient link : getLinks()) {
                rollValue += link.getSampledCountRatio();
            }
            double rollMultiplier = getMode().getLinkMultiplier(rollValue, getLinks().size());
            setSampledCount(getLinkedCount(rollMultiplier));
        }
        return getSampledCount(random);
    }

    default void addLinks(GTRecipe recipe) {
        var fullcontents = new ArrayList<Content>();
        fullcontents.addAll(recipe.getInputContents(ItemRecipeCapability.CAP));
        fullcontents.addAll(recipe.getInputContents(FluidRecipeCapability.CAP));
        fullcontents.addAll(recipe.getOutputContents(ItemRecipeCapability.CAP));
        fullcontents.addAll(recipe.getOutputContents(FluidRecipeCapability.CAP));
        for (Content c : fullcontents) {
            if (c.content instanceof IRangedIngredient ranged && ranged.hasMark() &&
                    getSymLinks().contains(ranged.getMark())) {
                getLinks().add(ranged);
            }
        }
        if (getLinks().isEmpty() && ConfigHolder.INSTANCE.dev.debug) {
            GTCEu.LOGGER.warn("Recipe with Linked Ingredients contained no valid links! Recipe:" + recipe.getId());
        }
    }

    default void roll(GTRecipe recipe) {
        getSampledCount(recipe);
    }
}
