package com.gregtechceu.gtceu.api.recipe.content;

import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;

import net.minecraft.util.ExtraCodecs;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jetbrains.annotations.NotNull;

public record Content(Object content, int chance, int maxChance, int tierChanceBoost) {

    public Content(Object content, int chance, int maxChance, int tierChanceBoost) {
        this.content = content;
        this.chance = chance;
        this.maxChance = maxChance;
        this.tierChanceBoost = fixBoost(tierChanceBoost);
    }

    public static <T> Codec<Content> codec(RecipeCapability<T> capability) {
        return RecordCodecBuilder.create(instance -> instance.group(
                        capability.serializer.codec().fieldOf("content").forGetter(val -> capability.of(val.content)),
                        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("chance", ChanceLogic.getMaxChancedValue())
                                .forGetter(val -> val.chance),
                        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("maxChance", ChanceLogic.getMaxChancedValue())
                                .forGetter(val -> val.maxChance),
                        Codec.INT.optionalFieldOf("tierChanceBoost", 0)
                                .forGetter(val -> val.tierChanceBoost))
                .apply(instance, Content::new));
    }

    /**
     * Directly copies a Content.
     */
    public Content copy(RecipeCapability<?> capability) {
        return new Content(capability.copyContent(content), chance, maxChance, tierChanceBoost);
    }

    /**
     * Applies a {@link ContentModifier} to a Content. Does not apply the Modifier if the Content has a Chance.
     */
    public Content copy(RecipeCapability<?> capability, @NotNull ContentModifier modifier) {
        if (modifier == ContentModifier.IDENTITY || chance < maxChance) {
            return copy(capability);
        } else {
            return new Content(capability.copyContent(content, modifier), chance, maxChance, tierChanceBoost);
        }
    }

    /**
     * Applies a {@link ContentModifier} to a Content. Even if the content has a Chance.
     */
    public Content copyChanced(RecipeCapability<?> capability, @NotNull ContentModifier modifier) {
        if (modifier == ContentModifier.IDENTITY) {
            return copy(capability);
        } else {
            return new Content(capability.copyContent(content, modifier), chance, maxChance, tierChanceBoost);
        }
    }

    public boolean isChanced() {
        return chance > 0 && chance < maxChance;
    }

    /**
     * Attempts to fix and round the given chance boost due to potential differences
     * between the max chance and {@link ChanceLogic#getMaxChancedValue()}.
     * <br />
     * The worst case would be {@code 5,001 / 10,000} , meaning the boost would
     * have to be halved to have the intended effect.
     *
     * @param chanceBoost the chance boost to be fixed
     * @return the fixed chance boost
     */
    private int fixBoost(int chanceBoost) {
        float error = (float) ChanceLogic.getMaxChancedValue() / maxChance;
        int fixed = Math.round(Math.abs(chanceBoost) / error);
        return chanceBoost < 0 ? -fixed : fixed;
    }
}
