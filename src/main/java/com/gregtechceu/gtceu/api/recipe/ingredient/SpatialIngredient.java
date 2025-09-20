package com.gregtechceu.gtceu.api.recipe.ingredient;

import com.gregtechceu.gtceu.api.recipe.content.IContentSerializer;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import com.mojang.serialization.Codec;

import java.util.function.Predicate;

public class SpatialIngredient implements Predicate<StructureTemplate> {
    // TODO: write structure match check

    @Override
    public boolean test(StructureTemplate template) {
        return false;
    }

    public static class Serializer implements IContentSerializer<SpatialIngredient> {

        public static final Serializer INSTANCE = new Serializer();

        @Override
        public SpatialIngredient of(Object o) {
            if (o instanceof SpatialIngredient spatialIngredient) {
                return spatialIngredient;
            }
        }

        @Override
        public SpatialIngredient defaultValue() {
            return null;
        }

        @Override
        public Class<SpatialIngredient> contentClass() {
            return null;
        }

        @Override
        public Codec<SpatialIngredient> codec() {
            return null;
        }
    }
}
