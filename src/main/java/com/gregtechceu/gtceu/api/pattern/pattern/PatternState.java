package com.gregtechceu.gtceu.api.pattern.pattern;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.ApiStatus;

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
    protected boolean isFormed = false;
    @Getter
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
    protected Set<BlockPos> posCache;
    @Getter
    protected CurrentBlockInfo cbi;



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
