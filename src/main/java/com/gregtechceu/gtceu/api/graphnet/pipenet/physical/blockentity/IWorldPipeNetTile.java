package com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

public interface IWorldPipeNetTile extends ICapabilityProvider {

    @NotNull
    EnumMap<Direction, BlockEntity> getTargetsWithCapabilities(WorldPipeNode destination);

    @Nullable
    BlockEntity getTargetWithCapabilities(WorldPipeNode destination, Direction facing);

    PipeCapabilityWrapper getWrapperForNode(WorldPipeNode node);

    @NotNull
    ICoverable getCoverHolder();

    @Nullable
    Level getLevel();

    BlockPos getBlockPos();
}
