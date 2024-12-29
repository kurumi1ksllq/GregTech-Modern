package com.gregtechceu.gtceu.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * IAppearance is an interface that provides a method to get the appearance of a block.
 * This is useful for blocks that have different appearances based on context.
 * For example, a block that changes appearance based on the block it is facing.
 * @see BlockState
 */
public interface IAppearance {
    /**
     * Gets the appearance of the block based on the context
     * @inheritDoc IForgeBlock#getAppearance(BlockState, BlockAndTintGetter, BlockPos, Direction, BlockState, BlockPos)
     * @param state the block state
     * @param level the block and tint getter
     * @param pos the block position
     * @param side the direction of the block
     * @param sourceState the block state of the source
     * @param sourcePos the block position of the source
     * @return the block state of the appearance
     */
    @Nullable
    default BlockState getBlockAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side,
                                          BlockState sourceState, BlockPos sourcePos) {
        return state;
    }
}