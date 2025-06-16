package com.gregtechceu.gtceu.common.machine.multiblock.primitive;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IWorkable;
import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableMultiblockMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.multiblock.Predicates;
import com.gregtechceu.gtceu.api.multiblock.TraceabilityPredicate;
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError;
import com.gregtechceu.gtceu.api.multiblock.pattern.FactoryExpandablePattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.IBlockPattern;
import com.gregtechceu.gtceu.api.multiblock.pattern.PatternState;
import com.gregtechceu.gtceu.api.multiblock.util.BlockInfo;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.item.tool.behavior.LighterBehavior;

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

    private final int[] bounds = new int[] { 0, MIN_DEPTH, MIN_RADIUS, MIN_RADIUS, MIN_RADIUS, MIN_RADIUS };
    private int maxTime = 0;
    private boolean hasAir = false;

    public CharcoalPileIgniterMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    @Override
    public void formStructure(String name) {
        super.formStructure(name);
        hasAir = false;
        forEachFormed(DEFAULT_STRUCTURE, (info, pos) -> {
            if (info.getBlockState().is(BlockTags.LOGS)) {
                logPos.add(pos.immutable());
            } else if(info.getBlockState().isAir()) {
                hasAir = true;
            }
        });
        this.getRecipeLogic().setDuration(Math.max(1, (int) Math.sqrt(logPos.size() * 240_000)));
    }

    @Override
    protected CharcoalRecipeLogic createRecipeLogic(Object... args) {
        return new CharcoalRecipeLogic(this);
    }

    @Override
    public CharcoalRecipeLogic getRecipeLogic() {
        return (CharcoalRecipeLogic) super.getRecipeLogic();
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean isActive() {
        return recipeLogic.isWorking();
    }

    @Override
    public boolean isWorkingEnabled() {
        return true;
    }

    @Override
    public PatternState checkStructurePattern(String name) {
        createStructurePattern();
        return super.checkStructurePattern(name);
    }

    @Override
    public IBlockPattern createStructurePattern() {
        var floor = Predicates.blocks(Blocks.BRICKS);
        var logs = TraceabilityPredicate.AIR.or(logPredicate());
        var walls = wallPredicate();

        updateDimensions();

        return FactoryExpandablePattern.start(RelativeDirection.UP, RelativeDirection.RIGHT, RelativeDirection.FRONT)
                .boundsFunction((l, b, f, u) -> bounds)
                .predicateFunction((bp, b) -> {
                    if (bp.equals(BlockPos.ZERO))
                        return Predicates.controller(Predicates.blocks(getDefinition().getBlock()));

                    int intersects = 0;

                    // aisle dir is up, so its bounds[0] and bounds[1]
                    // DOWN is negative
                    boolean topAisle = bp.getX() == b[0];
                    boolean bottomAisle = bp.getX() == -b[1];

                    if (topAisle || bottomAisle) intersects++;

                    // negative signs for the LEFT and BACK ordinals
                    // string dir is right, so its bounds[2] and bounds[3]
                    if (bp.getY() == -b[2] || bp.getY() == b[3]) intersects++;
                    // char dir is front, so its bounds[4] and bounds[5]
                    if (bp.getZ() == b[4] || bp.getZ() == -b[5]) intersects++;

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
                        null : new PatternStringError("gtceu.predicate_error.charcoal.walls"),
                map -> WALL_BLOCKS.stream()
                        .map(block -> BlockInfo.fromBlockState(block.defaultBlockState()))
                        .toArray(BlockInfo[]::new));
    }

    private TraceabilityPredicate logPredicate() {
        return new TraceabilityPredicate(multiblockState -> {
            boolean match = multiblockState.getBlockState().is(BlockTags.LOGS_THAT_BURN);
            return match ? null : new PatternStringError("gtceu.predicate_error.charcoal.logs");
        }, null);
    }

    public void updateDimensions() {
        Level level = getLevel();
        if (level == null) return;
        Direction front = getFrontFacing();
        Direction back = front.getOpposite();
        Direction left = front.getCounterClockWise();
        Direction right = left.getOpposite();

        int l = findWallPos(left, getPos().mutable().move(Direction.DOWN));
        int r = findWallPos(right, getPos().mutable().move(Direction.DOWN));
        int b = findWallPos(back, getPos().mutable().move(Direction.DOWN));
        int f = findWallPos(front, getPos().mutable().move(Direction.DOWN));
        int d = findFloorPos(getPos().mutable());

        if (d < MIN_DEPTH || l < MIN_RADIUS || r < MIN_RADIUS || b < MIN_RADIUS || f < MIN_RADIUS) {
            invalidateStructure();
            return;
        }

        bounds[1] = d;
        bounds[2] = l;
        bounds[3] = r;
        bounds[4] = f;
        bounds[5] = b;
    }

    private int findWallPos(Direction direction, BlockPos.MutableBlockPos bp) {
        for (int i = 1; i <= MAX_RADIUS; i++) {
            var block = getLevel().getBlockState(bp.move(direction)).getBlock();
            if (WALL_BLOCKS.contains(block)) {
                return i;
            }
        }
        return -1;
    }

    private int findFloorPos(BlockPos.MutableBlockPos bp) {
        for (int i = 1; i <= MAX_DEPTH; i++) {
            var block = getLevel().getBlockState(bp.move(Direction.DOWN)).getBlock();
            if (block == Blocks.BRICKS) {
                return i;
            }
        }
        return -1;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void clientTick() {
        super.clientTick();
        if (isActive()) {
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
        if (!(be instanceof IMachineBlockEntity machineBe)) return super.onUse(state, world, pos, player, hand, hit);
        MetaMachine mte = machineBe.getMetaMachine();

        if (mte instanceof CharcoalPileIgniterMachine cpi && cpi.isFormed() && !hasAir) {
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
                    cpi.getRecipeLogic().setStatus(RecipeLogic.Status.WORKING);
                    return InteractionResult.CONSUME;
                }
            }
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    public class CharcoalRecipeLogic extends RecipeLogic {

        private CharcoalPileIgniterMachine machine;

        public CharcoalRecipeLogic(CharcoalPileIgniterMachine machine) {
            super(machine);
        }

        @Override
        public void serverTick() {
            super.serverTick();
            if (isWorking() && duration > 0) {
                if (++progress == duration) {
                    progress = 0;
                    duration = 0;
                    machine.convertLogBlocks();
                    setStatus(Status.IDLE);
                }
            }
        }

        public void setDuration(int max) {
            this.duration = max;
        }
    }
}
