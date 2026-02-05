package com.cleanroommc.modularui.integration.xei.entry.fluid;

import com.cleanroommc.modularui.integration.xei.entry.EntryList;

import net.minecraftforge.fluids.FluidStack;

import java.util.List;

public sealed interface FluidEntryList extends EntryList<FluidStack>
        permits FluidStackList, FluidTagList, FluidHolderSetList {

    List<FluidStack> getStacks();

    boolean isEmpty();
}
