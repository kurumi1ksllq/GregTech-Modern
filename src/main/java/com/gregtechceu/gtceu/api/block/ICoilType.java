package com.gregtechceu.gtceu.api.block;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.util.Lazy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Comparator;


/**
 * ICoilType is an interface that provides methods to get the properties of Heating Coils.
 * This is useful for Heating Coils that have different properties based on the type of coil.
 * For example, a Heating Coil that provides different temperatures based on the level of the coil.
 * @see Material
 */
public interface ICoilType {
    /**
     * TODO: Rename to getRegistryName() or getUniqueName()
     * This is used for the registry name of the Heating Coil
     * @implNote This should be unique for each Heating Coil
     * @return the name of the Heating Coil
     */
    @NotNull
    String getName();

    /**
     * TODO: Rename to getTemperature()
     * This is used for the temperature of the Heating Coil
     * @return the temperature of the Heating Coil
     */
    int getCoilTemperature();

    /**
     * TODO: Rename to getParallelizationTier()
     * This is used for the parallelization tier of the Heating Coil
     * This is used for the amount of parallel recipes in the multi smelter
     * @return the parallelization tier of the Heating Coil
     */
    int getLevel();

    /**
     * This is used for the energy discount when used in the multi smelter
     * @return the energy discount of the Heating Coil
     */
    int getEnergyDiscount();

    /**
     * TODO: Rename to getEnergyDiscountTier()
     * This is used for the energy discount tier when used in the cracking unit and pyrolyse oven
     * @return the energy discount tier of the Heating Coil
     */
    int getTier();

    /**
     * This is used for the material of the Heating Coil
     * @implNote This can be {@code null} if the Heating Coil does not have a material
     * @return the {@link Material} of the Heating Coil if it has one, otherwise {@code null}
     */
    @Nullable
    Material getMaterial();

    /**
     * This is used for the texture of the Heating Coil
     * @return the {@link ResourceLocation} defining the base texture of the coil
     */
    ResourceLocation getTexture();

    /**
     * This is used to make a list of all Heating Coils sorted by temperature
     * @implNote This is a lazy-loaded list of Heating Coils sorted by temperature
     */
    Lazy<ICoilType[]> ALL_COILS_TEMPERATURE_SORTED = Lazy.of(() -> GTCEuAPI.HEATING_COILS.keySet().stream()
            .sorted(Comparator.comparing(ICoilType::getCoilTemperature))
            .toArray(ICoilType[]::new));

    /**
     * This is used to get the Heating Coil with the minimum-required temperature
     * @param requiredTemperature the minimum-required temperature
     * @return the Heating Coil with the minimum-required temperature, otherwise {@code null}
     */
    @Nullable
    static ICoilType getMinRequiredType(int requiredTemperature) {
        return Arrays.stream(ALL_COILS_TEMPERATURE_SORTED.get())
                .filter(coil -> coil.getCoilTemperature() >= requiredTemperature)
                .findFirst().orElse(null);
    }
}
