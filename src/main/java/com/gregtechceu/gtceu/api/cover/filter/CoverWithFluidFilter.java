package com.gregtechceu.gtceu.api.cover.filter;

import com.gregtechceu.gtceu.common.cover.data.FilterMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;

import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;

public interface CoverWithFluidFilter {

    @NotNull
    FilterHandler<FluidStack, FluidFilter> getFilterHandler();

    FilterMode getFilterMode();

    ManualIOMode getManualIOMode();
}
