package com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.blockentity.IPaintable;
import com.gregtechceu.gtceu.api.blockentity.ITickSubscription;
import com.gregtechceu.gtceu.api.blockentity.NeighborCacheBlockEntity;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.IToolable;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicEntry;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicRegistry;
import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicType;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNet;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.logic.TemperatureLogic;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IInsulatable;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeStructure;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block.PipeBlock;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.item.tool.IToolGridHighlight;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.network.PacketDataList;
import com.gregtechceu.gtceu.client.particle.GTOverheatParticle;
import com.gregtechceu.gtceu.client.particle.GTParticleManager;
import com.gregtechceu.gtceu.client.renderer.cover.CoverRendererPackage;
import com.gregtechceu.gtceu.client.renderer.cover.CoverRendererValues;
import com.gregtechceu.gtceu.client.renderer.pipe.PipeRenderProperties;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.editor.runtime.PersistedParser;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAutoPersistBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

@SuppressWarnings("lossy-conversions")
public class PipeBlockEntity extends NeighborCacheBlockEntity
                             implements IWorldPipeNetTile, ITickSubscription, IEnhancedManaged,
                             IAsyncAutoSyncBlockEntity, IAutoPersistBlockEntity, IToolGridHighlight, IToolable,
                             IPaintable {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PipeBlockEntity.class);
    @Getter
    public final IManagedStorage syncStorage = new FieldManagedStorage(this);

    public static final int DEFAULT_COLOR = 0xFFFFFFFF;

    public static final int UPDATE_PIPE_LOGIC = 0;

    @Getter
    private final Int2ObjectOpenHashMap<NetLogicData> netLogicDatas = new Int2ObjectOpenHashMap<>();
    private final Reference2ReferenceOpenHashMap<NetLogicType<?>, PendingLogicSync> pendingSyncs = new Reference2ReferenceOpenHashMap<>();
    private final ObjectOpenHashSet<NetLogicData.ListenerCallback<?>> listeners = new ObjectOpenHashSet<>();

    // this tile was loaded from datafixed NBT and needs to initialize its connections
    @Persisted
    private boolean legacy;
    // used to prevent firing neighbor block updates during chunkload, which will cause a CME
    private boolean suppressUpdates;

    // information that is only required for determining graph topology should be stored on the tile entity level,
    // while information interacted with during graph traversal should be stored on the NetLogicData level.

    @Persisted
    @DescSynced
    @Getter
    private byte connectionMask;
    @Persisted
    @DescSynced
    @Getter
    private byte renderMask;
    @Persisted
    @DescSynced
    @Getter
    private byte blockedMask;
    @Persisted
    @DescSynced
    @Getter
    @Setter
    private int paintingColor = -1;

    @RequireRerender
    @DescSynced
    @Persisted
    @Setter
    private @NotNull Material frameMaterial = GTMaterials.NULL;

    private final List<TickableSubscription> serverTicks = new ArrayList<>();
    private final List<TickableSubscription> waitingToAdd = new ArrayList<>();

    @Persisted
    @DescSynced
    @Getter
    protected final PipeCoverHolder coverHolder = new PipeCoverHolder(this);
    private final Reference2ReferenceOpenHashMap<NetNode, PipeCapabilityWrapper> netCapabilities = new Reference2ReferenceOpenHashMap<>();

    @Getter
    @Nullable
    private TemperatureLogic temperatureLogic;
    @Nullable
    private GTOverheatParticle overheatParticle;

    private final int offset = (int) (Math.random() * 20);

    public PipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState, true);
    }

    @Nullable
    public PipeBlockEntity getPipeNeighbor(Direction side, boolean allowChunkloading) {
        BlockEntity tile = allowChunkloading ? getNeighbor(side) : getNeighborNoChunkloading(side);
        if (tile instanceof PipeBlockEntity pipe) return pipe;
        else return null;
    }

    @Override
    public boolean triggerEvent(int id, int type) {
        if (id == 1) { // chunk re render
            if (level != null && level.isClientSide) {
                scheduleRenderUpdate();
            }
            return true;
        }
        return false;
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (getLevel() instanceof ServerLevel serverLevel) {
            getBlockType().getHandler(this)
                    .removeFromNets(serverLevel, this.getBlockPos(), this.getStructure());
            netCapabilities.values().forEach(PipeCapabilityWrapper::invalidate);
        } else killOverheatParticle();
    }

    public long getOffsetTimer() {
        return GTCEu.getMinecraftServer().getTickCount() + offset;
    }

    public void placedBy(ItemStack stack, Player player) {}

    public IPipeStructure getStructure() {
        return getBlockType().getStructure();
    }

    // mask //

    public boolean canConnectTo(Direction side) {
        return this.getStructure().canConnectTo(side, connectionMask);
    }

    public void setConnected(Direction side, boolean renderClosed) {
        this.connectionMask |= 1 << side.ordinal();
        updateActiveStatus(side, false);
        if (renderClosed) {
            this.renderMask |= 1 << side.ordinal();
        } else {
            this.renderMask &= ~(1 << side.ordinal());
        }
        scheduleRenderUpdate();
    }

    public void setDisconnected(Direction side) {
        this.connectionMask &= ~(1 << side.ordinal());
        this.renderMask &= ~(1 << side.ordinal());
        updateActiveStatus(side, false);
        scheduleRenderUpdate();
    }

    public boolean isConnected(Direction side) {
        return (this.connectionMask & 1 << side.ordinal()) > 0;
    }

    public boolean isConnectedCoverAdjusted(Direction side) {
        CoverBehavior cover;
        return ((this.connectionMask & 1 << side.ordinal()) > 0) ||
                (cover = getCoverHolder().getCoverAtSide(side)) != null && cover.forcePipeRenderConnection();
    }

    public void setRenderClosed(Direction side, boolean closed) {
        if (closed) {
            this.renderMask |= 1 << side.ordinal();
        } else {
            this.renderMask &= ~(1 << side.ordinal());
        }
    }

    public boolean renderClosed(Direction side) {
        return (this.renderMask & 1 << side.ordinal()) > 0;
    }

    public byte getCoverAdjustedConnectionMask() {
        byte connectionMask = this.connectionMask;
        for (Direction dir : GTUtil.DIRECTIONS) {
            CoverBehavior cover = getCoverHolder().getCoverAtSide(dir);
            if (cover != null) {
                if (cover.forcePipeRenderConnection()) connectionMask |= 1 << dir.ordinal();
            }
        }
        return connectionMask;
    }

    public void setBlocked(Direction side) {
        this.blockedMask |= (byte) (1 << side.ordinal());
        scheduleRenderUpdate();
    }

    public void setUnblocked(Direction side) {
        this.blockedMask &= (byte) ~(1 << side.ordinal());
        scheduleRenderUpdate();
    }

    public boolean isBlocked(Direction side) {
        return (this.blockedMask & 1 << side.ordinal()) > 0;
    }

    // paint //

    public int getVisualColor() {
        return isPainted() ? paintingColor : getDefaultPaintingColor();
    }

    public void setPaintingColor(int paintingColor, boolean alphaSensitive) {
        if (!alphaSensitive) {
            paintingColor |= 0xFF000000;
        }
        this.paintingColor = paintingColor;
        scheduleRenderUpdate();
    }

    @Override
    public boolean isPainted() {
        return this.paintingColor != -1;
    }

    @Override
    public int getDefaultPaintingColor() {
        return DEFAULT_COLOR;
    }

    public @NotNull Material getFrameMaterial() {
        // backwards compat
        // noinspection ConstantValue
        if (frameMaterial == null) {
            frameMaterial = GTMaterials.NULL;
        }
        return frameMaterial;
    }

    // ticking //

    @Override
    public @Nullable TickableSubscription subscribeServerTick(Runnable runnable) {
        if (!isClientSide()) {
            var subscription = new TickableSubscription(runnable);
            waitingToAdd.add(subscription);
            return subscription;
        }
        return null;
    }

    @Override
    public void unsubscribe(@Nullable TickableSubscription current) {
        if (current != null) {
            current.unsubscribe();
        }
    }

    public final void serverTick() {
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

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, this::initialize));
        }
    }

    @Override
    public void loadCustomPersistedData(CompoundTag tag) {
        if (tag.contains("connections")) {
            legacy = true;
            this.connectionMask = tag.getByte("connections");
        }
        if (tag.contains("blockedConnections")) {
            legacy = true;
            this.blockedMask = tag.getByte("blockedConnections");
        }
        if (tag.contains("cover")) {
            legacy = true;
            PersistedParser.deserializeNBT(tag.getCompound("cover"), new HashMap<>(),
                    PipeCoverHolder.class, this.coverHolder);
        }
    }

    // activeness //

    @Override
    public void onNeighborChanged(Block fromBlock, BlockPos fromPos, boolean isMoving) {
        super.onNeighborChanged(fromBlock, fromPos, isMoving);
        Direction side = GTUtil.getFacingToNeighbor(this.getBlockPos(), fromPos);
        coverHolder.onNeighborChanged(fromBlock, fromPos, isMoving);
        updateActiveStatus(side, false);
    }

    /**
     * Returns a map of facings to tile entities that should have at least one of the required capabilities.
     * 
     * @param node the node for this tile entity. Used to identify the capabilities to match.
     * @return a map of facings to tile entities.
     */
    public @NotNull EnumMap<Direction, BlockEntity> getTargetsWithCapabilities(WorldPipeNode node) {
        PipeCapabilityWrapper wrapper = netCapabilities.get(node);
        EnumMap<Direction, BlockEntity> caps = new EnumMap<>(Direction.class);
        if (wrapper == null) return caps;

        for (Direction side : GTUtil.DIRECTIONS) {
            if (wrapper.isActive(side)) {
                BlockEntity tile = getNeighbor(side);
                if (tile == null) updateActiveStatus(side, false);
                else caps.put(side, tile);
            }
        }
        return caps;
    }

    public @Nullable BlockEntity getTargetWithCapabilities(WorldPipeNode node, Direction side) {
        PipeCapabilityWrapper wrapper = netCapabilities.get(node);
        if (wrapper == null || !wrapper.isActive(side)) return null;
        else return getNeighbor(side);
    }

    @Override
    public PipeCapabilityWrapper getWrapperForNode(WorldPipeNode node) {
        return netCapabilities.get(node);
    }

    /**
     * Updates the pipe's active status based on the tile entity connected to the side.
     * 
     * @param side              the side to check. Can be null, in which case all sides will be checked.
     * @param canOpenConnection whether the pipe is allowed to open a new connection if it finds a tile it can connect
     *                          to.
     */
    public void updateActiveStatus(@Nullable Direction side, boolean canOpenConnection) {
        if (side == null) {
            for (Direction facing : GTUtil.DIRECTIONS) {
                updateActiveStatus(facing, canOpenConnection);
            }
            return;
        }
        if (!this.isConnectedCoverAdjusted(side) && !(canOpenConnection && canConnectTo(side))) {
            setAllIdle(side);
            return;
        }

        BlockEntity tile = getNeighbor(side);
        if (tile == null || tile instanceof PipeBlockEntity) {
            setAllIdle(side);
            return;
        }

        boolean oneActive = false;
        for (var netCapability : netCapabilities.entrySet()) {
            for (Capability<?> cap : netCapability.getValue().capabilities.keySet()) {
                // hardcode an exception for hazard containers since they can
                // "connect" and push the pipe contents into empty space
                if (tile.getCapability(cap, side.getOpposite()).isPresent() ||
                        cap == GTCapability.CAPABILITY_HAZARD_CONTAINER) {
                    oneActive = true;
                    netCapability.getValue().setActive(side);
                    break;
                }
            }
        }
        if (canOpenConnection && oneActive) this.setConnected(side, false);
    }

    private void setAllIdle(Direction side) {
        for (var netCapability : netCapabilities.entrySet()) {
            netCapability.getValue().setIdle(side);
        }
    }

    // capability //

    public <T> LazyOptional<T> getCapabilityCoverQuery(@NotNull Capability<T> capability, @Nullable Direction side) {
        for (PipeCapabilityWrapper wrapper : netCapabilities.values()) {
            LazyOptional<T> cap = wrapper.getCapability(capability, side);
            if (cap.isPresent()) return cap;
        }
        return LazyOptional.empty();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction side) {
        if (capability == GTCapability.CAPABILITY_COVERABLE) {
            return GTCapability.CAPABILITY_COVERABLE.orEmpty(capability, LazyOptional.of(this::getCoverHolder));
        }
        LazyOptional<T> pipeCapability = LazyOptional.empty();
        for (PipeCapabilityWrapper wrapper : netCapabilities.values()) {
            if ((pipeCapability = wrapper.getCapability(capability, side)).isPresent()) break;
        }
        if (!pipeCapability.isPresent()) pipeCapability = super.getCapability(capability, side);

        CoverBehavior cover = side == null ? null : getCoverHolder().getCoverAtSide(side);
        if (cover == null) {
            if (side == null || isConnected(side)) {
                return pipeCapability;
            }
            return super.getCapability(capability, side);
        }

        LazyOptional<T> coverCapability = cover.getCapability(capability, pipeCapability);
        if (coverCapability == pipeCapability) {
            if (isConnectedCoverAdjusted(side)) {
                return pipeCapability;
            }
            return super.getCapability(capability, side);
        }
        return coverCapability;
    }

    // data sync management //

    public NetLogicData getNetLogicData(int networkID) {
        return netLogicDatas.get(networkID);
    }

    public @NotNull PipeBlock getBlockType() {
        return (PipeBlock) this.getBlockState().getBlock();
    }

    @Override
    public void setLevel(@NotNull Level level) {
        if (level == this.getLevel()) return;
        super.setLevel(level);
    }

    public void initialize() {
        if (!getLevel().isClientSide) {
            this.netLogicDatas.clear();
            this.netCapabilities.clear();
            this.listeners.forEach(NetLogicData.ListenerCallback::retire);
            this.listeners.clear();
            for (WorldPipeNode node : PipeBlock.getNodesForTile(this)) {
                WorldPipeNet net = node.getNet();
                this.netCapabilities.put(node, net.buildCapabilityWrapper(this, node));
                int networkID = net.getNetworkID();
                netLogicDatas.put(networkID, node.getData());
                node.getData().addListener(
                        (e, r, f) -> markDataForSync(networkID, e, r, f));
                for (var entry : node.getData().getEntries()) {
                    markDataForSync(networkID, entry, false, true);
                }
                if (this.temperatureLogic == null) {
                    TemperatureLogic candidate = node.getData().getLogicEntryNullable(TemperatureLogic.TYPE);
                    if (candidate != null)
                        updateTemperatureLogic(candidate);
                }
            }
            if (this.legacy) {
                for (Direction side : GTUtil.DIRECTIONS) {
                    if (this.isConnected(side)) {
                        PipeBlock.connect(this, this.getPipeNeighbor(side, false), side);
                        BlockPos pos = this.getBlockPos().relative(side);
                        ChunkAccess chunk = getLevel().getChunk(pos);
                        if (chunk instanceof LevelChunk levelChunk) {
                            BlockEntity candidate = levelChunk
                                    .getBlockEntity(pos, LevelChunk.EntityCreationType.CHECK);
                            if (candidate instanceof PipeBlockEntity pipe)
                                PipeBlock.connect(this, pipe, side);
                        }
                    }
                }
            }
            this.netLogicDatas.trim();
            this.netCapabilities.trim();
            this.suppressUpdates = true;
            updateActiveStatus(null, false);
            this.suppressUpdates = false;
        }
    }

    private void markDataForSync(int networkID, NetLogicEntry<?, ?> entry, boolean removed, boolean fullChange) {
        // attempt to collapse multiple updates to the same data that occur before a sync packet is sent
        PendingLogicSync existing = pendingSyncs.get(entry.getType());
        if (existing != null) {
            if (removed && !existing.isRemoved()) existing.markRemoved();
            else if (!removed && existing.isRemoved()) {
                // if the previous change was a removal and then this change is not a removal,
                // then this is equivalent to a full change.
                existing.markRemoved();
                existing.markFullChange();
            }
            if (fullChange) existing.markFullChange();
        } else {
            pendingSyncs.put(entry.getType(), new PendingLogicSync(networkID, entry, removed, fullChange));
        }
        notifyWorldOfPendingPackets();
    }

    @Override
    protected void beforeUpdatePacket(PacketDataList pendingUpdates) {
        for (PendingLogicSync pendingSync : pendingSyncs.values()) {
            writeCustomData(UPDATE_PIPE_LOGIC, buf -> {
                buf.writeVarInt(pendingSync.networkID());
                buf.writeBoolean(pendingSync.isRemoved());
                if (pendingSync.isRemoved())
                    buf.writeVarInt(NetLogicRegistry.getNetworkID(pendingSync.entry().getType()));
                else NetLogicData.writeEntry(buf, pendingSync.entry(), pendingSync.isFullChange());
            });
        }
        pendingSyncs.clear();
    }

    @Override
    public void receiveCustomData(int discriminator, @NotNull FriendlyByteBuf buf) {
        if (discriminator == UPDATE_PIPE_LOGIC) {
            // extra check just to make sure we don't affect actual net data with our writes
            if (level.isClientSide) {
                int networkID = buf.readVarInt();
                boolean removed = buf.readBoolean();
                if (removed) {
                    NetLogicType<?> type = NetLogicRegistry.getType(buf.readVarInt());
                    NetLogicData data = this.netLogicDatas.get(networkID);
                    if (data != null) data.removeLogicEntry(type);
                } else {
                    NetLogicData data = this.netLogicDatas.computeIfAbsent(networkID, i -> new NetLogicData());
                    NetLogicEntry<?, ?> read = data.readEntry(buf);
                    if (read instanceof TemperatureLogic tempLogic) {
                        updateTemperatureLogic(tempLogic);
                    }
                }
            }
        }
    }

    // particle //

    public void updateTemperatureLogic(@NotNull TemperatureLogic logic) {
        this.temperatureLogic = logic;
        if (getLevel().isClientSide) {
            updateTemperatureLogicClient(logic);
        }
    }

    @OnlyIn(Dist.CLIENT)
    protected void updateTemperatureLogicClient(@NotNull TemperatureLogic logic) {
        if (overheatParticle == null || !overheatParticle.isAlive()) {
            int temp = logic.getTemperature(logic.getLastRestorationTick());
            if (temp > GTOverheatParticle.TEMPERATURE_CUTOFF) {
                IPipeStructure structure = this.getStructure();
                overheatParticle = new GTOverheatParticle(this, logic, structure.getPipeBoxes(this),
                        structure instanceof IInsulatable i && i.isInsulated());
                GTParticleManager.INSTANCE.addEffect(overheatParticle);
            }
        } else {
            overheatParticle.setTemperatureLogic(logic);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void killOverheatParticle() {
        if (overheatParticle != null) {
            overheatParticle.setExpired();
            overheatParticle = null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public boolean isOverheatParticleAlive() {
        return overheatParticle != null && overheatParticle.isAlive();
    }

    // misc overrides //

    public void scheduleNeighborShapeUpdate() {
        if (suppressUpdates) {
            return;
        }
        Level level = getLevel();
        if (level == null) return;
        BlockPos pos = getBlockPos();

        level.getBlockState(pos).updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
    }

    @Override
    @SuppressWarnings("ConstantConditions") // yes this CAN actually be null
    public void markAsDirty() {
        if (hasLevel()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_KNOWN_SHAPE);
        }
        // this most notably gets called when the covers of a pipe get updated, aka the edge predicates need syncing.
        for (var node : this.netCapabilities.keySet()) {
            if (node instanceof WorldPipeNode n) n.getNet().updatePredication(n, this);
        }
        this.setChanged();
    }

    @Override
    public @NotNull ModelData getModelData() {
        byte frameMask = 0;
        for (Direction side : GTUtil.DIRECTIONS) {
            CoverBehavior cover = getCoverHolder().getCoverAtSide(side);
            if (cover != null) {
                frameMask |= 1 << side.ordinal();
                if (cover.forcePipeRenderConnection()) this.connectionMask |= (byte) (1 << side.ordinal());
            }
        }
        frameMask = (byte) ~frameMask;
        return ModelData.builder()
                .with(PipeRenderProperties.THICKNESS_PROPERTY, this.getStructure().getRenderThickness())
                .with(PipeRenderProperties.CONNECTED_MASK_PROPERTY, connectionMask)
                .with(PipeRenderProperties.CLOSED_MASK_PROPERTY, renderMask)
                .with(PipeRenderProperties.BLOCKED_MASK_PROPERTY, blockedMask)
                .with(PipeRenderProperties.COLOR_PROPERTY, getPaintingColor())
                .with(PipeRenderProperties.FRAME_MATERIAL_PROPERTY, frameMaterial)
                .with(PipeRenderProperties.FRAME_MASK_PROPERTY, frameMask)
                .with(CoverRendererPackage.PROPERTY, getCoverHolder().createPackage())
                .build();
    }

    @Override
    public void scheduleRenderUpdate() {
        super.scheduleRenderUpdate();
        requestModelDataUpdate();
        if (getLevel().isClientSide) scheduleRenderUpdateClient();
    }

    protected void scheduleRenderUpdateClient() {
        if (overheatParticle != null) {
            overheatParticle.updatePipeBoxes(getStructure().getPipeBoxes(this));
        }
    }

    public void getCoverBoxes(Consumer<VoxelShape> consumer) {
        for (Direction side : GTUtil.DIRECTIONS) {
            if (getCoverHolder().hasCover(side)) {
                consumer.accept(Shapes.create(CoverRendererValues.PLATE_AABBS.get(side)));
            }
        }
    }

    @Override
    public boolean shouldRenderGrid(Player player, BlockPos pos, BlockState state, ItemStack held,
                                    Set<GTToolType> toolTypes) {
        if (toolTypes.contains(getBlockType().getToolClass()) || toolTypes.contains(GTToolType.SCREWDRIVER))
            return true;
        for (CoverBehavior cover : coverHolder.getCovers()) {
            if (cover.shouldRenderGrid(player, pos, state, held, toolTypes)) return true;
        }
        return false;
    }

    public @Nullable ResourceTexture sideTips(Player player, BlockPos pos, BlockState state, Set<GTToolType> toolTypes,
                                              Direction side) {
        if (toolTypes.contains(getBlockType().getToolClass())) {
            if (player.isShiftKeyDown() && this.getBlockType().allowsBlocking()) {
                return getStructure().getPipeTexture(isBlocked(side));
            } else {
                return getStructure().getPipeTexture(isConnected(side));
            }
        }
        var cover = coverHolder.getCoverAtSide(side);
        if (cover != null) {
            return cover.sideTips(player, pos, state, toolTypes, side);
        }
        return null;
    }

    @Override
    public Pair<@Nullable GTToolType, InteractionResult> onToolClick(@NotNull Set<GTToolType> toolTypes,
                                                                     ItemStack itemStack, UseOnContext context) {
        // the side hit from the machine grid
        var playerIn = context.getPlayer();
        if (playerIn == null) return Pair.of(null, InteractionResult.PASS);

        var hand = context.getHand();
        var hitResult = new BlockHitResult(context.getClickLocation(), context.getClickedFace(),
                context.getClickedPos(), false);
        Direction gridSide = ICoverable.determineGridSideHit(hitResult);
        CoverBehavior coverBehavior = gridSide == null ? null : coverHolder.getCoverAtSide(gridSide);
        if (gridSide == null) gridSide = hitResult.getDirection();

        // Prioritize covers where they apply (Screwdriver, Soft Mallet)
        if (toolTypes.contains(GTToolType.SCREWDRIVER) || (itemStack.isEmpty() && playerIn.isShiftKeyDown())) {
            if (coverBehavior != null) {
                return Pair.of(GTToolType.SCREWDRIVER, coverBehavior.onScrewdriverClick(playerIn, hand, hitResult));
            }
        } else if (toolTypes.contains(GTToolType.SOFT_MALLET)) {
            if (coverBehavior != null) {
                return Pair.of(GTToolType.SOFT_MALLET, coverBehavior.onSoftMalletClick(playerIn, hand, hitResult));
            }
        } else if (toolTypes.contains(this.getBlockType().getToolClass())) {
            if (playerIn.isShiftKeyDown() && this.getBlockType().allowsBlocking()) {
                boolean isBlocked = this.isBlocked(gridSide);
                if (isBlocked) {
                    PipeBlock.unblock(this, this.getPipeNeighbor(gridSide, true), gridSide);
                } else {
                    PipeBlock.block(this, this.getPipeNeighbor(gridSide, true), gridSide);
                }
            } else {
                boolean isOpen = this.isConnected(gridSide);
                if (isOpen) {
                    PipeBlock.disconnect(this, this.getPipeNeighbor(gridSide, true), gridSide);
                } else {
                    PipeBlock.connect(this, this.getPipeNeighbor(gridSide, true), gridSide);
                }
            }
            return Pair.of(this.getBlockType().getToolClass(),
                    InteractionResult.sidedSuccess(context.getLevel().isClientSide));
        } else if (toolTypes.contains(GTToolType.CROWBAR)) {
            if (coverBehavior != null) {
                if (isServerSide()) {
                    coverHolder.removeCover(gridSide, playerIn);
                    return Pair.of(GTToolType.CROWBAR, InteractionResult.sidedSuccess(context.getLevel().isClientSide));
                }
            } else if (!getFrameMaterial().isNull()) {
                Block.popResource(context.getLevel(), getBlockPos(),
                        GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, getFrameMaterial()).asStack());
                setFrameMaterial(GTMaterials.NULL);
                return Pair.of(GTToolType.CROWBAR, InteractionResult.sidedSuccess(context.getLevel().isClientSide));
            }
        }

        return Pair.of(null, InteractionResult.PASS);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onChanged() {
        this.markAsDirty();
    }

    @Override
    public IManagedStorage getRootStorage() {
        return syncStorage;
    }
}
