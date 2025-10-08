package com.gregtechceu.gtceu.api.recipe.ingredient;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IntProviderLinkedFluidIngredient extends IntProviderFluidIngredient implements ILinkedIngredient {

    @Getter
    private List<String> symLinks;

    @Getter
    private List<IRangedIngredient> links = new ArrayList<>();

    @Getter
    private LinkedIngredientLinkMode mode;

    private IntProviderLinkedFluidIngredient(IntProviderFluidIngredient inner, LinkedIngredientLinkMode mode,
                                             List<String> links) {
        super(inner.inner, inner.countProvider, inner.sampledCount, inner.mark);
        this.symLinks = links;
        this.mode = mode;
    }

    public static IntProviderLinkedFluidIngredient of(IntProviderFluidIngredient inner, String mode,
                                                      String... links) {
        return new IntProviderLinkedFluidIngredient(inner, LinkedIngredientLinkMode.getModeFromName(mode),
                Arrays.stream(links).toList());
    }

    public static IntProviderLinkedFluidIngredient of(IntProviderFluidIngredient inner, LinkedIngredientLinkMode mode,
                                                      String... links) {
        return new IntProviderLinkedFluidIngredient(inner, mode, Arrays.stream(links).toList());
    }

    public static IntProviderLinkedFluidIngredient of(IntProviderFluidIngredient inner, LinkedIngredientLinkMode mode,
                                                      List<String> links) {
        return new IntProviderLinkedFluidIngredient(inner, mode, links);
    }

    @Override
    public IntProviderLinkedFluidIngredient copy() {
        return new IntProviderLinkedFluidIngredient(super.copy(), mode, symLinks);
    }

    @Override
    public void reset() {
        sampledCount = -1;
        fluidStacks = null;
        links.clear();
    }
}
