package com.gregtechceu.gtceu.api.capability;

import com.gregtechceu.gtceu.api.fluids.FluidConstants;
import com.gregtechceu.gtceu.api.fluids.FluidState;
import com.gregtechceu.gtceu.api.fluids.attribute.FluidAttribute;
import com.gregtechceu.gtceu.api.fluids.attribute.FluidAttributes;
import com.gregtechceu.gtceu.api.fluids.attribute.IAttributedFluid;

import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

import java.util.Collection;

/**
 * Interface for FluidHandlerItemStacks which handle GT's unique fluid mechanics
 * 
 * @see FluidAttribute
 * @see FluidAttributes
 * @see IAttributedFluid
 */
public interface IThermalFluidHandlerItemStack {

    /**
     *
     * @param stack the {@link FluidStack} to check
     * @return whether the FluidStack can be used to fill this fluid container
     */
    default boolean canFillFluidType(FluidStack stack) {
        if (stack == null || stack.getFluid() == null) return false;

        FluidType fluidType = stack.getFluid().getFluidType();
        var temp = fluidType.getTemperature();
        if (temp > getMaxFluidTemperature()) return false;
        // fluids less than 120K are cryogenic
        if (temp < getMinFluidTemperature()) return false;
        if (fluidType.isLighterThanAir() && !isGasProof()) return false;
        return true;
    }

    /**
     * This is always checked, regardless of the contained fluid being a {@link IAttributedFluid} or not
     *
     * @return the maximum allowed temperature for a fluid to be stored in this container
     */
    int getMaxFluidTemperature();

    /**
     * This is always checked, regardless of the contained fluid being a {@link IAttributedFluid} or not
     *
     * @return the minimum allowed temperature for a fluid to be stored in this container
     */
    int getMinFluidTemperature();

    /**
     * This is always checked, regardless of the contained fluid being a {@link IAttributedFluid} or not
     *
     * @return true if this fluid container allows gases, otherwise false
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isGasProof();

    /**
     * @see FluidAttributes
     *
     * @return true if this fluid container allows plasmas, otherwise false
     */
    boolean isPlasmaProof();
}
