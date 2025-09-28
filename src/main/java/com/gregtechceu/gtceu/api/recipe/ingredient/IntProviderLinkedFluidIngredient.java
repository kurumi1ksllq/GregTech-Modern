package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.util.RandomSource;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class IntProviderLinkedFluidIngredient extends IntProviderFluidIngredient implements IRangedIngredient {

    @Getter
    private List<IRangedIngredient> links;

    @Getter
    private LinkedIngredientLinkMode mode;

    private IntProviderLinkedFluidIngredient(IntProviderFluidIngredient inner, LinkedIngredientLinkMode mode, List<IRangedIngredient> links) {
        super(inner);
        this.links = links;
        this.mode = mode;
    }

    public static IntProviderLinkedFluidIngredient of(IntProviderFluidIngredient inner, String mode, IRangedIngredient... links) {
        return new IntProviderLinkedFluidIngredient(inner, LinkedIngredientLinkMode.getModeFromName(mode), Arrays.stream(links).toList());
    }

    public static IntProviderLinkedFluidIngredient of(IntProviderFluidIngredient inner, LinkedIngredientLinkMode mode, IRangedIngredient... links) {
        return new IntProviderLinkedFluidIngredient(inner, mode, Arrays.stream(links).toList());
    }

    public static IntProviderLinkedFluidIngredient of(IntProviderFluidIngredient inner, LinkedIngredientLinkMode mode, List<IRangedIngredient> links) {
        return new IntProviderLinkedFluidIngredient(inner, mode, links);
    }

    @Override
    public int getSampledCount() {
        return getSampledCount(GTValues.RNG);
    }

    @Override
    public int getSampledCount(@NotNull RandomSource random) {
        if (!isRolled() && !links.isEmpty()) {
            double rollValue = 0;
            for (IRangedIngredient link : links) {
                rollValue += link.getSampledCountRatio();
            }

            switch (mode) {
                case LINK_DIRECT:
                    setSampledCount(getLinkedCount(rollValue / links.size()));
                case LINK_INVERSE:
                    setSampledCount(getLinkedCount(1.0 - (rollValue / links.size())));
                case LINK_XOR:
                    setSampledCount(getLinkedCount(rollValue));
                case LINK_XOR_INVERSE:
                    setSampledCount(getLinkedCount(1.0 - rollValue));
                default:
                    // pass
            }
        }
        return super.getSampledCount();
    }

    private int getLinkedCount(double roll) {
        int min = getCountProvider().getMinValue();
        int max = getCountProvider().getMaxValue();

        return (int) Math.round((max - min) * roll) + min;
    }

}
