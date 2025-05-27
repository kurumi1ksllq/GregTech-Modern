package com.gregtechceu.gtceu.common.machine.multiblock.primitive;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IWorkable;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.multiblock.BetterBlockPos;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.multiblock.error.PatternError;
import com.gregtechceu.gtceu.api.multiblock.pattern.FactoryExpandablePattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.item.tool.behavior.LighterBehavior;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireChargeItem;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.util.*;

public class CharcoalPileIgniterMachine extends WorkableMultiblockMachine implements IWorkable {

    private static final Set<Block> WALL_BLOCKS = new ObjectOpenHashSet<>();
    static {
        WALL_BLOCKS.add(Blocks.DIRT);
        WALL_BLOCKS.add(Blocks.COARSE_DIRT);
        WALL_BLOCKS.add(Blocks.PODZOL);
        WALL_BLOCKS.add(Blocks.GRASS_BLOCK);
        WALL_BLOCKS.add(Blocks.DIRT_PATH);
        WALL_BLOCKS.add(Blocks.SAND);
        WALL_BLOCKS.add(Blocks.RED_SAND);

    }

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            CharcoalPileIgniterMachine.class,
            WorkableMultiblockMachine.MANAGED_FIELD_HOLDER);

    private final Collection<BlockPos> logPos = new ObjectOpenHashSet<>();

    private static final int MIN_RADIUS = 1;
    private static final int MIN_DEPTH = 2;
    private static final int MAX_DEPTH = 5;
    private static final int MAX_RADIUS = 5;

    private final int[] bounds = new int[] {0, MIN_DEPTH, MIN_RADIUS, MIN_RADIUS, MIN_RADIUS, MIN_RADIUS};

    @DescSynced
    @RequireRerender
    private boolean isActive;
    private int progressTime = 0;
    private int maxTime = 0;
    private TickableSubscription burnLogsSubscription;

    public CharcoalPileIgniterMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public void formStructure(String name) {
        super.formStructure(name);
        forEachFormed(DEFAULT_STRUCTURE, (info, pos) -> {
            if(info.getBlockState().is(BlockTags.LOGS)) {
                logPos.add(pos.immutable());
            }
        });
        updateMaxProgessTime();
        burnLogsSubscription = subscribeServerTick(this::tick);
        tick();
    }

    @Override
    public void invalidateStructure(String name) {
        super.invalidateStructure(name);
        resetState();
        this.progressTime = 0;
        this.maxTime = 0;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onUnload() {
        super.onUnload();
        resetState();
    }

    private void resetState() {
        unsubscribe(burnLogsSubscription);
        isActive = false;
    }

    @Override
    public int getProgress() {
        return progressTime;
    }

    @Override
    public int getMaxProgress() {
        return maxTime;
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public boolean isWorkingEnabled() {
        return true;
    }

    @Override
    public IBlockPattern createStructurePattern() {
        var floor = Predicates.blocks(Blocks.BRICKS);
        var logs = logPredicate();
        var walls = wallPredicate();

        updateDimensions();

        return FactoryExpandablePattern.start(RelativeDirection.UP, RelativeDirection.RIGHT, RelativeDirection.FRONT)
                .boundsFunction((l, b, f, u) -> bounds)
                .predicateFunction((bp, b) -> {
                    if (bp.origin()) return Predicates.controller(Predicates.blocks(getDefinition().getBlock()));

                    int intersects = 0;

                    // aisle dir is up, so its bounds[0] and bounds[1]
                    // DOWN is negative
                    boolean topAisle = bp.x() == b[0];
                    boolean bottomAisle = bp.x() == -b[1];

                    if (topAisle || bottomAisle) intersects++;

                    // negative signs for the LEFT and BACK ordinals
                    // string dir is right, so its bounds[2] and bounds[3]
                    if (bp.y() == -b[2] || bp.y() == b[3]) intersects++;
                    // char dir is front, so its bounds[4] and bounds[5]
                    if (bp.z() == b[4] || bp.z() == -b[5]) intersects++;

                    if (intersects >= 2) return TraceabilityPredicate.ANY;

                    if (intersects == 1) {
                        if (bottomAisle) return floor;
                        return walls;
                    }
                    return logs;
                })
                .build();
    }

    private TraceabilityPredicate wallPredicate() {
        return new TraceabilityPredicate(
                multiblockState -> WALL_BLOCKS.contains(multiblockState.getBlockState().getBlock()) ?
                        null : PatternError.PLACEHOLDER,
                null);
    }

    private TraceabilityPredicate logPredicate() {
        return new TraceabilityPredicate(multiblockState -> {
            boolean match = multiblockState.getBlockState().is(BlockTags.LOGS_THAT_BURN);
            return match ? null : PatternError.PLACEHOLDER;
        }, null);
    }

    public void updateDimensions() {
        Level level = getLevel();
        if (level == null) return;
        Direction front = getFrontFacing();
        Direction back = front.getOpposite();
        Direction left = front.getCounterClockWise();
        Direction right = left.getOpposite();

        int l = findWallPos(left, new BetterBlockPos(getPos()).offset(Direction.DOWN));
        int r = findWallPos(right, new BetterBlockPos(getPos()).offset(Direction.DOWN));
        int b = findWallPos(back, new BetterBlockPos(getPos()).offset(Direction.DOWN));
        int f = findWallPos(front, new BetterBlockPos(getPos()).offset(Direction.DOWN));
        int d = findFloorPos(Direction.DOWN, new BetterBlockPos(getPos()));

        if(d <= 0 || l <= 0 || r <= 0 || b <= 0 || f <= 0) {
            invalidateStructure();
            return;
        }

        bounds[1] = d;
        bounds[2] = l;
        bounds[3] = r;
        bounds[4] = f;
        bounds[5] = b;
    }

    private int findWallPos(Direction direction, BetterBlockPos bp) {
        for (int i = 1; i <= MAX_RADIUS; i++) {
            if(WALL_BLOCKS.contains(getLevel().getBlockState(bp.offset(direction).immutable()).getBlock())) {
                return i;
            }
        }
        return -1;
    }

    private int findFloorPos(Direction direction, BetterBlockPos bp) {
        for (int i = 1; i <= MAX_RADIUS; i++) {
            if(getLevel().getBlockState(bp.offset(direction).immutable()).getBlock() == Blocks.BRICKS) {
                return i;
            }
        }
        return -1;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    private void updateMaxProgessTime() {
        this.maxTime = Math.max(1, (int) Math.sqrt(logPos.size() * 240_000));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        super.clientTick();
        if (isActive) {
            var pos = this.getPos();
            var facing = Direction.UP;
            float xPos = facing.getStepX() * 0.76F + pos.getX() + 0.25F + GTValues.RNG.nextFloat() / 2.0F;
            float yPos = facing.getStepY() * 0.76F + pos.getY() + 0.25F;
            float zPos = facing.getStepZ() * 0.76F + pos.getZ() + 0.25F + GTValues.RNG.nextFloat() / 2.0F;

            float ySpd = facing.getStepY() * 0.1F + 0.01F * GTValues.RNG.nextFloat();
            float horSpd = 0.03F * GTValues.RNG.nextFloat();
            float horSpd2 = 0.03F * GTValues.RNG.nextFloat();

            if (GTValues.RNG.nextFloat() < 0.1F) {
                getLevel().playLocalSound(xPos, yPos, zPos, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 1.0F,
                        1.0F, false);
            }
            for (float xi = xPos - 1; xi <= xPos + 1; xi++) {
                for (float zi = zPos - 1; zi <= zPos + 1; zi++) {
                    if (GTValues.RNG.nextFloat() < .9F)
                        continue;
                    getLevel().addParticle(ParticleTypes.LARGE_SMOKE, xi, yPos, zi, horSpd, ySpd, horSpd2);
                }
            }
        }
    }

    public void tick() {
        if (isActive && maxTime > 0) {
            if (++progressTime == maxTime) {
                progressTime = 0;
                maxTime = 0;
                convertLogBlocks();
                isActive = false;
            }
        }
    }

    private void convertLogBlocks() {
        Level level = getLevel();
        for (BlockPos pos : logPos) {
            level.setBlock(pos, GTBlocks.BRITTLE_CHARCOAL.getDefaultState(), Block.UPDATE_ALL);
        }
        logPos.clear();
    }

    @Override
    public InteractionResult onUse(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand,
                                   BlockHitResult hit) {
        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof IMachineBlockEntity machineBe) {
            MetaMachine mte = machineBe.getMetaMachine();
            if (mte instanceof CharcoalPileIgniterMachine cpi && cpi.isFormed()) {
                if (world.isClientSide) {
                    player.swing(hand);
                } else if (!cpi.isActive()) {
                    boolean shouldActivate = false;
                    ItemStack stack = player.getItemInHand(hand);
                    if (stack.getItem() instanceof FlintAndSteelItem) {
                        stack.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));
                        getLevel().playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

                        shouldActivate = true;
                    } else if (stack.getItem() instanceof FireChargeItem) {
                        stack.shrink(1);

                        getLevel().playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f, 1.0f);

                        shouldActivate = true;
                    } else if (stack.getItem() instanceof ComponentItem compItem) {
                        for (var component : compItem.getComponents()) {
                            if (component instanceof LighterBehavior lighter && lighter.consumeFuel(player, stack)) {
                                getLevel().playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0f,
                                        1.0f);

                                shouldActivate = true;
                                break;
                            }
                        }
                    }

                    if (shouldActivate) {
                        cpi.setActive(true);
                        return InteractionResult.CONSUME;
                    }
                }
            }
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }
}
