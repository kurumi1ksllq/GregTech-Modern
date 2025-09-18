package com.gregtechceu.gtceu.api.recipe.ingredient;

import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;

import org.jetbrains.annotations.NotNull;

public interface IRangedIngredient {

    IntProvider getCountProvider();

    int getSampledCount();

    int getSampledCount(@NotNull RandomSource random);

    void setSampledCount(int count);

    /**
     * @return the average roll of this ranged amount
     */
    default double getMidRoll() {
        return ((getCountProvider().getMaxValue() + getCountProvider().getMinValue()) / 2.0);
    }

    /**
     * @return a decimal from 0 to 1.0 of how high this ingredient rolled out of its maximum range. If this ingredient
     *         has not been rolled, returns 0.
     */
    default double getSampledCountRatio() {
        if (!isRolled()) return 0;
        else {
            int min = getCountProvider().getMinValue();
            int max = getCountProvider().getMaxValue();
            int count = getSampledCount();

            return (1.0 - ((double) (max - count) / (max - min)));
        }
    }

    boolean isRolled();

    boolean isEmpty();
}
