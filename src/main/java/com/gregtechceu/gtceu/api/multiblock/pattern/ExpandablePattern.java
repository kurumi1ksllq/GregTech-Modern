package com.gregtechceu.gtceu.api.multiblock.pattern;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.OriginOffset;
import com.gregtechceu.gtceu.api.multiblock.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.error.SinglePredicateError;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.utils.QuadFunction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.longs.*;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;

public class ExpandablePattern implements IBlockPattern {

    protected final QuadFunction<Level, BlockPos.MutableBlockPos, Direction, Direction, int[]> boundsFunc;
    protected final BiFunction<BlockPos.MutableBlockPos, int[], TraceabilityPredicate> predicateFunc;
    protected final OriginOffset offset = new OriginOffset();

    protected final RelativeDirection[] directions;
    // protected final Object2IntMap<SimplePredicate> globalCount = new Object2IntOpenHashMap<>();
    protected PatternState patternState;
    // protected final Long2ObjectMap<BlockInfo> cache = new Long2ObjectOpenHashMap<>();

    private final Lock patternLock = new ReentrantLock();

    public ExpandablePattern(@NotNull QuadFunction<Level, BlockPos.MutableBlockPos, Direction, Direction, int[]> boundsFunc,
                             @NotNull BiFunction<BlockPos.MutableBlockPos, int[], TraceabilityPredicate> predicateFunc,
                             @NotNull RelativeDirection[] directions) {
        this.boundsFunc = boundsFunc;
        this.predicateFunc = predicateFunc;
        this.directions = directions;
    }

    @Override
    public void setActivePatternState(PatternState state) {
        patternLock.lock();
        try {
            patternState = state;
        } finally {
            patternLock.unlock();
        }
    }

