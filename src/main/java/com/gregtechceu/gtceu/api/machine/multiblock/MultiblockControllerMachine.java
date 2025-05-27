package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.api.block.IMachineBlock;
import com.gregtechceu.gtceu.api.block.MetaMachineBlock;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.IParallelHatch;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiController;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IMultiPart;
import com.gregtechceu.gtceu.api.multiblock.BetterBlockPos;
import com.gregtechceu.gtceu.api.multiblock.MultiblockWorldSavedData;
import com.gregtechceu.gtceu.api.multiblock.pattern.CurrentBlockInfo;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternState;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class MultiblockControllerMachine extends MetaMachine implements IMultiController {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            MultiblockControllerMachine.class, MetaMachine.MANAGED_FIELD_HOLDER);
    private CurrentBlockInfo controllerBlockInfo;
    private final List<IMultiPart> parts = new ArrayList<>();
    private @Nullable IParallelHatch parallelHatch = null;
    @Getter
    @DescSynced
    @UpdateListener(methodName = "onPartsUpdated")
    private BlockPos[] partPositions = new BlockPos[0];
    @Getter
    @Persisted
    @DescSynced
    @RequireRerender
    protected boolean isFormed;
    @Getter
    @Setter
    @Persisted
    @DescSynced
    protected boolean isFlipped;

    public static final String DEFAULT_STRUCTURE = "main";

    protected final Reference2ObjectMap<String, IBlockPattern> structures = new Reference2ObjectOpenHashMap<>();
    protected Reference2ObjectMap<String, PatternState> patternStates = new Reference2ObjectOpenHashMap<>();

    public MultiblockControllerMachine(IMachineBlockEntity holder) {
        super(holder);
        createStructurePatterns();
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public MultiblockMachineDefinition getDefinition() {
        return (MultiblockMachineDefinition) super.getDefinition();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            createStructurePatterns();
            MultiblockWorldSavedData.getOrCreate(serverLevel).addAsyncLogic(this);
        }
    }

    @Override
    public void onUnload() {
        super.onUnload();
        if (getLevel() instanceof ServerLevel serverLevel) {
            MultiblockWorldSavedData.getOrCreate(serverLevel).removeAsyncLogic(this);
        }
    }

    @NotNull
    public CurrentBlockInfo getBlockInfo() {
        if (controllerBlockInfo == null) {
            controllerBlockInfo = new CurrentBlockInfo();
            controllerBlockInfo.setLevel(getLevel());
            controllerBlockInfo.setCurrentPos(getPos());
        }
        return controllerBlockInfo;
    }

    @SuppressWarnings("unused")
    protected void onPartsUpdated(BlockPos[] newValue, BlockPos[] oldValue) {
        if (getLevel() == null) return;
        parts.clear();
        for (var pos : newValue) {
            if (getMachine(getLevel(), pos) instanceof IMultiPart part) {
                parts.add(part);
            }
        }
    }

    protected void updatePartPositions() {
        this.partPositions = this.parts.isEmpty() ? new BlockPos[0] :
                this.parts.stream().map(part -> part.self().getPos()).toArray(BlockPos[]::new);
    }

    @Override
    public List<IMultiPart> getParts() {
        // for the client side, when the chunk unloaded
        if (parts.size() != this.partPositions.length) {
            parts.clear();
            for (var pos : this.partPositions) {
                if (getMachine(getLevel(), pos) instanceof IMultiPart part) {
                    parts.add(part);
                }
            }
        }
        return this.parts;
    }

    @Override
    public Optional<IParallelHatch> getParallelHatch() {
        return Optional.ofNullable(parallelHatch);
    }

    //////////////////////////////////////
    // *** Multiblock LifeCycle ***//
    //////////////////////////////////////
    @Getter
    private final Lock patternLock = new ReentrantLock();

    @Override
    public void asyncCheckPattern(long periodID) {
        for(var entry : patternStates.entrySet()) {
            var name = entry.getKey();
            var patternState = entry.getValue();
            if ((patternState.hasError() || !isFormed) && (getHolder().getOffset() + periodID) % 4 == 0 &&
                    checkPatternWithTryLock(name)) { // per second
                if (getLevel() instanceof ServerLevel serverLevel) {
                    serverLevel.getServer().execute(() -> {
                        patternLock.lock();
                        if (checkPatternWithLock(name)) { // formed
                            formStructure(name);
                            //checkAndFormStructurePatterns();
                            var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
                            //var state = structure.getPatternState();
                            mwsd.addMapping(getBlockInfo(), patternState);
                            //for(var state : patternStates.values()) mwsd.addMapping(getBlockInfo(), state);
                            mwsd.removeAsyncLogic(this);
                        }
                        patternLock.unlock();
                    });
                }
            }
        }
    }

    public void createStructurePatterns() {
        var defaultPattern = createStructurePattern();
        var defaultPatternState = new PatternState();
        patternStates.put(DEFAULT_STRUCTURE, defaultPatternState);
        defaultPattern.setActivePatternState(defaultPatternState);
        structures.put(DEFAULT_STRUCTURE, defaultPattern);
    }

    public void checkAndFormStructurePatterns() {
        for(String name : structures.keySet()) {
            formStructure(name);
        }
    }

    public PatternState getDefaultPatternState() {
        return patternStates.get(DEFAULT_STRUCTURE);
        //return structures.get(DEFAULT_STRUCTURE).getPatternState();
    }

    public PatternState getPatternState(String name) {
        return patternStates.get(name);
        //return structures.get(name).getPatternState();
    }

    public PatternState checkStructurePattern() {
        return checkStructurePattern(DEFAULT_STRUCTURE);
    }

    public PatternState checkStructurePattern(String name) {
        IBlockPattern pattern = getSubstructure(name);
        var pState = patternStates.get(name);
        if(!pState.shouldUpdate() || getLevel() == null) return pState;

        long time = System.nanoTime();
        pState.setController(this, getPos());
        pattern.setActivePatternState(pState);
        pattern.checkPatternFastAt(getLevel(), getPos(), getFrontFacing(), getUpwardsFacing(), allowFlip());
        //GTCEu.LOGGER.info("Structure check for {} took {} ns", self().getDefinition().getName(), (System.nanoTime() - time));

        return pState;
    }

    @Override
    public void formStructure(String name) {
        var patternState = getPatternState(name);
        patternState.setFormed(true);

        if(patternState.getState().isValid()) {
            if(patternState.isFormed()) {
                if(patternState.getState() == PatternState.CheckState.VALID_UNCACHED) {
                    forEachMultiPart(name, part -> {
                        if(parts.contains(part)) return true;

                        if(part.hasController(getPos()) && !part.canShared(this, name)) {
                            invalidateStructure(name);
                            return false;
                        }
                        return true;
                    });

                    forEachMultiPart(name, part -> {
                        if(parts.contains(part)) return true;

                        if(shouldAddPartToController(part)) {
                            this.parts.add(part);
                        }
                        return true;
                    });

                    this.parts.sort(getDefinition().getPartSorter());
                    for (var part : parts) {
                        if (part instanceof IParallelHatch pHatch) {
                            parallelHatch = pHatch;
                        }
                        part.addedToController(this, name);
                    }
                    updatePartPositions();

                    patternState.setFormed(true);
                    setFlipped(patternState.isFlipped(), patternState);
                }
                return;
            }

            boolean[] valid = new boolean[1];
            valid[0] = true;

            forEachMultiPart(name, part -> {
                if(part.hasController(getPos()) && !part.canShared(this, name)) {
                    valid[0] = false;
                    return false;
                }
                return true;
            });

            if(!valid[0]) return;

            patternState.setFormed(true);
            isFormed = true;
            setFlipped(patternState.isFlipped(), patternState);

        } else {
            if(patternState.isFormed()) {
                invalidateStructure(name);
            }
        }
    }

    public void setFlipped(boolean flipped, PatternState state) {
        boolean flip = state.isActualFlipped();
        if(flip != flipped) {
            state.setActualFlipped(flipped);
            this.isFlipped = flipped;
            notifyBlockUpdate();
            markDirty();
        }
    }

    public void invalidateStructure() {
        invalidateStructure(DEFAULT_STRUCTURE);
        isFormed = false;
    }

    @Override
    public void invalidateStructure(String name) {
        //patternStates.clear();
        if(!patternStates.get(name).isFormed()) return;

        parts.removeIf(part -> {
            if(name.equals(part.getSubstructureName())) {
                part.removedFromController(this);
                return true;
            }
            return false;
        });
        patternStates.get(name).setFormed(false);
        isFormed = false;
        parallelHatch = null;
        updatePartPositions();
    }

    protected void invalidStructureCaches() {
        for(var pState : patternStates.values()) {
            pState.getPosCache().clear();
        }
    }

    /*@Override
    public void onStructureFormed(String name) {
        isFormed = true;
        this.parts.clear();
        Set<IMultiPart> set = getMultiblockState().getMatchContext().getOrCreate("parts", Collections::emptySet);
        for (IMultiPart part : set) {
            if (shouldAddPartToController(part)) {
                this.parts.add(part);
            }
        }
        this.parts.sort(getDefinition().getPartSorter());
        for (var part : parts) {
            if (part instanceof IParallelHatch pHatch) {
                parallelHatch = pHatch;
            }
            //part.addedToController(this);
        }
        updatePartPositions();
    }

    @Override
    public void onStructureInvalid(String name) {
        isFormed = false;
        for (IMultiPart part : parts) {
            part.removedFromController(this);
        }
        parallelHatch = null;
        parts.clear();
        updatePartPositions();
    }*/

    public IBlockPattern getSubstructure(String name) {
        return structures.get(name);
    }

    void forEachMultiPart(String name, Predicate<IMultiPart> action) {
        var cache = getSubstructure(name).getCache();
        for(BlockInfo info : cache.values()) {
            BlockEntity be = info.getBlockEntity();
            if(be instanceof MetaMachineBlockEntity mmbe) {
                var machine = mmbe.metaMachine;
                if (machine instanceof IMultiPart part) {
                    if (!action.test(part)) return;
                }
            }
        }
    }

    protected void forEachFormed(String name, BiConsumer<BlockInfo, BetterBlockPos> action) {
        var cache = getSubstructure(name).getCache();
        BetterBlockPos pos = new BetterBlockPos();
        for(var entry : cache.long2ObjectEntrySet()) {
            action.accept(entry.getValue(), pos.fromLong(entry.getLongKey()));
        }
    }

    /**
     * mark multiblockState as unload error first.
     * if it's actually cuz by block breaking.
     * {@link #//onStructureInvalid(String)} will be called from
     * {@link #//onBlockStateChanged(BlockPos, BlockState)}
     */
    @Override
    public void onPartUnload() {
        /*parts.removeIf(part -> part.self().isInValid());
        getMultiblockState().setError(MultiblockState.UNLOAD_ERROR);
        if (getLevel() instanceof ServerLevel serverLevel) {
            MultiblockWorldSavedData.getOrCreate(serverLevel).addAsyncLogic(this);
        }
        updatePartPositions();*/
    }

    @Override
    public void onRotated(Direction oldFacing, Direction newFacing) {
        if (oldFacing != newFacing && getLevel() instanceof ServerLevel serverLevel) {
            // invalid structure
            //this.onStructureInvalid();
            invalidStructureCaches();
            var mwsd = MultiblockWorldSavedData.getOrCreate(serverLevel);
            for(var structure : structures.values()) {
                var state = structure.getPatternState();
                mwsd.removeMapping(state);
            }
            mwsd.addAsyncLogic(this);
        }
    }

    public boolean allowFlip() {
        return getDefinition().isAllowFlip();
    }

    @Override
    public void setUpwardsFacing(@NotNull Direction upwardsFacing) {
        if (getLevel() == null) return;
        if (!getDefinition().isAllowExtendedFacing()) return;
        BlockState blockState = getBlockState();
        if (blockState.getBlock() instanceof MetaMachineBlock &&
                blockState.getValue(IMachineBlock.UPWARDS_FACING_PROPERTY) != upwardsFacing) {
            getLevel().setBlockAndUpdate(getPos(),
                    blockState.setValue(IMachineBlock.UPWARDS_FACING_PROPERTY, upwardsFacing));
            if (getLevel() != null && !getLevel().isClientSide) {
                notifyBlockUpdate();
                markDirty();

                invalidStructureCaches();
                checkAndFormStructurePatterns();
            }
        }
    }

    @Override
    protected InteractionResult onWrenchClick(Player playerIn, InteractionHand hand, Direction gridSide,
                                              BlockHitResult hitResult) {
        if (gridSide == getFrontFacing() && allowExtendedFacing()) {
            var newUp = getUpwardsFacing().getClockWise(getFrontFacing().getAxis());
            if(playerIn.isShiftKeyDown()) newUp = newUp.getOpposite();
            setUpwardsFacing(newUp);
            playerIn.swing(hand);
            return InteractionResult.CONSUME;
        }
        if (playerIn.isShiftKeyDown()) {
            if (gridSide == getFrontFacing() || !isFacingValid(gridSide)) {
                return InteractionResult.FAIL;
            }
            if (!isRemote()) {
                setFrontFacing(gridSide);
            }
            playerIn.swing(hand);
            return InteractionResult.CONSUME;
        }
        return super.onWrenchClick(playerIn, hand, gridSide, hitResult);
    }

    @Override
    public void setFrontFacing(Direction facing) {
        super.setFrontFacing(facing);

        if (getLevel() != null && !getLevel().isClientSide) {
            invalidStructureCaches();
            checkAndFormStructurePatterns();
        }
    }
}
