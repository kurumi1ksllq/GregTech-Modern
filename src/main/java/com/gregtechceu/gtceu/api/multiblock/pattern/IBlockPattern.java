package com.gregtechceu.gtceu.api.multiblock.pattern;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.BetterBlockPos;
import com.gregtechceu.gtceu.api.multiblock.OriginOffset;
import com.gregtechceu.gtceu.api.multiblock.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface IBlockPattern {
    /**
     * Checks the pattern fast, this should always be preferred to checkPatternAt(...) for multiblock code.
     *
     * @param level         The world the multiblock is in.
     * @param centerPos     The position of the controller.
     * @param frontFacing   The front facing of the controller, obtained via
     *                      {@link MultiblockControllerMachine#getFrontFacing()}
     * @param upwardsFacing The up facing of the controller, obtained via
     *                      {@link MultiblockControllerMachine#getUpwardsFacing()}
     * @param allowsFlip    Whether the multiblock allows flipping.
     * @return The internal state of the pattern. Check whether its valid first before using other fields.
     */
    PatternState checkPatternFastAt(Level level, BlockPos centerPos, Direction frontFacing, Direction upwardsFacing, boolean allowsFlip);

    /**
     * Checks the whole pattern, you should probably use checkPatternFastAt(...) instead.
     *
     * @param level         The world the multiblock is in.
     * @param centerPos     The position of the controller.
     * @param frontFacing   The front facing of the controller, obtained via
     *                      {@link MultiblockControllerMachine#getFrontFacing()}
     * @param upwardsFacing The up facing of the controller, obtained via
     *                      {@link MultiblockControllerMachine#getUpwardsFacing()}
     * @param isFlipped     Is the multiblock flipped or not.
     * @return True if the check passed, in which case the context is mutated for returning from checkPatternFastAt(...)
     */
    boolean checkPatternAt(Level level, BlockPos centerPos, Direction frontFacing, Direction upwardsFacing, boolean isFlipped);

    /**
     * Gets the default shape, if the multiblock does not specify one. Return null to represent the default shape does
     * not exist.
     *
     * @param keyMap The map from multiblock builder for autobuild.
     * @return The long key is using {@link BetterBlockPos#toLong(int, int, int)} with x, y, z
     *         respectively being. The map is sorted using the natural ordering(thus with x, y, z order).
     */
    Long2ObjectSortedMap<TraceabilityPredicate> getDefaultShape(MultiblockControllerMachine src, @NotNull Map<String, String> keyMap);

    void setActivePatternState(PatternState patternState);

    /**
     * Gets the internal pattern state, you should use the one returned from
     * {@link IBlockPattern#checkPatternFastAt(Level, BlockPos, Direction, Direction, boolean)} always
     * except for the shouldUpdate field.
     */
    PatternState getPatternState();

    /**
     * Clears the cache for checkPatternFastAt(...) in case something in the pattern is changed. Default impl just
     * getCache and then clears it.
     */
    default void clearCache() {
        getCache().clear();
    }

    /**
     * Gets the cache, do not modify. Note that the cache stores everything in the AABB of the substructure, except for
     * any() TraceabilityPredicates.
     *
     * @return The cache for rapid pattern checking.
     */
    Long2ObjectMap<BlockInfo> getCache();

    OriginOffset getOffset();

    default void moveOffset(RelativeDirection dir, int amount) {
        getOffset().move(dir, amount);
    }

    default void moveOffset(RelativeDirection dir) {
        getOffset().move(dir);
    }
}
