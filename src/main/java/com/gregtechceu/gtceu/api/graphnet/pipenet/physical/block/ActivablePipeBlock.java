package com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block;

import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeStructure;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.ActivablePipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.common.data.GTBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public abstract class ActivablePipeBlock extends PipeBlock {

    public ActivablePipeBlock(BlockBehaviour.Properties properties, IPipeStructure structure) {
        super(properties, structure);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ActivablePipeBlockEntity(GTBlockEntities.ACTIVATABLE_PIPE.get(), pos, state);
    }

    @Override
    public @Nullable ActivablePipeBlockEntity getBlockEntity(@NotNull BlockGetter level, @NotNull BlockPos pos) {
        if (lastTilePos.get().equals(pos)) {
            PipeBlockEntity blockEntity = lastTile.get().get();
            if (blockEntity != null && !blockEntity.isRemoved()) {
                return (ActivablePipeBlockEntity) blockEntity;
            }
        }
        BlockEntity tile = level.getBlockEntity(pos);
        if (tile instanceof ActivablePipeBlockEntity pipe) {
            lastTilePos.set(pos.immutable());
            lastTile.set(new WeakReference<>(pipe));
            return pipe;
        } else return null;
    }
}
