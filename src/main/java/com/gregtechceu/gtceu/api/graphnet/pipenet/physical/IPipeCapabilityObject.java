package com.gregtechceu.gtceu.api.graphnet.pipenet.physical;

import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;

import net.minecraftforge.common.capabilities.ICapabilityProvider;

import org.jetbrains.annotations.NotNull;

public interface IPipeCapabilityObject extends ICapabilityProvider {

    void init(@NotNull PipeBlockEntity tile, @NotNull PipeCapabilityWrapper wrapper);
}
