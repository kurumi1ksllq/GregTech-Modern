package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An {@link IntProviderIngredient whose rolled value is calculated based on the rolls of one or more other
 * {@link IRangedIngredient}s.}
 */
public class IntProviderLinkedIngredient extends IntProviderIngredient implements IRangedIngredient {

    @Getter
    private List<String> symlinks;

    private List<IRangedIngredient> links = new ArrayList<>();

    @Getter
    private LinkedIngredientLinkMode mode;

    private IntProviderLinkedIngredient(IntProviderIngredient inner, LinkedIngredientLinkMode mode,
                                        List<String> symlinks) {
        super(inner.inner, inner.countProvider);
        this.symlinks = symlinks;
        this.mode = mode;
    }

    public static IntProviderLinkedIngredient of(IntProviderIngredient inner, String mode, String... links) {
        return new IntProviderLinkedIngredient(inner, LinkedIngredientLinkMode.getModeFromName(mode),
                Arrays.stream(links).toList());
    }

    public static IntProviderLinkedIngredient of(IntProviderIngredient inner, LinkedIngredientLinkMode mode,
                                                 String... links) {
        return new IntProviderLinkedIngredient(inner, mode, Arrays.stream(links).toList());
    }

    public static IntProviderLinkedIngredient of(IntProviderIngredient inner, LinkedIngredientLinkMode mode,
                                                 List<String> links) {
        return new IntProviderLinkedIngredient(inner, mode, links);
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

    // TODO:
    // mark linked ingredients
    // pull marked ingredients from recipe to get rolls
    public ItemStack[] getItems(GTRecipe recipe, IO io) {
        if (itemStacks == null) {
            var fullcontents = recipe.getInputContents(ItemRecipeCapability.CAP);
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
        return super.getItems();
    }
}
