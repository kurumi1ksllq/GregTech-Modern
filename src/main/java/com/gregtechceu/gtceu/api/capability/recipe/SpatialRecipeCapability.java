package com.gregtechceu.gtceu.api.capability.recipe;

import com.gregtechceu.gtceu.api.recipe.content.IContentSerializer;
import com.gregtechceu.gtceu.api.recipe.ingredient.SpatialIngredient;

public class SpatialRecipeCapability extends RecipeCapability<SpatialIngredient> {

    public static final SpatialRecipeCapability CAP = new SpatialRecipeCapability();

    public SpatialRecipeCapability() {
        this("spatial", 0xFF00FF, false, 3, SpatialIngredient.Serializer.INSTANCE);
    }

    protected SpatialRecipeCapability(String name, int color, boolean doRenderSlot, int sortIndex,
                                      IContentSerializer<SpatialIngredient> serializer) {
        super(name, color, doRenderSlot, sortIndex, serializer);
    }
}
