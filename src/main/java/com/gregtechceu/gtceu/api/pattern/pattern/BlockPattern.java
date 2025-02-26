package com.gregtechceu.gtceu.api.pattern.pattern;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.pattern.BetterBlockPos;
import com.gregtechceu.gtceu.api.pattern.MultiblockState;
import com.gregtechceu.gtceu.api.pattern.OriginOffset;
import com.gregtechceu.gtceu.api.pattern.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.pattern.error.PatternError;
import com.gregtechceu.gtceu.api.pattern.error.SinglePredicateError;
import com.gregtechceu.gtceu.api.pattern.predicates.SimplePredicate;
import com.gregtechceu.gtceu.api.pattern.util.BlockInfo;
import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import it.unimi.dsi.fastutil.chars.Char2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectSortedMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import lombok.Getter;
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
    @Getter
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
                state.setError(new SinglePredicateError(entry.getKey(), 1));
                return false;
            }
        }

        state.setError(null);
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
    public boolean checkAisle(BetterBlockPos controllerPos, Direction frontFacing, Direction upwardsFacing, int aisleIndex, int aisleOffset, boolean flip) {
        Direction absoluteAisle = directions[0].getRelativeFacing(frontFacing, upwardsFacing, flip);
        Direction absoluteString = directions[1].getRelativeFacing(frontFacing, upwardsFacing, flip);
        Direction absoluteChar = directions[2].getRelativeFacing(frontFacing, upwardsFacing, flip);

        BetterBlockPos aisleStart = startPos(controllerPos, frontFacing, upwardsFacing, flip).offset(absoluteAisle, aisleOffset);

        BetterBlockPos stringStart = aisleStart.copy();
        BetterBlockPos charPos = aisleStart.copy();
        PatternAisle aisle = aisles[aisleIndex];

        layerCount.clear();

        for(int stringI = 0; stringI < dimensions[1]; stringI++) {
            for(int charI = 0; charI < dimensions[2]; charI++) {
                multiblockState.setPos(charPos);
                TraceabilityPredicate pred = predicates.get(aisle.charAt(stringI, charI));

                if(pred != TraceabilityPredicate.ANY) {
                    BlockEntity be = multiblockState.getTileEntity();
                    cache.put(charPos.toLong(), new BlockInfo(multiblockState.getBlockState(), !(be instanceof IMachineBlockEntity mbe) ? be : null));
                }

                PatternError res = pred.test(multiblockState, globalCount, layerCount);
                if(res != null) {
                    state.setError(res);
                    return false;
                }

                charPos.offset(absoluteChar);
            }

            stringStart.offset(absoluteString);
            charPos.from(stringStart);
        }

        for(Object2IntMap.Entry<SimplePredicate> entry : layerCount.object2IntEntrySet()) {
            if(entry.getIntValue() < entry.getKey().minLayerCount) {
                state.setError(new SinglePredicateError(entry.getKey(), 3));
                return false;
            }
        }

        for(Object2IntMap.Entry<SimplePredicate> entry : layerCount.object2IntEntrySet()) {
            globalCount.put(entry.getKey(), globalCount.getInt(entry.getKey()) + entry.getIntValue());
        }

        return true;
    }

    public int getRepetitionCount(int aisleI) {
        return aisles[aisleI].actualRepeats;
    }

    @Override
    public Long2ObjectSortedMap<TraceabilityPredicate> getDefaultShape(MultiblockControllerMachine src, @NotNull Map<String, String> keyMap) {
        Long2ObjectSortedMap<TraceabilityPredicate> map = new Long2ObjectRBTreeMap<>();
        Direction absoluteAisle = directions[0].getRelativeFacing(src.getFrontFacing(), src.getUpwardsFacing());
        Direction absoluteString = directions[1].getRelativeFacing(src.getFrontFacing(), src.getUpwardsFacing());
        Direction absoluteChar = directions[2].getRelativeFacing(src.getFrontFacing(), src.getUpwardsFacing());

        BetterBlockPos pos = new BetterBlockPos(src.getPos());
        BetterBlockPos start = startPos(pos, src.getFrontFacing(), src.getUpwardsFacing(), false);
        BetterBlockPos serial = new BetterBlockPos().from(start);

        int[] order = aisleStrategy.getDefaultAisles(keyMap);
        for(int i = 0; i < order.length; i++) {
            for(int j = 0; j < dimensions[1]; j++) {
                for(int k = 0; k < dimensions[2]; k++) {
                    TraceabilityPredicate pred = predicates.get(aisles[order[i]].charAt(j, k));
                    if(pred != TraceabilityPredicate.ANY && pred != TraceabilityPredicate.AIR)
                        map.put(serial.toLong(), predicates.get(aisles[order[i]].charAt(j, k)));
                    serial.offset(absoluteChar);
                }
                serial.offset(absoluteString);
                serial.offset(absoluteChar.getOpposite(), dimensions[2]);
            }
            serial.from(start);
            serial.offset(absoluteAisle, i + 1);
        }

        return map;
    }

    @Override
    public PatternState getPatternState() {
        return state;
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
