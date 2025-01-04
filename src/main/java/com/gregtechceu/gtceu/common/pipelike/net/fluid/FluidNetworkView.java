package com.gregtechceu.gtceu.common.pipelike.net.fluid;

import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerList;

import net.minecraftforge.fluids.capability.IFluidHandler;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;

public record FluidNetworkView(FluidHandlerList handler, BiMap<IFluidHandler, NetNode> handlerNetNodeBiMap) {

    public static final FluidNetworkView EMPTY = FluidNetworkView.of(ImmutableBiMap.of());

    public static FluidNetworkView of(BiMap<IFluidHandler, NetNode> handlerNetNodeBiMap) {
        return new FluidNetworkView(new FluidHandlerList(handlerNetNodeBiMap.keySet()), handlerNetNodeBiMap);
    }
}
