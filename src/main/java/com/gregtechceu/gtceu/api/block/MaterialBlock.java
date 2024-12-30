package com.gregtechceu.gtceu.api.block;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.item.PipeBlockItem;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.client.renderer.block.MaterialBlockRenderer;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.recipe.VanillaRecipeHelper;

import com.lowdragmc.lowdraglib.Platform;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * MaterialBlock is a class that provides methods to get the properties of Material Blocks.
 * This takes care of the appearances depending on the GTCEU Material.
 * @see AppearanceBlock
 * @see Material
 */
@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class MaterialBlock extends AppearanceBlock {
    public final TagPrefix tagPrefix;
    public final Material material;

    /**
     * This is the constructor for the MaterialBlock class.
     * @param properties the properties of the block
     * @param tagPrefix the tag prefix of the block
     * @param material the material of the block
     * @param registerModel whether to register the model of the block
     */
    public MaterialBlock(Properties properties, TagPrefix tagPrefix, Material material, boolean registerModel) {
        super(properties);
        this.material = material;
        this.tagPrefix = tagPrefix;
        if (registerModel && Platform.isClient()) {
            MaterialBlockRenderer.create(this, tagPrefix.materialIconType(), material.getMaterialIconSet());
        }
    }

    /**
     * This is the constructor for the MaterialBlock class when registerModel is ignored.
     * @param properties the properties of the block
     * @param tagPrefix the tag prefix of the block
     * @param material the material of the block
     */
    public MaterialBlock(Properties properties, TagPrefix tagPrefix, Material material) {
        this(properties, tagPrefix, material, true);
    }

    /**
     * This is the function that changes the color of the block.
     * @implNote This is only used on the client side.
     * @return the tinted color of the block
     */
    @OnlyIn(Dist.CLIENT)
    public static BlockColor tintedColor() {
        return (state, reader, pos, tintIndex) -> {
            if (!(state.getBlock() instanceof MaterialBlock block)) return -1;
            return block.material.getLayerARGB(tintIndex);
        };
    }

    // TODO: Rename to DEFAULT_FRAME_COLLISION_BOX
    public static VoxelShape FRAME_COLLISION_BOX = Shapes.box(0.05, 0.0, 0.05, 0.95, 1.0, 0.95);

    /**
     * This is the function that gets the collision shape of the block.
     * @param state the {@link BlockState} of the block
     * @param level the {@link BlockGetter} of the block
     * @param pos the {@link BlockPos} of the block
     * @param context the {@link CollisionContext} of the block
     * @return the {@link VoxelShape} of the block
     */
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (this.tagPrefix == TagPrefix.frameGt) {
            return FRAME_COLLISION_BOX;
        }
        return super.getCollisionShape(state, level, pos, context);
    }

    /**
     * This is the function that handles the block when placed.
     * @implNote This is suppressed because the method is deprecated.
     * @param state the {@link BlockState} of the block
     * @param level the {@link Level} of the block
     * @param pos the {@link BlockPos} of the block
     * @param oldState the {@link BlockState} of the old block
     * @param isMoving whether the block is moving
     */
    @SuppressWarnings("deprecation")
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (TagPrefix.ORES.containsKey(this.tagPrefix) && TagPrefix.ORES.get(tagPrefix).isSand() &&
                ConfigHolder.INSTANCE.worldgen.sandOresFall) {
            level.scheduleTick(pos, this, this.getDelayAfterPlace());
        }
    }

    /**
     * This is the function that updates the shape of the block.
     * @param state the {@link BlockState} of the block
     * @param direction the {@link Direction} of the block
     * @param neighborState the {@link BlockState} of the neighbor block
     * @param level the {@link LevelAccessor} of the block
     * @param currentPos the {@link BlockPos} of the current block
     * @param neighborPos the {@link BlockPos} of the neighbor block
     * @return the {@link BlockState} of the block
     */
    @SuppressWarnings("deprecation")
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
                                  BlockPos currentPos, BlockPos neighborPos) {
        // TODO: Simplify conditions and return early
        if (TagPrefix.ORES.containsKey(this.tagPrefix) && TagPrefix.ORES.get(tagPrefix).isSand() &&
                ConfigHolder.INSTANCE.worldgen.sandOresFall) {
            level.scheduleTick(currentPos, this, this.getDelayAfterPlace());
        }
        return super.updateShape(state, direction, neighborState, level, currentPos, neighborPos);
    }

    /**
     * This is the function that updates the block with game ticks.
     * @param state the {@link BlockState} of the block
     * @param level the {@link ServerLevel} of the block
     * @param pos the {@link BlockPos} of the block
     * @param random the {@link RandomSource} of the block
     */
    @SuppressWarnings("deprecation")
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        // TODO: Simplify condition
        if (!FallingBlock.isFree(level.getBlockState(pos.below())) || pos.getY() < level.getMinBuildHeight()) {
            return;
        }
        FallingBlockEntity.fall(level, pos, state);
    }

    /**
     * This is the function that animates the block with game ticks.
     * @param state the {@link BlockState} of the block
     * @param level the {@link Level} of the block
     * @param pos the {@link BlockPos} of the block
     * @param random the {@link RandomSource} of the block
     */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // TODO: Simplify condition
        if (!TagPrefix.ORES.containsKey(this.tagPrefix) || !TagPrefix.ORES.get(tagPrefix).isSand() ||
                !ConfigHolder.INSTANCE.worldgen.sandOresFall)
            return;
        if (random.nextInt(16) == 0 && FallingBlock.isFree(level.getBlockState(pos.below()))) {
            double d = (double) pos.getX() + random.nextDouble();
            double e = (double) pos.getY() - 0.05;
            double f = (double) pos.getZ() + random.nextDouble();
            level.addParticle(new BlockParticleOption(ParticleTypes.FALLING_DUST, state), d, e, f, 0.0, 0.0, 0.0);
        }
    }

    /**
     * Gets the amount of time in ticks this block will wait before attempting to start falling.
     * @return the delay after the block is placed
     */
    protected int getDelayAfterPlace() {
        return 2;
    }

    /**
     * This is the function that gets the description ID of the block.
     * @return the unlocalized name of the block
     */
    @Override
    public String getDescriptionId() {
        return tagPrefix.getUnlocalizedName(material);
    }

    /**
     * This is the function that gets the name of the block.
     * @return the localized name of the block
     */
    @Override
    public MutableComponent getName() {
        return tagPrefix.getLocalizedName(material);
    }

    /**
     * This is the function determines how the block is used.
     * @return the result of the interaction
     */
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hit) {
        if (this.tagPrefix != TagPrefix.frameGt) {
            return super.use(state, level, pos, player, hand, hit);
        }
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty())
            return InteractionResult.PASS;

        if (stack.getItem() instanceof PipeBlockItem) {
            return replaceWithFramedPipe(level, pos, state, player, stack, hit) ? InteractionResult.SUCCESS :
                    InteractionResult.PASS;
        }

        Set<GTToolType> types = ToolHelper.getToolTypes(stack);
        if (!types.isEmpty() && ToolHelper.canUse(stack) && types.contains(GTToolType.CROWBAR)) {
            return removeFrame(level, pos, player, stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        var frameBlock = getFrameboxFromItem(stack);
        if (frameBlock == null) return InteractionResult.PASS;

        BlockPos.MutableBlockPos blockPos = pos.mutable();
        for (int i = 0; i < 32; i++) {
            if (level.getBlockState(blockPos).getBlock() instanceof MaterialBlock matBlock &&
                    matBlock.tagPrefix == TagPrefix.frameGt) {
                blockPos.move(Direction.UP);
                continue;
            }
            BlockEntity te = level.getBlockEntity(blockPos);
            if (te instanceof PipeBlockEntity<?, ?> pbe && pbe.getFrameMaterial() != null) {
                blockPos.move(Direction.UP);
                continue;
            }
            if (canSupportRigidBlock(level, blockPos.below())) {
                level.setBlock(blockPos, frameBlock.defaultBlockState(), Block.UPDATE_ALL);
                if (!player.isCreative())
                    stack.shrink(1);
                return InteractionResult.SUCCESS;
            } else if (te instanceof PipeBlockEntity<?, ?> pbe && pbe.getFrameMaterial() == null) {
                pbe.setFrameMaterial(frameBlock.material);

                if (!player.isCreative())
                    stack.shrink(1);
                return InteractionResult.SUCCESS;
            } else {
                return InteractionResult.PASS;
            }
        }

        return InteractionResult.PASS;
    }

    /**
     * TODO: Tbh I actually don't know what this does enough to describe it
     * @param stack the {@link ItemStack} of the block
     * @return the {@link MaterialBlock} of the item if it is a frame box, otherwise {@code null}
     */
    @Nullable
    public static MaterialBlock getFrameboxFromItem(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof BlockItem ib) {
            Block block = ib.getBlock();
            if (block instanceof MaterialBlock matBlock)
                return matBlock.tagPrefix == TagPrefix.frameGt ? matBlock : null;
        }
        return null;
    }

    /**
     * This is the function that determines if a frame block is removed from the world.
     * @param level the {@link Level} of the block
     * @param pos the {@link BlockPos} of the block
     * @param player the {@link Player} of the block
     * @param stack the {@link ItemStack} of the block
     * @return whether the frame block was removed
     */
    public boolean removeFrame(Level level, BlockPos pos, Player player, ItemStack stack) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof PipeBlockEntity<?, ?> pipeTile) {
            Material mat = pipeTile.getFrameMaterial();
            if (mat != null) {
                pipeTile.setFrameMaterial(null);
                Block.popResource(level, pos, this.asItem().getDefaultInstance());
                ToolHelper.damageItem(stack, player);
                ToolHelper.playToolSound(GTToolType.CROWBAR, (ServerPlayer) player);
                return true;
            }
        }
        return false;
    }

    /**
     * This is the function that determines if a block can be replaced in-place.
     * @param state the {@link BlockState} of the block
     * @param useContext the {@link BlockPlaceContext} of the block
     * @return whether the block can be replaced
     */
    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        if (this.tagPrefix == TagPrefix.frameGt && useContext.getItemInHand().getItem() instanceof PipeBlockItem &&
                !useContext.getPlayer().isCrouching())
            return true;
        return super.canBeReplaced(state, useContext);
    }

    /**
     * This is the function that determines if a block can be replaced with a framed pipe?
     * @param level the {@link Level} of the block
     * @param pos the {@link BlockPos} of the block
     * @param state the {@link BlockState} of the block
     * @param player the {@link Player} of the block
     * @param stackInHand the {@link ItemStack} of the block
     * @param hit the {@link BlockHitResult} of the block
     * @return whether the block can be replaced with a framed pipe
     */
    public boolean replaceWithFramedPipe(Level level, BlockPos pos, BlockState state, Player player,
                                         ItemStack stackInHand, BlockHitResult hit) {
        PipeBlock<?, ?, ?> pipeBlock = (PipeBlock<?, ?, ?>) ((PipeBlockItem) stackInHand.getItem()).getBlock();
        if (pipeBlock.pipeType.getThickness() < 1) {
            PipeBlockItem itemBlock = (PipeBlockItem) stackInHand.getItem();
            BlockState pipeState = pipeBlock.defaultBlockState();
            BlockPlaceContext context = new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, stackInHand,
                    hit);
            BlockState original = level.getBlockState(context.getClickedPos());
            itemBlock.placeBlock(context, pipeState);
            var pipeTile = pipeBlock.getPipeTile(level, pos);
            if (pipeTile instanceof PipeBlockEntity<?, ?> pipeBlockEntity) {
                pipeBlockEntity.setFrameMaterial(material);
            } else {
                // reset the state if we didn't place correctly
                level.setBlockAndUpdate(context.getClickedPos(), original);
                return false;
            }

            SoundType type = VanillaRecipeHelper.isMaterialWood(pipeTile.getFrameMaterial()) ? SoundType.WOOD :
                    SoundType.METAL;
            level.playSound(player, pos,
                    type.getPlaceSound(), SoundSource.BLOCKS,
                    (type.getVolume() + 1.0F) / 2.0F, type.getPitch() * 0.8F);
            if (!player.isCreative())
                stackInHand.shrink(1);
            return true;
        }
        return false;
    }

    /**
     * TODO: Tbh I actually don't know what this does enough to describe it
     * @param state the {@link BlockState} of the block
     * @param level the {@link Level} of the block
     * @param pos the {@link BlockPos} of the block
     * @param entity the {@link Entity} of the block
     */
    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (this.tagPrefix == TagPrefix.frameGt && entity instanceof LivingEntity livingEntity) {
            double currentAccel = 0.15D * (livingEntity.getDeltaMovement().y < 0.3D ? 2.5D : 1.0D);
            double currentSpeedVertical = 0.9D * (livingEntity.isInWater() ? 0.4D : 1.0D);
            Vec3 deltaMovement = livingEntity.getDeltaMovement();
            livingEntity.resetFallDistance();
            float f = 0.15F;
            double d0 = Mth.clamp(deltaMovement.x, -f, f);
            double d1 = Mth.clamp(deltaMovement.z, -f, f);
            double d2 = Math.max(deltaMovement.y, -f);
            if (d2 < 0.0 && !livingEntity.getFeetBlockState().isScaffolding(livingEntity) &&
                    livingEntity.isSuppressingSlidingDownLadder() &&
                    livingEntity instanceof Player) {
                d2 = Math.min(deltaMovement.y + currentAccel, 0.0D);
            }
            if (livingEntity.horizontalCollision) {
                d2 = 0.3;
            }
            deltaMovement = new Vec3(d0, d2, d1);
            entity.setDeltaMovement(deltaMovement);
        }
    }
}
