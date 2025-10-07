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
    public IntProviderLinkedIngredient copy(){
        return new IntProviderLinkedIngredient(IntProviderIngredient.of(inner, countProvider, mark), mode, symlinks);
    }

    @Override
    public int getSampledCount(@NotNull RandomSource random) {
        if (!isRolled() && !links.isEmpty()) {
            double rollValue = 0;
            for (IRangedIngredient link : links) {
                rollValue += link.getSampledCountRatio();
            }
            double rollMultiplier = mode.getLinkMultiplier(rollValue, links.size());
            if (rollMultiplier > 0) {
                setSampledCount(getLinkedCount(rollMultiplier));
            }
        }
        return super.getSampledCount(random);
    }

    @Override
    public ItemStack[] getItems(){
        throw new IllegalCallerException("A linked ingredient cannot be rolled outside a recipe");
    }

    // TODO:
    // mark linked ingredients
    // pull marked ingredients from recipe to get rolls
    public ItemStack[] getItems(GTRecipe recipe, IO io) {
        if (itemStacks == null) {
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
        return super.getItems();
    }

    @Override
    public void reroll(){
        sampledCount = -1;
        itemStacks = null;
        links.clear();
    }
}
