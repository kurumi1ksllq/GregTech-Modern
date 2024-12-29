package com.gregtechceu.gtceu.api.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

/**
 * @implNote AppearanceBlock is an abstract class that implements IAppearance.
 * This is useful for blocks that have different appearances based on context.
 * For example, a block that changes appearance based on the block it is facing.
 * @see IAppearance
 * @see Block
 * @see BlockState
 */
public class AppearanceBlock extends Block implements IAppearance {

    /**
     * Constructor for AppearanceBlock that sets the properties of the block
     * @param properties the properties of the block
     */
    public AppearanceBlock(Properties properties) {
        super(properties);
    }

    /**
     * Gets the appearance of the block based on the context
     * Overrides the getAppearance method in IAppearance
     * @param state the block state
     * @param level the block and tint getter
     * @param pos the block position
     * @param side the direction of the block
     * @param queryState the block state of the query
     * @param queryPos the block position of the query
     * @return the block state of the appearance
     */
    @Override
    public BlockState getAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side,
                                    @Nullable BlockState queryState, @Nullable BlockPos queryPos) {
        var appearance = this.getBlockAppearance(state, level, pos, side, queryState, queryPos);
        return appearance == null ? state : appearance;
    }
}
