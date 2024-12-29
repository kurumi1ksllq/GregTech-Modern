package com.gregtechceu.gtceu.api.block;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * ActiveBlock extends AppearanceBlock with the property ACTIVE.
 * This is useful for blocks that have different appearances when active.
 * For example, a block that changes appearance when powered by redstone.
 * @see AppearanceBlock
 * @see Block
 * @see BlockState
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ActiveBlock extends AppearanceBlock {
    // Properties
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    /**
     * Constructor for ActiveBlock that adds the ACTIVE property to the block state
     * Also registers the default block state and properties
     * @param properties the properties of the block
     */
    public ActiveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ACTIVE, false));
    }

    /**
     * Adds the ACTIVE property to the block state
     * Overrides the createBlockStateDefinition method in AppearanceBlock
     * @param builder the block state builder
     */
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    /**
     * Changes the active state of the block
     * @param state the block state
     * @param active whether the block is active or not
     * @return the new block state
     */
    public BlockState changeActive(BlockState state, boolean active) {
        if (state.is(this)) return state.setValue(ACTIVE, active);
        return state;
    }

    /**
     * Gets the active state of the block
     * @param state the block state
     * @return whether the block is active or not
     */
    public boolean isActive(BlockState state) {
        return state.getValue(ACTIVE);
    }

    /**
     * Gets the block appearance based on the active state
     * Overrides the getBlockAppearance method in AppearanceBlock
     * @param state the block state
     * @param level the block and tint getter
     * @param pos the block position
     * @param side the direction of the block
     * @param sourceState the source block state
     * @param sourcePos the source block position
     * @return the block state
     */
    @Override
    public BlockState getBlockAppearance(BlockState state, BlockAndTintGetter level, BlockPos pos, Direction side,
                                         BlockState sourceState, BlockPos sourcePos) {
        return defaultBlockState();
    }
}