package com.gregtechceu.gtceu.api.multiblock.pattern;

import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.multiblock.OriginOffset;
import com.gregtechceu.gtceu.api.multiblock.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.error.SinglePredicateError;
import com.gregtechceu.gtceu.api.multiblock.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class BlockPattern implements IBlockPattern {

    protected final RelativeDirection[] directions;

    protected final int[] dimensions;
    protected final OriginOffset offset;

    protected final boolean hasStartOffset;
    protected final PatternAisle[] aisles;
    @Getter
    protected final AisleStrategy aisleStrategy;
    protected final Char2ObjectMap<TraceabilityPredicate> predicates;
    protected volatile PatternState patternState;

    private final ReadWriteLock patternLock = new ReentrantReadWriteLock();

    public BlockPattern(@NotNull PatternAisle @NotNull [] aisles, @NotNull AisleStrategy aisleStrategy,
                        int @NotNull [] dimensions, @NotNull RelativeDirection @NotNull [] directions,
                        @Nullable OriginOffset offset,
                        @NotNull Char2ObjectMap<@NotNull TraceabilityPredicate> predicates,
                        char centerChar) {
        this.aisles = aisles;
        this.aisleStrategy = aisleStrategy;
        this.dimensions = dimensions;
        this.directions = directions;
        this.predicates = predicates;
        hasStartOffset = offset != null;

        if (offset == null) {
            this.offset = new OriginOffset();
            legacyStartOffset(centerChar);
        } else {
            this.offset = offset;
        }

        patternState = new PatternState();
    }

    /*@Override
    public void setActivePatternState(PatternState state) {
        var wLock = patternLock.writeLock();
        try {
            wLock.lock();
            patternState = state;
        } finally {
            wLock.unlock();
        }
    }*/

    private void legacyStartOffset(char center) {
        if (center == 0) return;
        for (int aisleI = 0; aisleI < dimensions[0]; aisleI++) {
            int[] res = aisles[aisleI].firstInstanceOf(center);
            if (res != null) {
                moveOffset(directions[0], -aisleI);
                moveOffset(directions[1], -res[0]);
                moveOffset(directions[2], -res[1]);
                return;
            }
        }
        throw new IllegalStateException("Failed to find center char:  '" + center + "'");
    }

    @Override
    public PatternState checkPatternFastAt(Level level, IMultiController controller, BlockPos centerPos, Direction frontFacing,
                                           Direction upwardsFacing, boolean allowsFlip) {
        patternState.setController(controller, centerPos);
        if (!patternState.cache.isEmpty()) {
            boolean pass = true;
            BlockPos.MutableBlockPos mBlockPos = new BlockPos.MutableBlockPos();
            for (var entry : patternState.cache.long2ObjectEntrySet()) {
                BlockPos pos = mBlockPos.set(entry.getLongKey()).immutable();
                BlockState state = level.getBlockState(pos);

                if (state != entry.getValue().getBlockState()) {
                    pass = false;
                    break;
                }

                BlockEntity cachedBlockEntity = entry.getValue().getBlockEntity();

                if (cachedBlockEntity != null) {
                    BlockEntity be = level.getBlockEntity(pos);
                    if (be != cachedBlockEntity) {
                        pass = false;
                        break;
                    }
                }
            }

            if (pass) {
                if (patternState.hasError()) {
                    patternState.setState(PatternState.CheckState.INVALID_CACHED);
                } else {
                    patternState.setState(PatternState.CheckState.VALID_CACHED);
                }

                return patternState;
            }
        }

        boolean valid = checkPatternAt(level, centerPos, frontFacing, upwardsFacing, false);
        if (valid) {
            // reaching here means the cache failed or was empty
            patternState.setState(PatternState.CheckState.VALID_UNCACHED);
            patternState.setFlipped(false);
            return patternState;
        }

        if (allowsFlip) {
            valid = checkPatternAt(level, centerPos, frontFacing, upwardsFacing, true);
        }
        if (!valid) { // dont store a partial formed cache
            patternState.getCache().clear();
            patternState.setState(PatternState.CheckState.INVALID_UNCACHED);
            return patternState;
        }


        patternState.setState(PatternState.CheckState.VALID_UNCACHED);
        patternState.setFlipped(true);
        return patternState;
    }

    /*@Override
    public Long2ObjectMap<BlockInfo> getCache() {
        var rLock = patternLock.readLock();
        try {
            rLock.lock();
            if (patternState == null) {
                throw new IllegalStateException("PatternState not set");
            }
            return new Long2ObjectOpenHashMap<>(patternState.cache);
        } finally {
            rLock.unlock();
        }
    */

    @Override
    public boolean checkPatternAt(Level level, BlockPos centerPos, Direction frontFacing, Direction upwardsFacing,
                                  boolean isFlipped) {
        if (patternState == null) {
            throw new IllegalStateException("PatternState not set");
        }
        patternState.globalCount.clear();
        patternState.layerCount.clear();
        patternState.cache.clear();

        patternState.cbi.setLevel(level);

        BlockPos.MutableBlockPos controllerPos = centerPos.mutable();

        aisleStrategy.pattern = this;
        aisleStrategy.start(controllerPos, frontFacing, upwardsFacing);
        if (!aisleStrategy.check(isFlipped)) return false;

        for (Object2IntMap.Entry<SimplePredicate> entry : patternState.globalCount.object2IntEntrySet()) {
            if (entry.getIntValue() < entry.getKey().minCount) {
                patternState.setError(new SinglePredicateError(entry.getKey(), SinglePredicateError.ErrorType.MIN_COUNT));
                return false;
            }
        }

        patternState.setError(null);
        return true;
    }

    /**
     * Checks a specific aisle for validity
     *
     * @param controllerPos The position of the controller
     * @param frontFacing   The front facing of the controller
     * @param upwardsFacing The up facing of the controller
     * @param aisleIndex    The index of the aisle, this is where the pattern is gotten from, treats repeatable aisles
     *                      as only 1
     * @param aisleOffset   The offset of the aisle, how much offset in aisleDir to check the blocks in world, for
     *                      example, if the first aisle is repeated 2 times, aisleIndex is 1 while this is 2
     * @param flip          Whether to flip or not
     * @return True if the check passed
     */
    public boolean checkAisle(BlockPos.MutableBlockPos controllerPos, Direction frontFacing, Direction upwardsFacing,
                              int aisleIndex, int aisleOffset, boolean flip) {
        Direction absoluteAisle = directions[0].getRelativeFacing(frontFacing, upwardsFacing, flip);
        Direction absoluteString = directions[1].getRelativeFacing(frontFacing, upwardsFacing, flip);
        Direction absoluteChar = directions[2].getRelativeFacing(frontFacing, upwardsFacing, flip);

        BlockPos.MutableBlockPos aisleStart = startPos(controllerPos, frontFacing, upwardsFacing, flip)
                .move(absoluteAisle, aisleOffset);

        BlockPos.MutableBlockPos stringStart = aisleStart.mutable();
        BlockPos.MutableBlockPos charPos = aisleStart.mutable();
        PatternAisle aisle = aisles[aisleIndex];

        patternState.layerCount.clear();

        for (int stringI = 0; stringI < dimensions[1]; stringI++) {
            for (int charI = 0; charI < dimensions[2]; charI++) {
                patternState.cbi.setCurrentPos(charPos);
                TraceabilityPredicate pred = predicates.get(aisle.charAt(stringI, charI));

                if (pred != TraceabilityPredicate.ANY) {
                    var be = patternState.cbi.retrieveCurrentBlockEntity();
                    var state = patternState.cbi.retrieveCurrentBlockState();
                    patternState.cache.put(charPos.asLong(), new BlockInfo(state, be));
                    patternState.posCache.add(charPos.immutable());
                }

                PatternError res = pred.test(patternState.cbi, patternState.globalCount, patternState.layerCount);
                if (res != null) {
                    patternState.setError(res);
                    return false;
                }

                charPos.move(absoluteChar);
            }

            stringStart.move(absoluteString);
            charPos.set(stringStart);
        }

        for (Object2IntMap.Entry<SimplePredicate> entry : patternState.layerCount.object2IntEntrySet()) {
            if (entry.getIntValue() < entry.getKey().minLayerCount) {
                patternState.setError(
                        new SinglePredicateError(entry.getKey(), SinglePredicateError.ErrorType.MIN_LAYER_COUNT));
                return false;
            }
        }

        for (Object2IntMap.Entry<SimplePredicate> entry : patternState.layerCount.object2IntEntrySet()) {
            patternState.globalCount.put(entry.getKey(),
                    patternState.globalCount.getInt(entry.getKey()) + entry.getIntValue());
        }

        return true;
    }

    public int getRepetitionCount(int aisleI) {
        return aisles[aisleI].actualRepeats;
    }

    @Override
    public Long2ObjectSortedMap<TraceabilityPredicate> getDefaultShape(MultiblockControllerMachine src,
                                                                       @NotNull Map<String, String> keyMap) {
        Long2ObjectSortedMap<TraceabilityPredicate> map = new Long2ObjectRBTreeMap<>();
        Direction absoluteAisle = directions[0].getRelativeFacing(src.getFrontFacing(), src.getUpwardsFacing());
        Direction absoluteString = directions[1].getRelativeFacing(src.getFrontFacing(), src.getUpwardsFacing());
        Direction absoluteChar = directions[2].getRelativeFacing(src.getFrontFacing(), src.getUpwardsFacing());

        BlockPos.MutableBlockPos pos = src.getPos().mutable();
        BlockPos.MutableBlockPos start = startPos(pos, src.getFrontFacing(), src.getUpwardsFacing(), false);
        BlockPos.MutableBlockPos serial = start.mutable();

        int[] order = aisleStrategy.getDefaultAisles(keyMap);
        for (int i = 0; i < order.length; i++) {
            for (int j = 0; j < dimensions[1]; j++) {
                for (int k = 0; k < dimensions[2]; k++) {
                    TraceabilityPredicate pred = predicates.get(aisles[order[i]].charAt(j, k));
                    if (pred != TraceabilityPredicate.ANY && pred != TraceabilityPredicate.AIR)
                        map.put(serial.asLong(), predicates.get(aisles[order[i]].charAt(j, k)));
                    serial.move(absoluteChar);
                }
                serial.move(absoluteString);
                serial.move(absoluteChar.getOpposite(), dimensions[2]);
            }
            serial.set(start);
            serial.move(absoluteAisle, i + 1);
        }

        return map;
    }

    @Override
    public PatternState getPatternState() {
        return patternState;
    }

    @Override
    public OriginOffset getOffset() {
        return offset;
    }

    private BlockPos.MutableBlockPos startPos(BlockPos.MutableBlockPos controllerPos, Direction frontFacing,
                                              Direction upwardsFacing, boolean flip) {
        BlockPos.MutableBlockPos start = controllerPos.mutable();
        offset.apply(start, frontFacing, upwardsFacing, flip);
        return start;
    }
}
