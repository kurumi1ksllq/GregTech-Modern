package com.gregtechceu.gtceu.api.pipenet;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.block.MaterialPipeBlock;
import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.api.blockentity.*;
import com.gregtechceu.gtceu.api.blockentity.IGregtechBlockEntity;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.IToolable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.IToolGridHighlight;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.pipenet.*;
import com.gregtechceu.gtceu.api.sync_system.ManagedSyncBlockEntity;
import com.gregtechceu.gtceu.api.sync_system.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import com.mojang.datafixers.util.Pair;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class PipeBlockEntity<PipeType extends Enum<PipeType> & IPipeType<NodeDataType>, NodeDataType>
                                     extends ManagedSyncBlockEntity
                                     implements IGregtechBlockEntity, IToolGridHighlight, ITickSubscription, IToolable, ICopyable, IPaintable, IDebugOverlayTextSupplier {

    private final long offset = GTValues.RNG.nextInt(20);

    public static final int ALL_OPENED = 0b111111;
    public static final int ALL_CLOSED = 0b000000;

    @Getter
    @SyncToClient
    @SaveField(nbtKey = "cover")
    protected final PipeCoverContainer coverContainer;

    @Getter
    @SyncToClient
    @SaveField
    @RerenderOnChanged
    protected int connections = ALL_CLOSED;
    @SyncToClient
    @SaveField
    @RerenderOnChanged
    private int blockedConnections = ALL_CLOSED;
    private NodeDataType cachedNodeData;

    @SaveField
    @SyncToClient
    @RerenderOnChanged
    @Getter
    private int paintingColor = -1;

    @RerenderOnChanged
    @SyncToClient
    @SaveField
    @NotNull
    private Material frameMaterial = GTMaterials.NULL;

    private boolean attachedToNet = false;

    private final List<TickableSubscription> serverTicks;
    private final List<TickableSubscription> waitingToAdd;
    @Getter
    private final PipeNetworkType networkType;

    public PipeBlockEntity(BlockEntityType<?> type, PipeNetworkType networkType, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
        this.coverContainer = new PipeCoverContainer(this);
        this.networkType = networkType;
        this.serverTicks = new ArrayList<>();
        this.waitingToAdd = new ArrayList<>();
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        coverContainer.onUnload();
        if (!isRemote()) {
            var lvl = getLevel();
            attachedToNet = false;
            if (lvl instanceof ServerLevel serverLevel) {
                LevelPipeNet.getLevelPipeNet(serverLevel, networkType).removeNode(getBlockPos());
            }
        }
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    @Override
    public long getOffsetTimer() {
        return level == null ? offset : (level.getServer().getTickCount() + offset);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        coverContainer.onLoad();
    }

    public void setConnections(int connections) {
        this.connections = connections;
        syncDataHolder.markClientSyncFieldDirty("connections");
    }

    public void setBlockedConnections(int blocked) {
        this.blockedConnections = blocked;
        syncDataHolder.markClientSyncFieldDirty("blockedConnections");
    }

    public void setPaintingColor(int col) {
        paintingColor = col;
        syncDataHolder.markClientSyncFieldDirty("paintingColor");
    }

    public void setFrameMaterial(Material mat) {
        frameMaterial = mat;
        syncDataHolder.markClientSyncFieldDirty("frameMaterial");
    }

    public int getNumConnections() {
        int count = 0;
        int connections = getConnections();
        while (connections > 0) {
            count++;
            connections = connections & (connections - 1);
        }
        return count;
    }

    public @NotNull Material getFrameMaterial() {
        // backwards compat
        // noinspection ConstantValue
        if (frameMaterial == null) {
            frameMaterial = GTMaterials.NULL;
        }
        return frameMaterial;
    }

    /**
     * If pipe is set to block connection from the specific side
     *
     * @param side face
     */
    public boolean isBlocked(Direction side) {
        return PipeBlockEntity.isFaceBlocked(getBlockedConnections(), side);
    }

    public int getBlockedConnections() {
        return canHaveBlockedFaces() ? blockedConnections : 0;
    }

    public boolean isConnected(Direction side) {
        return PipeBlockEntity.isConnected(getConnections(), side);
    }

    // if a face is blocked it will still render as connected, but it won't be able to receive stuff from that direction
    public boolean canHaveBlockedFaces() {
        return true;
    }

    @SuppressWarnings("unchecked")
    public PipeBlock<PipeType, NodeDataType> getPipeBlock() {
        return (PipeBlock<PipeType, NodeDataType>) getBlockState().getBlock();
    }

    @Nullable
    public PipeNet getPipeNet() {
        if (getLevel() instanceof ServerLevel serverLevel) {
            return LevelPipeNet.getLevelPipeNet(serverLevel, networkType).getNetFromPos(getBlockPos());
        }
        return null;
    }

    public PipeType getPipeType() {
        return getPipeBlock().pipeType;
    }

    public NodeDataType getNodeData() {
        if (cachedNodeData == null) {
            this.cachedNodeData = getPipeBlock().getBaseProperties();
        }
        return cachedNodeData;
    }

    @Nullable
    public TickableSubscription subscribeServerTick(Runnable runnable) {
        if (!isRemote()) {
            var subscription = new TickableSubscription(runnable);
            waitingToAdd.add(subscription);
            return subscription;
        }
        return null;
    }

    public void unsubscribe(@Nullable TickableSubscription current) {
        if (current != null) {
            current.unsubscribe();
        }
    }

    public final void serverTick() {
        if (!attachedToNet) {
            int activeConnections = getConnections();
            boolean isActiveNode = activeConnections != 0;
            LevelPipeNet.getLevelPipeNet((ServerLevel) level, networkType).addNode(getBlockPos(), activeConnections,
                    isActiveNode);
        }

        if (!waitingToAdd.isEmpty()) {
            serverTicks.addAll(waitingToAdd);
            waitingToAdd.clear();
        }
        for (var iter = serverTicks.iterator(); iter.hasNext();) {
            var tickable = iter.next();
            if (tickable.isStillSubscribed()) {
                tickable.run();
            }
            if (!tickable.isStillSubscribed()) {
                iter.remove();
            }
        }
    }

    public void scheduleNeighborShapeUpdate() {
        Level level = getLevel();
        BlockPos pos = getBlockPos();

        if (level == null)
            return;

        level.getBlockState(pos).updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
    }

    @Nullable
    public BlockEntity getNeighbor(Direction direction) {
        return getLevel().getBlockEntity(getBlockPos().relative(direction));
    }

    public void onNeighbourChange(BlockState state, BlockPos pos, BlockPos neighbor) {
        Direction facing = GTUtil.getFacingToNeighbor(pos, neighbor);
        if (facing == null) return;
        CoverBehavior cover = getCoverContainer().getCoverAtSide(facing);
        if (!ConfigHolder.INSTANCE.machines.gt6StylePipesCables) {
            boolean open = isConnected(facing);
            boolean canConnect = cover != null || getPipeBlock().canConnect(this, facing);
            if (!open && canConnect)
                setConnection(facing, true, false);
            if (open && !canConnect)
                setConnection(facing, false, false);
        }
        PipeNet net = getPipeNet();
        if (net != null) {
            getPipeNet().onNeighbourUpdate(neighbor);
        }
        if (cover != null) cover.onNeighborChanged(state.getBlock(), pos, false);
    }

    //////////////////////////////////////
    // ******* Pipe Status *******//
    //////////////////////////////////////

    public void setBlocked(Direction side, boolean isBlocked) {
        if (level instanceof ServerLevel serverLevel && canHaveBlockedFaces()) {
            blockedConnections = withSideConnection(blockedConnections, side, isBlocked);
            syncDataHolder.markClientSyncFieldDirty("blockedConnections");
            setChanged();
            LevelPipeNet worldPipeNet = LevelPipeNet.getLevelPipeNet(serverLevel, networkType);
            PipeNet net = worldPipeNet.getNetFromPos(getBlockPos());
            if (net != null) {
                net.onPipeConnectionsUpdate();
            }
        }
    }

    public int getVisualConnections() {
        var visualConnections = connections;
        for (var side : GTUtil.DIRECTIONS) {
            var cover = getCoverContainer().getCoverAtSide(side);
            if (cover != null && cover.canPipePassThrough()) {
                visualConnections = visualConnections | (1 << side.ordinal());
            }
        }
        return visualConnections;
    }

    public void setConnection(Direction side, boolean connected, boolean fromNeighbor) {
        // fix desync between two connections. Can happen if a pipe side is blocked, and a new pipe is placed next to
        // it.
        if (!getLevel().isClientSide) {
            if (isConnected(side) == connected) {
                return;
            }
            BlockEntity tile = getNeighbor(side);
            // block connections if Pipe Types do not match
            if (connected &&
                    tile instanceof PipeBlockEntity<?, ?> pipeTile &&
                    pipeTile.getPipeType().getClass() != this.getPipeType().getClass()) {
                return;
            }

            if (!connected) {
                var cover = getCoverContainer().getCoverAtSide(side);
                if (cover != null && cover.canPipePassThrough()) return;
            }

            connections = withSideConnection(connections, side, connected);
            syncDataHolder.markClientSyncFieldDirty("connections");
            updateNetworkConnection(side, connected);
            // notify neighbor of change so Auto Output updates its ticking status
            getLevel().neighborChanged(getBlockPos().relative(side), getPipeBlock(), getBlockPos());
            setChanged();

            if (!fromNeighbor && tile instanceof PipeBlockEntity<?, ?> pipeTile) {
                syncPipeConnections(side, pipeTile);
            }
        }
    }

    private void syncPipeConnections(Direction side, PipeBlockEntity<?, ?> pipe) {
        Direction oppositeSide = side.getOpposite();
        boolean neighbourOpen = pipe.isConnected(oppositeSide);
        if (isConnected(side) == neighbourOpen) {
            return;
        }
        if (!neighbourOpen || pipe.getCoverContainer().getCoverAtSide(oppositeSide) == null) {
            pipe.setConnection(oppositeSide, !neighbourOpen, true);
        }
    }

    private void updateNetworkConnection(Direction side, boolean connected) {
        LevelPipeNet worldPipeNet = LevelPipeNet.getLevelPipeNet((ServerLevel) getLevel(), networkType);
        worldPipeNet.updateBlockedConnections(getBlockPos(), side, !connected);
    }

    protected int withSideConnection(int blockedConnections, Direction side, boolean connected) {
        int index = 1 << side.ordinal();
        if (connected) {
            return blockedConnections | index;
        } else {
            return blockedConnections & ~index;
        }
    }

    public void notifyBlockUpdate() {
        getLevel().updateNeighborsAt(getBlockPos(), getPipeBlock());
    }

    @Override
    public boolean triggerEvent(int id, int para) {
        if (id == 1) { // chunk re render
            if (level != null && level.isClientSide) {
                scheduleRenderUpdate();
            }
            return true;
        }
        return false;
    }

    @Override
    public void setChanged() {
        if (getLevel() != null) {
            getLevel().blockEntityChanged(getBlockPos());
        }
    }

    //////////////////////////////////////
    // ******* Interaction *******//
    //////////////////////////////////////
    @Override
    public boolean shouldRenderGrid(Player player, BlockPos pos, BlockState state, ItemStack held,
                                    Set<GTToolType> toolTypes) {
        if (toolTypes.contains(getPipeTuneTool())) return true;
        for (CoverBehavior cover : coverContainer.getCovers()) {
            if (cover.shouldRenderGrid(player, pos, state, held, toolTypes)) return true;
        }
        return false;
    }

    public ResourceTexture getPipeTexture(boolean isBlock) {
        return isBlock ? GuiTextures.TOOL_PIPE_CONNECT : GuiTextures.TOOL_PIPE_BLOCK;
    }

    @Override
    public @Nullable ResourceTexture sideTips(Player player, BlockPos pos, BlockState state, Set<GTToolType> toolTypes,
                                              Direction side) {
        if (toolTypes.contains(getPipeTuneTool())) {
            if (player.isShiftKeyDown() && this.canHaveBlockedFaces()) {
                return getPipeTexture(isBlocked(side));
            } else {
                return getPipeTexture(isConnected(side));
            }
        }
        var cover = coverContainer.getCoverAtSide(side);
        if (cover != null) {
            return cover.sideTips(player, pos, state, toolTypes, side);
        }
        return null;
    }

    @Override
    public Pair<GTToolType, InteractionResult> onToolClick(Set<GTToolType> toolTypes, ItemStack itemStack,
                                                           UseOnContext context) {
        // the side hit from the machine grid
        var playerIn = context.getPlayer();
        if (playerIn == null) return Pair.of(null, InteractionResult.PASS);

        var hand = context.getHand();
        var hitResult = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.getClickedPos(), false);
        Direction gridSide = ICoverable.determineGridSideHit(hitResult);
        CoverBehavior coverBehavior = gridSide == null ? null : coverContainer.getCoverAtSide(gridSide);
        if (gridSide == null) gridSide = hitResult.getDirection();

        // Prioritize covers where they apply (Screwdriver, Soft Mallet)
        if (toolTypes.isEmpty() && playerIn.isShiftKeyDown()) {
            if (coverBehavior != null) {
                return Pair.of(null, coverBehavior.onScrewdriverClick(playerIn, hand, hitResult));
            }
        }
        if (toolTypes.contains(GTToolType.SCREWDRIVER)) {
            if (coverBehavior != null) {
                return Pair.of(GTToolType.SCREWDRIVER, coverBehavior.onScrewdriverClick(playerIn, hand, hitResult));
            }
        } else if (toolTypes.contains(GTToolType.SOFT_MALLET)) {
            if (coverBehavior != null) {
                return Pair.of(GTToolType.SOFT_MALLET, coverBehavior.onSoftMalletClick(playerIn, hand, hitResult));
            }
        } else if (toolTypes.contains(getPipeTuneTool())) {
            if (playerIn.isShiftKeyDown() && this.canHaveBlockedFaces()) {
                boolean isBlocked = this.isBlocked(gridSide);
                this.setBlocked(gridSide, !isBlocked);
            } else {
                boolean isOpen = this.isConnected(gridSide);
                this.setConnection(gridSide, !isOpen, false);
            }
            return Pair.of(getPipeTuneTool(), InteractionResult.sidedSuccess(playerIn.level().isClientSide));
        } else if (toolTypes.contains(GTToolType.CROWBAR)) {
            if (coverBehavior != null) {
                if (!isRemote()) {
                    getCoverContainer().removeCover(gridSide, playerIn);
                    return Pair.of(GTToolType.CROWBAR, InteractionResult.sidedSuccess(playerIn.level().isClientSide));
                }
            } else {
                if (!frameMaterial.isNull()) {
                    Block.popResource(getLevel(), getBlockPos(),
                            GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, frameMaterial).asStack());
                    frameMaterial = GTMaterials.NULL;
                    return Pair.of(GTToolType.CROWBAR, InteractionResult.sidedSuccess(playerIn.level().isClientSide));
                }
            }
        }

        return Pair.of(null, InteractionResult.PASS);
    }

    public GTToolType getPipeTuneTool() {
        return GTToolType.WRENCH;
    }

    @Override
    public int getDefaultPaintingColor() {
        return this.getPipeBlock() instanceof MaterialPipeBlock<?, ?> materialPipeBlock ?
                materialPipeBlock.material.getMaterialRGB() : 0xFFFFFF;
    }

    public void doExplosion(float explosionPower) {
        getLevel().removeBlock(getBlockPos(), false);
        if (!getLevel().isClientSide) {
            ((ServerLevel) getLevel()).sendParticles(ParticleTypes.LARGE_SMOKE, getBlockPos().getX() + 0.5,
                    getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5,
                    10, 0.2, 0.2, 0.2, 0.0);
        }
        getLevel().explode(null, getBlockPos().getX() + 0.5, getBlockPos().getY() + 0.5, getBlockPos().getZ() + 0.5,
                explosionPower, Level.ExplosionInteraction.NONE);
    }

    public static boolean isFaceBlocked(int blockedConnections, Direction side) {
        return (blockedConnections & (1 << side.ordinal())) > 0;
    }

    public static boolean isConnected(int connections, Direction side) {
        return (connections & (1 << side.ordinal())) > 0;
    }

    @Override
    public CompoundTag copyConfig(CompoundTag tag) {
        return ICopyable.super.copyConfig(tag);
    }

    @Override
    public void pasteConfig(ServerPlayer player, CompoundTag tag) {
        ICopyable.super.pasteConfig(player, tag);
    }

    @Override
    public List<ItemStack> getItemsRequiredToPaste() {
        return coverContainer.getItemsRequiredToPaste();
    }

    @Override
    public void addDebugOverlayText(Consumer<String> leftLines, Consumer<String> rightLines) {
        leftLines.accept(ChatFormatting.UNDERLINE + "Targeted Pipe: " + getBlockPos().toShortString());
        leftLines.accept(getBlockState().getBlock().getName().getString());
        leftLines.accept("");
        leftLines.accept(ChatFormatting.UNDERLINE + "Pipe Network");
        leftLines.accept(getNetworkType().toString());
        leftLines.accept(getPipeNet() != null ? getPipeNet().toString(): "<no network>");
        leftLines.accept("");
        leftLines.accept(ChatFormatting.UNDERLINE + "Pipe Segment");
        for (var p: getPipeBlock().defaultSegmentProperties.getProperties().entrySet()) {
            leftLines.accept(p.getKey().getId().toString() + "=" + p.getValue().toString() + ",");
        }
    }
}
