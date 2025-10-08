package com.gregtechceu.gtceu.api.recipe.ingredient;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * An {@link IntProviderIngredient whose rolled value is calculated based on the rolls of one or more other
 * {@link IRangedIngredient}s.}
 */
public class IntProviderLinkedIngredient extends IntProviderIngredient implements ILinkedIngredient {

    @Getter
    private List<String> symLinks;

    @Getter
    private List<IRangedIngredient> links = new ArrayList<>();

    @Getter
    private LinkedIngredientLinkMode mode;

    private IntProviderLinkedIngredient(IntProviderIngredient inner, LinkedIngredientLinkMode mode,
                                        List<String> symlinks) {
        super(inner.inner, inner.countProvider, inner.sampledCount, inner.mark);
        this.symLinks = symlinks;
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
    public IntProviderLinkedIngredient copy() {
        return new IntProviderLinkedIngredient(super.copy(), mode, symLinks);
    }

    @Override
    public void reset() {
        sampledCount = -1;
        itemStacks = null;
        links.clear();
    }
}
