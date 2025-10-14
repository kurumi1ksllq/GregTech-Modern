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

    default int getSampledCount(List<IRangedIngredient> marks) {
        return getSampledCount(GTValues.RNG, marks);
    }

    default int getSampledCount(@NotNull RandomSource random, List<IRangedIngredient> marks) {
        if (getLinks().isEmpty()) addLinks(marks);
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

    default void addLinks(List<IRangedIngredient> marks) {
        for (IRangedIngredient mark : marks) {
            if (getSymLinks().contains(mark.getMark())) {
                getLinks().add(mark);
            }
        }
    }

    default void roll(List<IRangedIngredient> marks) {
        getSampledCount(marks);
    }
}
