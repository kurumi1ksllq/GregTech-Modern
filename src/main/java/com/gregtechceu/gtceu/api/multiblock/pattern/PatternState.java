package com.gregtechceu.gtceu.api.multiblock.pattern;

import com.gregtechceu.gtceu.api.block.ActiveBlock;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.multiblock.error.PatternError;

import com.gregtechceu.gtceu.api.multiblock.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

/*
 * Contains vital information to an instanced version of a structure pattern.
 */
public class PatternState {

    @Getter
    protected BlockPos controllerPos;
    @Getter
    protected IMultiController controller;
    @Getter
    @Setter
    @DescSynced
    protected boolean isFormed = false;
    @Getter
    @DescSynced
    protected boolean isFlipped = false;
    @Setter
    @Getter
    protected boolean actualFlipped = false;
    @Setter
    protected boolean shouldUpdate = true;
    @Setter
    @Getter
    protected PatternError error;
    @Getter
    protected CheckState state;
    @Getter
    protected Set<BlockPos> posCache = new HashSet<>();
    @Getter
    @NotNull
    protected CurrentBlockInfo cbi = new CurrentBlockInfo();
    protected final Object2IntMap<SimplePredicate> globalCount = new Object2IntOpenHashMap<>();
    protected final Object2IntMap<SimplePredicate> layerCount = new Object2IntOpenHashMap<>();
    protected final Long2ObjectMap<BlockInfo> cache = new Long2ObjectOpenHashMap<>();

    public void setController(IMultiController controller, BlockPos controllerPos) {
        this.controller = controller;
        this.controllerPos = controllerPos;
    }

    @ApiStatus.Internal
    public void setFlipped(boolean flipped) {
        isFlipped = flipped;
    }

    public boolean shouldUpdate() {
        return shouldUpdate;
    }

    public boolean hasError() {
        return error != null;
    }

    protected void setState(CheckState state) {
        this.state = state;
    }

    public void onBlockStateChanged(BlockPos pos, BlockState state) {
        if(cbi.getLevel() instanceof ServerLevel serverLevel) {
            if(pos.equals(controllerPos)) {
                if(controller != null) {
                    if(!state.is(controller.self().getBlockState().getBlock())) {
                        controller.invalidateStructure(MultiblockControllerMachine.DEFAULT_STRUCTURE);
                        var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                        mwsd.removeMapping(this);
                    }
                }
            } else {
              if(controller != null) {
                  if(controller.isFormed() && state.getBlock() instanceof ActiveBlock) {
                      return;
                  }
                  if(!controller.checkStructurePattern(MultiblockControllerMachine.DEFAULT_STRUCTURE).hasError()) {
                      controller.formStructure(MultiblockControllerMachine.DEFAULT_STRUCTURE);
                  } else {
                      controller.invalidateStructure(MultiblockControllerMachine.DEFAULT_STRUCTURE);
                      var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                      mwsd.removeMapping(this);
                      mwsd.addAsyncLogic(controller);
                  }
              }

            }
        }
    }


    public enum CheckState {
        /**
         * The cache doesn't match with the structure's data. The structure has been rechecked from scratch, is valid,
         * and the cache is now populated.
         */
        VALID_UNCACHED,

        /**
         * The cache matches the structure's data.
         */
        VALID_CACHED,

        /**
         * The cache doesn't match with the structure's data. The structure has been rechecked from scratch, is invalid,
         * and the cache is now empty.
         */
        INVALID_CACHED,

        /**
         * The cache is empty. The structure has been rechecked from scratch and is invalid, the cache remains empty.
         */
        INVALID_UNCACHED;

        public boolean isValid() {
            return ordinal() < 2;
        }
    }

}
