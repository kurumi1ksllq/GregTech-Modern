package com.gregtechceu.gtceu.api.graphnet.pipenet;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import org.jetbrains.annotations.NotNull;

public interface NodeExposingCapabilities {

    @NotNull
    ICapabilityProvider getProvider();

    default Direction exposedFacing() {
        return null;
    }
}
