package com.gregtechceu.gtceu.api.misc.forge;

import com.gregtechceu.gtceu.api.capability.IThermalFluidHandlerItemStack;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.capability.templates.FluidHandlerItemStackSimple;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class SimpleThermalFluidHandlerItemStack extends FluidHandlerItemStackSimple
                                                implements IThermalFluidHandlerItemStack {

    @Getter
    private final int maxFluidTemperature;
    @Getter
    private final int minFluidTemperature;
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
}
