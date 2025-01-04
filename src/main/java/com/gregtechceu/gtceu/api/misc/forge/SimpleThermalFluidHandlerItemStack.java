package com.gregtechceu.gtceu.api.misc.forge;

import com.gregtechceu.gtceu.api.capability.IThermalFluidHandlerItemStack;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStackSimple;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * @author KilaBash
 * @date 2023/2/22
 * @implNote FluidHandlerHelperImpl
 */
public class SimpleThermalFluidHandlerItemStack extends FluidHandlerItemStackSimple
                                                implements IThermalFluidHandlerItemStack {

    @Getter
    public final int maxFluidTemperature;
    @Getter
    public final int minFluidTemperature;
    @Getter
    private final boolean gasProof;
    @Getter
    private final boolean plasmaProof;

    public SimpleThermalFluidHandlerItemStack(@NotNull ItemStack container, int capacity,
                                              int maxFluidTemperature, int minFluidTemperature,
                                              boolean gasProof, boolean plasmaProof) {
        super(container, capacity);
        this.maxFluidTemperature = maxFluidTemperature;
        this.minFluidTemperature = minFluidTemperature;
        this.gasProof = gasProof;
        this.plasmaProof = plasmaProof;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack drained = super.drain(resource, action);
        this.removeTagWhenEmpty(action);
        return drained;
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        FluidStack drained = super.drain(maxDrain, action);
        this.removeTagWhenEmpty(action);
        return drained;
    }

    private void removeTagWhenEmpty(FluidAction action) {
        if (getFluid() == FluidStack.EMPTY && action.execute()) {
            this.container.setTag(null);
        }
    }
}
