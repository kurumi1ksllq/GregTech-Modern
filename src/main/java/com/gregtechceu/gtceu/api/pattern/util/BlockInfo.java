package com.gregtechceu.gtceu.api.pattern.util;

import com.lowdragmc.lowdraglib.utils.FacadeBlockAndTintGetter;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;

public class BlockInfo {
    public static final BlockInfo EMPTY = new BlockInfo(Blocks.AIR);

    @Getter
    private BlockState blockState;
    private boolean hasBlockEntity;
    private CompoundTag tag;
    private ItemStack itemStack;
    @Getter
    private BlockEntity blockEntity;
    private BlockEntity lastEntity;

    public BlockInfo(Block block) {
        this(block.defaultBlockState());
    }

    public BlockInfo(BlockState blockState) {
        this(blockState, false);
    }

    public BlockInfo(BlockState blockState, boolean hasBlockEntity) {
        this(blockState, hasBlockEntity, null, null);
    }

    public BlockInfo(BlockState blockState, BlockEntity blockEntity) {
        this(blockState, true, null, blockEntity);
    }

    public BlockInfo(BlockState blockState, boolean hasBlockEntity, ItemStack itemStack, BlockEntity blockEntity) {
        this.blockState = blockState;
        this.hasBlockEntity = hasBlockEntity;
        this.itemStack = itemStack;
        this.blockEntity = blockEntity;
    }

    public static BlockInfo fromBlockState(BlockState state) {
        if(state.getBlock() instanceof EntityBlock entityBlock) {
            BlockEntity be = entityBlock.newBlockEntity(BlockPos.ZERO, state);
            if(be != null) {
                return new BlockInfo(state, true);
            }
        }
        return new BlockInfo(state);
    }

    public static BlockInfo fromBlock(Block block) {
        return fromBlockState(block.defaultBlockState());
    }

    public boolean hasBlockEntity() {
        return hasBlockEntity;
    }

    public BlockEntity getBlockEntity(BlockPos pos) {
        if(hasBlockEntity && blockState.getBlock() instanceof EntityBlock entityBlock) {
            if(lastEntity != null && lastEntity.getBlockPos().equals(pos)) {
                return lastEntity;
            }
            lastEntity = entityBlock.newBlockEntity(pos, blockState);
            if(tag != null && lastEntity != null) {
                var tag2 = lastEntity.saveWithoutMetadata();
                var tag3 = tag2.copy();
                tag2.merge(tag);
                if(!tag2.equals(tag3)) {
                    lastEntity.load(tag2);
                }
            }
            return lastEntity;
        }
        return null;
    }

    public BlockEntity getBlockEntity(Level level, BlockPos pos) {
        BlockEntity entity = getBlockEntity(pos);
        if(entity != null) {
            entity.setLevel(level);
        }
        return entity;
    }

    public ItemStack getItemStackForm() {
        return itemStack == null ? new ItemStack(blockState.getBlock()) : itemStack;
    }

    public ItemStack getItemStackForm(BlockAndTintGetter level, BlockPos pos) {
        if(itemStack != null) return itemStack;
        return blockState.getBlock().getCloneItemStack(new FacadeBlockAndTintGetter(level, pos, blockState, null), pos, blockState);
    }

    public void apply(Level level, BlockPos pos) {
        level.setBlockAndUpdate(pos, blockState);
        BlockEntity be = getBlockEntity(pos);
        if(be != null) {
            level.setBlockEntity(be);
        }
    }

    public void clearBlockEntityCache() {
        lastEntity = null;
    }
}
