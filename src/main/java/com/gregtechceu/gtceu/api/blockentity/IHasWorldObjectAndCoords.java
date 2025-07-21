package com.gregtechceu.gtceu.api.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

public interface IHasWorldObjectAndCoords extends IDirtyNotifiable {

    Level getLevel();

    BlockPos getBlockPos();

    default boolean isServerSide() {
        return getLevel() != null && !getLevel().isClientSide;
    }

    default boolean isClientSide() {
        return getLevel() != null && getLevel().isClientSide;
    }

    default void notifyBlockUpdate() {
        if (getLevel() != null) {
            getLevel().updateNeighborsAt(getBlockPos(), getLevel().getBlockState(getBlockPos()).getBlock());
        }
    }

    default void scheduleRenderUpdate() {
        var pos = getBlockPos();
        if (getLevel() != null) {
            var state = getLevel().getBlockState(pos);
            if (getLevel().isClientSide) {
                getLevel().sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            } else {
                getLevel().blockEvent(pos, state.getBlock(), 1, 0);
            }
        }
    }
}
