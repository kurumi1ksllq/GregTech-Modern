package com.gregtechceu.gtceu.api.block;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * IFusionCasingType is an interface that provides methods to get the properties of Fusion Casings.
 * This is useful for Fusion Casings that have different properties based on the type of casing.
 * For example, a Fusion Casing that provides different textures based on the level of the casing.
 */
public interface IFusionCasingType extends StringRepresentable {
    /**
     * Get the texture of the fusing casing.
     * @return the {@link ResourceLocation} defining the base texture of the casing
     */
    ResourceLocation getTexture();

    /**
     * Get the harvest level of the casing.
     * @return the Harvest level of this casing as an integer
     * @see net.minecraft.world.item.Tier
     */
    int getHarvestLevel();
}
