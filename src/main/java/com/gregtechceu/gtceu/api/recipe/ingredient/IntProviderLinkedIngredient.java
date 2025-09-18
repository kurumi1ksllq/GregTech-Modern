package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.api.GTValues;

import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.util.valueproviders.IntProvider;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

public class IntProviderLinkedIngredient implements IRangedIngredient {

    @Getter
    private IRangedIngredient inner;

    @Getter
    private List<IRangedIngredient> links;

    public enum LinkMode implements StringRepresentable {
        LINK_DIRECT("direct"),
        LINK_INVERSE("inverse"),
        LINK_XOR("xor"),
        LINK_NONE("none");

        @Getter
        private final String serializedName;

        LinkMode(String name){
            this.serializedName = name;
        }

        public static LinkMode getModeFromName(String name){
            return switch (name.strip().toLowerCase()) {
                case "direct" -> LINK_DIRECT;
                case "inverse" -> LINK_INVERSE;
                case "xor" -> LINK_XOR;
                default -> LINK_NONE;
            };
        }
    }

    @Getter
    private LinkMode mode;

    private IntProviderLinkedIngredient(IRangedIngredient inner, LinkMode mode, List<IRangedIngredient> links) {
        this.inner = inner;
        this.links = links;
        this.mode = mode;
    }

    public IntProviderLinkedIngredient of(IRangedIngredient inner, String mode, IRangedIngredient... links) {
        return new IntProviderLinkedIngredient(inner, LinkMode.getModeFromName(mode), Arrays.stream(links).toList());
    }

    public IntProviderLinkedIngredient of(IRangedIngredient inner, LinkMode mode, IRangedIngredient... links) {
        return new IntProviderLinkedIngredient(inner, mode, Arrays.stream(links).toList());
    }

    @Override
    public IntProvider getCountProvider() {
        return inner.getCountProvider();
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
                    inner.setSampledCount(getLinkedCount(rollValue / links.size()));
                case LINK_INVERSE:
                    inner.setSampledCount(getLinkedCount((1.0 - rollValue) / links.size()));
                case LINK_XOR:
                    inner.setSampledCount(getLinkedCount(rollValue));
            }
        }

        return inner.getSampledCount();
    }

    private int getLinkedCount(double roll) {
        int min = getCountProvider().getMinValue();
        int max = getCountProvider().getMaxValue();

        return (int) Math.round((max - min) * roll) + min;
    }

    @Override
    public void setSampledCount(int count) {
        inner.setSampledCount(count);
    }

    @Override
    public boolean isRolled() {
        return inner.isRolled();
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }
}
