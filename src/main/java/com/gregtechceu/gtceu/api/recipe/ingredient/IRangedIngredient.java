package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;

import org.jetbrains.annotations.NotNull;

/**
 * An Ingredient, Fluid Ingredient, or something that can be passed into a crafting recipe, with a count or amount which
 * is randomly rolled upon consumption or production.
 * Due to type requirements in {@link NotifiableRecipeHandlerTrait}, all IRangedIngredients must extend a basic
 * ingredient type, and contain an instance of that ingredient ({@code inner}).
 */
public interface IRangedIngredient {

    IntProvider getCountProvider();

    /**
     * Used to establish Links in {@link IntProviderLinkedIngredient}s
     */
    String getMark();

    default boolean hasMark() {
        return getMark() != null || getMark().isEmpty();
    }

    /**
     * If this ingredient has not yet had its count rolled, rolls it and returns the roll.
     * If it has, returns the existing roll.
     * Passthrough method, invokes {@code getSampledCount()} using the threadsafe {@link GTValues#RNG}.
     *
     * @return the amount rolled
     */
    default int getSampledCount() {
        return getSampledCount(GTValues.RNG);
    }

    int getSampledCount(@NotNull RandomSource random);

    void setSampledCount(int count);

    /**
     * @return the average roll of this ranged amount
     */
    default double getMidRoll() {
        return ((getCountProvider().getMaxValue() + getCountProvider().getMinValue()) / 2.0);
    }

    /**
     * @return a decimal from 0 to 1.0 of how high this ingredient rolled out of its maximum range.
     */
    default double getSampledCountRatio() {
        int min = getCountProvider().getMinValue();
        int max = getCountProvider().getMaxValue();
        int count = getSampledCount();

        return (1.0 - ((double) (max - count) / (max - min)));
    }

    default int getLinkedCount(double roll) {
        int min = getCountProvider().getMinValue();
        int max = getCountProvider().getMaxValue();

        return (int) Math.round((max - min) * roll) + min;
    }

    boolean isRolled();

    void reroll();

    boolean isEmpty();

    int hashCode();
}
