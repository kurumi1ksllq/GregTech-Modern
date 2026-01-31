package com.gregtechceu.gtceu.api.pipenet;

import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

public class PipeNetworkType {

    public ResourceLocation networkID;

    public PipeNetworkType(ResourceLocation id) {
        networkID = id;
    }

    @Override
    public String toString() {
        return networkID.toString();
    }
}
