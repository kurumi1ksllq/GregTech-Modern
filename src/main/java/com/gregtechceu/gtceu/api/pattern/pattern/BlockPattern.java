package com.gregtechceu.gtceu.api.pattern.pattern;

import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.BetterBlockPos;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.OriginOffset;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.pattern.util.BlockInfo;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class BlockPattern implements IBlockPattern {

    protected final RelativeDirection[] directions;

    protected final int[] dimensions;
    protected final OriginOffset offset;

    protected final boolean hasStartOffset;
    protected final PatternAisle[] aisles;
    protected final AisleStrategy aisleStrategy;
    protected final Char2ObjectMap<TraceabilityPredicate> predicates;
    protected final MultiblockState multiblockState;
    protected final Object2IntMap<SimplePredicate> globalCount = new Object2IntOpenHashMap<>();
    protected final Object2IntMap<SimplePredicate> layerCount = new Object2IntOpenHashMap<>();
    protected final PatternState state = new PatternState();
    protected  final Long2ObjectMap<BlockInfo> cache = new Long2ObjectOpenHashMap<>();

    public BlockPattern(@NotNull PatternAisle @NotNull[] aisles, @NotNull AisleStrategy aisleStrategy,
                        int @NotNull[] dimensions, @NotNull RelativeDirection @NotNull[] directions,
                        @Nullable OriginOffset offset, @NotNull Char2ObjectMap<@NotNull TraceabilityPredicate> predicates,
                        char centerChar) {
        this.aisles = aisles;
        this.aisleStrategy = aisleStrategy;
        this.dimensions = dimensions;
        this.directions = directions;
        this.predicates = predicates;
        hasStartOffset = offset != null;

        if(offset == null) {
            this.offset = new OriginOffset();
            legacyStartOffset(centerChar);
        }
        else {
            this.offset = offset;
        }

        this.multiblockState = new MultiblockState();
    }

    private void legacyStartOffset(char center) {
        if(center == 0) return;
        for(int aisleI = 0; aisleI < dimensions[0]; aisleI++) {
            int[] res = aisles[aisleI].firstInstanceOf(center);
            if(res != null) {
                moveOffset(directions[0], -aisleI);
                moveOffset(directions[1], -res[0]);
                moveOffset(directions[2], -res[1]);
                return;
            }
        }
        throw new IllegalStateException("Failed to find center char:  '" + center + "'");
    }

    @Override
    public PatternState checkPatternFastAt(Level level, BlockPos centerPos, Direction frontFacing, Direction upwardsFacing, boolean allowsFlip) {
        if(!cache.isEmpty()) {
            boolean pass = true;
            BetterBlockPos bbPos = new BetterBlockPos();
            for(var entry : cache.long2ObjectEntrySet()) {
                BlockPos pos = bbPos.fromLong(entry.getLongKey()).immutable();
                BlockState state = level.getBlockState(pos);

                if(state != entry.getValue().getBlockState()) {
                    pass = false;
                    break;
                }

                BlockEntity cachedBlockEntity = entry.getValue().getBlockEntity(pos);

                if (cachedBlockEntity != null) {

                    BlockEntity be = level.getBlockEntity(pos);
                    if(be != cachedBlockEntity) {
                        pass = false;
                        break;
                    }
                }
            }

            if(pass) {
                if(state.hasError()) {
                    state.setState(PatternState.CheckState.INVALID_CACHED);
                } else {
                    state.setState(PatternState.CheckState.VALID_CACHED);
                }

                return state;
            }
        }

        boolean valid = checkPatternAt(level, centerPos, frontFacing, upwardsFacing, false);
        if (valid) {
            state.setState(PatternState.CheckState.VALID_UNCACHED);
            state.setFlipped(false);
            return state;
        }

        if(allowsFlip) {
            valid = checkPatternAt(level, centerPos, frontFacing, upwardsFacing, true);
        }
        if(!valid) {
            clearCache();
            state.setState(PatternState.CheckState.INVALID_UNCACHED);
            return state;
        }

        state.setState(PatternState.CheckState.VALID_UNCACHED);
        state.setFlipped(true);
        return state;
    }

    @Override
    public Long2ObjectMap<BlockInfo> getCache() {
        return cache;
    }

    @Override
    public boolean checkPatternAt(Level level, BlockPos centerPos, Direction frontFacing, Direction upwardsFacing, boolean isFlipped) {
        this.globalCount.clear();
        this.layerCount.clear();
        cache.clear();

        multiblockState.setLevel(level);

        BetterBlockPos controllerPos = new BetterBlockPos(centerPos);

        aisleStrategy.pattern = this;
        aisleStrategy.start(controllerPos, frontFacing, upwardsFacing);
        if(!aisleStrategy.check(isFlipped)) return false;

        for(Object2IntMap.Entry<SimplePredicate> entry : globalCount.object2IntEntrySet()) {
            if(entry.getIntValue() < entry.getKey().minCount) {
                state.setError(new SimplePredicateError(entry.getKey(), 1));
                return false;
            }
        }

        state.setError(null);
        return true;
    }

    public int getRepetitionCount(int aisleI) {
        return aisles[aisleI].actualRepeats;
    }

    @Override
    public Long2ObjectSortedMap<TraceabilityPredicate> getDefaultShape(MultiblockControllerMachine src, @NotNull Map<String, String> keyMap) {
        return null;
    }

    @Override
    public PatternState getPatternState() {
        return state;
    }

    public AisleStrategy getAisleStrategy() {
        return aisleStrategy;
    }

    @Override
    public OriginOffset getOffset() {
        return offset;
    }

    private BetterBlockPos startPos(BetterBlockPos controllerPos, Direction frontFacing, Direction upwardsFacing, boolean flip) {
        BetterBlockPos start = controllerPos.copy();
        offset.apply(start, frontFacing, upwardsFacing, flip);
        return start;
    }
}
