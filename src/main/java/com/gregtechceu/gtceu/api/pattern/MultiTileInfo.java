package com.gregtechceu.gtceu.api.pattern;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.gregtechceu.gtceu.api.pattern.error.PatternStringError;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Class allowing access to a block at a certain pos for structure checks.
 */
public class MultiTileInfo {

    /*public final static PatternError UNLOAD_ERROR = new PatternStringError("multiblocked.pattern.error.chunk");
    public final static PatternError UNINIT_ERROR = new PatternStringError("multiblocked.pattern.error.init");*/

    @Setter
    @Getter
    protected Level level;
    @Getter
    private BlockPos pos;
    private BlockState blockState;
    private BlockEntity tileEntity;
    private boolean teInitialized;
    /*public BlockPos controllerPos;
    public IMultiController lastController;*/

    // persist
    public LongOpenHashSet cache;

    /*public MultiblockState(Level level, BlockPos controllerPos) {
        this.level = level;
        this.controllerPos = controllerPos;
    }*/

    protected void clean() {
        cache = new LongOpenHashSet();
    }

    public void update(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos.immutable();
        this.blockState = null;
        this.tileEntity = null;
        this.teInitialized = false;
    }

    public IMultiController getController() {
       /* if (level.isLoaded(controllerPos)) {
            if (level.getBlockEntity(controllerPos) instanceof IMachineBlockEntity machineBlockEntity &&
                    machineBlockEntity.getMetaMachine() instanceof IMultiController controller) {
                return lastController = controller;
            }
        } else {
            GTCEu.LOGGER.error("Level is not loaded when trying to get controller pos");
        }*/
        return null;
    }

    public BlockState getBlockState() {
        if (this.blockState == null) {
            this.blockState = this.level.getBlockState(this.pos);
        }
        return this.blockState;
    }

    @Nullable
    public BlockEntity getTileEntity() {
        if (!getBlockState().hasBlockEntity()) {
            return null;
        }
        if (this.tileEntity == null && !this.teInitialized) {
            this.tileEntity = this.level.getBlockEntity(this.pos);
            this.teInitialized = true;
        }

        return this.tileEntity;
    }

    public void setPos(BlockPos pos) {
        this.pos = pos.immutable();
        this.blockState = null;
        this.tileEntity = null;
        this.teInitialized = false;
    }

    public void setPos(BetterBlockPos pos) {
        this.pos = pos.immutable();
        this.blockState = null;
        this.tileEntity = null;
        this.teInitialized = false;
    }

    public void addPosCache(BlockPos pos) {
        cache.add(pos.asLong());
    }

    public boolean isPosInCache(BlockPos pos) {
        return cache.contains(pos.asLong());
    }

    public Collection<BlockPos> getCache() {
        return cache.stream().map(BlockPos::of).collect(Collectors.toList());
    }

    public void onBlockStateChanged(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            /*if (pos.equals(controllerPos)) {
                if (lastController != null) {
                    if (!state.is(lastController.self().getBlockState().getBlock())) {
                        lastController.invalidateStructure(MultiblockControllerMachine.DEFAULT_STRUCTURE);
                        var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                        mwsd.removeMapping(this);
                    }
                }
            } else {*/
                //IMultiController controller = getController();
                /*if (controller != null) {
                    if (controller.isFormed() && state.getBlock() instanceof ActiveBlock) {
                        *//*LongSet activeBlocks = getMatchContext().getOrDefault("vaBlocks", LongSets.emptySet());
                        if (activeBlocks.contains(pos.asLong())) {
                            // fine! it's caused by active blocks.
                            // speed up here!
                            return;
                        }*//*
                    }
                    if (controller.checkPatternWithLock()) {
                        // refresh structure
                        //controller.self().setFlipped(this.neededFlip);
                        controller.formStructure(MultiblockControllerMachine.DEFAULT_STRUCTURE);
                    } else {
                        // invalid structure
                        controller.self().setFlipped(false);
                        controller.invalidateStructure(MultiblockControllerMachine.DEFAULT_STRUCTURE);
                        var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                        mwsd.removeMapping(this);
                        mwsd.addAsyncLogic(controller);
                    }
                }*/
            //}
        }
    }
}