    @Override
    public PatternState checkPatternFastAt(Level level, BlockPos centerPos, Direction frontFacing,
                                           Direction upwardsFacing, boolean allowsFlip) {
        if (patternState == null) {
            throw new IllegalStateException("PatternState not set");
        }
        if (!patternState.cache.isEmpty()) {
            boolean pass = true;
            BlockPos.MutableBlockPos mbp = new BlockPos.MutableBlockPos();
            for (var entry : patternState.cache.long2ObjectEntrySet()) {
                BlockPos pos = mbp.set(entry.getLongKey()).immutable();
                BlockState state = level.getBlockState(pos);

                if (state != entry.getValue().getBlockState()) {
                    pass = false;
                    break;
                }

                BlockEntity cachedBE = entry.getValue().getBlockEntity();
                if (cachedBE != null) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != cachedBE) {
                        pass = false;
                        break;
                    }
                }
            }
            if (pass) {
                if (patternState.hasError())
                    patternState.setState(PatternState.CheckState.INVALID_CACHED);
                else
                    patternState.setState(PatternState.CheckState.VALID_CACHED);

                return patternState;
            }
        }

        patternState.setFlipped(false);
        boolean valid = checkPatternAt(level, centerPos, frontFacing, upwardsFacing, false);
        if (valid) {
            patternState.setState(PatternState.CheckState.VALID_UNCACHED);
            return patternState;
        }

        clearCache();
        patternState.setState(PatternState.CheckState.INVALID_UNCACHED);
        return patternState;
    }

    @Override
    public boolean checkPatternAt(Level level, BlockPos centerPos, Direction frontFacing, Direction upwardsFacing,
                                  boolean isFlipped) {
        if (patternState == null) {
            throw new IllegalStateException("PatternState not set");
        }
        int[] bounds = boundsFunc.apply(level, centerPos.mutable(), frontFacing, upwardsFacing);
        if (bounds == null) return false;

        patternState.globalCount.clear();

        BlockPos.MutableBlockPos negCorner = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos posCorner = new BlockPos.MutableBlockPos();

        Direction[] absolutes = new Direction[3];

        for (int i = 0; i < 3; i++) {
            RelativeDirection selected = directions[i];

            absolutes[i] = selected.getRelativeFacing(frontFacing, upwardsFacing, isFlipped);

            if (i == 0) {
                negCorner.setX(-bounds[selected.oppositeOrdinal()]);
                posCorner.setX(bounds[selected.ordinal()]);
            } else if (i == 1) {
                negCorner.setY(-bounds[selected.oppositeOrdinal()]);
                posCorner.setY(bounds[selected.ordinal()]);
            } else {
                negCorner.setZ(-bounds[selected.oppositeOrdinal()]);
                posCorner.setZ(bounds[selected.ordinal()]);
            }
        }

        patternState.cbi.setLevel(level);

        BlockPos.MutableBlockPos translation = centerPos.mutable();

        // SOUTH, UP, EAST means point is +z, line is +y, plane is +x. this basically means the x val of the iter is
        // aisle count, y is str count, and z is char count.
        // for(BetterBlockPos pos : BetterBlockPos.allInBox(negCorner, posCorner, Direction.SOUTH, Direction.UP,
        // Direction.EAST)) {
        for (var pos : BlockPos.betweenClosed(negCorner, posCorner)) {
            BlockPos.MutableBlockPos mPos = pos.mutable();
            TraceabilityPredicate pred = predicateFunc.apply(mPos, bounds);

            // int[] arr = pos.getAll();
            // this basically reshuffles the coordinates into absolute form from relative form
            mPos.set(BlockPos.ZERO).move(absolutes[0], mPos.getX()).move(absolutes[1], mPos.getY()).move(absolutes[2],
                    mPos.getZ());
            // pos.zero().offset(absolutes[0], arr[0]).offset(absolutes[1], arr[1]).offset(absolutes[2], arr[2]);
            // translate from the origin to the center
            patternState.cbi.setCurrentPos(mPos.offset(translation));

            if (pred != TraceabilityPredicate.ANY) {
                var bstate = patternState.cbi.retrieveCurrentBlockState();
                BlockEntity be = patternState.cbi.retrieveCurrentBlockEntity();
                patternState.cache.put(mPos.asLong(), new BlockInfo(bstate, be));
            }

            PatternError res = pred.test(patternState.cbi, patternState.globalCount, null);
            if (res != null) {
                patternState.setError(res);
                return false;
            }
        }

        for (var entry : patternState.globalCount.object2IntEntrySet()) {
            if (entry.getIntValue() < entry.getKey().minCount) {
                patternState
                        .setError(new SinglePredicateError(entry.getKey(), SinglePredicateError.ErrorType.MIN_COUNT));
                return false;
            }
        }
        return true;
    }

    @Override
    public Long2ObjectSortedMap<TraceabilityPredicate> getDefaultShape(MultiblockControllerMachine src,
                                                                       @NotNull Map<String, String> keyMap) {
        Direction front = src.getFrontFacing();
        Direction up = src.getUpwardsFacing();

        int[] bounds = boundsFunc.apply(src.getLevel(), src.getPos().mutable(), front, up);
        if (bounds == null) return Long2ObjectSortedMaps.emptyMap();

        Long2ObjectSortedMap<TraceabilityPredicate> predicates = new Long2ObjectRBTreeMap<>();

        BlockPos.MutableBlockPos negCorner = new BlockPos.MutableBlockPos();
        BlockPos.MutableBlockPos posCorner = new BlockPos.MutableBlockPos();

        Direction[] absolutes = new Direction[3];

        for (int i = 0; i < 3; i++) {
            RelativeDirection selected = directions[i];

            absolutes[i] = selected.getRelativeFacing(front, up, false);

            if (i == 0) {
                negCorner.setX(-bounds[selected.oppositeOrdinal()]);
                posCorner.setX(bounds[selected.ordinal()]);
            } else if (i == 1) {
                negCorner.setY(-bounds[selected.oppositeOrdinal()]);
                posCorner.setY(bounds[selected.ordinal()]);
            } else {
                negCorner.setZ(-bounds[selected.oppositeOrdinal()]);
                posCorner.setZ(bounds[selected.ordinal()]);
            }
        }

        // BetterBlockPos translation = new BetterBlockPos(src.getPos());

        for (var pos : BlockPos.betweenClosed(negCorner, posCorner)) {
            BlockPos.MutableBlockPos mPos = pos.mutable();
            TraceabilityPredicate pred = predicateFunc.apply(mPos, bounds);

            // int[] arr = pos.getAll();
            // this basically reshuffles the coordinates into absolute form from relative form
            mPos.set(BlockPos.ZERO).move(absolutes[0], mPos.getX()).move(absolutes[1], mPos.getY()).move(absolutes[2],
                    mPos.getZ());

            if (pred != TraceabilityPredicate.ANY && pred != TraceabilityPredicate.AIR) {
                predicates.put(mPos.asLong(), pred);
            }
        }
        return predicates;
    }

    @Override
    public PatternState getPatternState() {
        return patternState;
    }

    @Override
    public Long2ObjectMap<BlockInfo> getCache() {
        if (patternState == null) {
            throw new IllegalStateException("PatternState not set");
        }
        return patternState.cache;
    }

    @Override
    public OriginOffset getOffset() {
        return offset;
    }
}
