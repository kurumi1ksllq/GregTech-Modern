package com.gregtechceu.gtceu.common.blockentity;

import com.gregtechceu.gtceu.api.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ILaserContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.api.pipenet.IPipeNode;
import com.gregtechceu.gtceu.common.pipelike.laser.*;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.EnumMap;

public class LaserMirrorPipeBlockEntity extends LaserPipeBlockEntity {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(LaserMirrorPipeBlockEntity.class,
            PipeBlockEntity.MANAGED_FIELD_HOLDER);

    protected final EnumMap<Direction, LaserNetHandler> handlers = new EnumMap<>(Direction.class);
    public final ILaserContainer clientCapability = new LaserPipeBlockEntity.DefaultLaserContainer();
    private WeakReference<LaserPipeNet> currentPipeNet = new WeakReference<>(null);
    @Getter
    protected LaserNetHandler defaultHandler;

    protected LaserMirrorPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static LaserMirrorPipeBlockEntity create(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        return new LaserMirrorPipeBlockEntity(type, pos, state);
    }

    //public static void onBlockEntityRegister(BlockEntityType<LaserMirrorPipeBlockEntity> ignored) {}

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if(cap == GTCapability.CAPABILITY_LASER) {
            if (getLevel().isClientSide) {
                return GTCapability.CAPABILITY_LASER.orEmpty(cap, LazyOptional.of(() -> clientCapability));
            }

            if (handlers.isEmpty()) {
                initHandlers();
            }
            checkNetwork();
            return GTCapability.CAPABILITY_LASER.orEmpty(cap, LazyOptional.of(() -> handlers.getOrDefault(side, defaultHandler)));
        } else if (cap == GTCapability.CAPABILITY_COVERABLE) {
            return GTCapability.CAPABILITY_COVERABLE.orEmpty(cap, LazyOptional.of(this::getCoverContainer));
        } else if (cap == GTCapability.CAPABILITY_TOOLABLE) {
            return GTCapability.CAPABILITY_TOOLABLE.orEmpty(cap, LazyOptional.of(() -> this));
        }
        return super.getCapability(cap, side);
    }

    @Override
    public boolean canHaveBlockedFaces() {
        return false;
    }

    public void initHandlers() {
        LaserPipeNet net = getLaserPipeNet();
        if(net == null) return;
        for(Direction face : GTUtil.DIRECTIONS) {
            handlers.put(face, new LaserNetHandler(net, this, face));
        }
        defaultHandler = new LaserNetHandler(net, this, null);
    }

    public void checkNetwork() {
        if(defaultHandler != null) {
            LaserPipeNet current = getLaserPipeNet();
            if(defaultHandler.getNet() != current) {
                for(var handler : handlers.values()) {
                    handler.updateNetwork(current);
                }
            }
        }
    }

    public LaserPipeNet getLaserPipeNet() {
        if(level == null || level.isClientSide) {
            return null;
        }
        LaserPipeNet currentNet = this.currentPipeNet.get();
        if (currentNet != null && currentNet.isValid() && currentNet.containsNode(getPipePos())) {
            return currentNet;
        }
        LevelLaserPipeNet worldNet = (LevelLaserPipeNet) getPipeBlock().getWorldPipeNet((ServerLevel) getPipeLevel());
        currentNet = worldNet.getNetFromPos(getPipePos());
        if (currentNet != null) {
            this.currentPipeNet = new WeakReference<>(currentNet);
        }
        return currentNet;
    }

    @Override
    public boolean canAttachTo(Direction side) {
        if(level != null) {
            return true;
        }
        return false;
    }

    private static int numBitSet(int connections) {
        int r = 0;
        for(int i = 0; i < 6; i++) {
            if((connections >> i & 1) == 1) r++;
        }
        return r;
    }

    @Override
    public void setConnection(Direction side, boolean connected, boolean fromNeighbor) {
        if(!getLevel().isClientSide && connected) {
            int connections = getConnections();
            int numBitSet = numBitSet(connections);
            if(numBitSet >= 2) return;

            BlockEntity tile = getLevel().getBlockEntity(getBlockPos().relative(side));
            if(tile instanceof IPipeNode<?, ?> pipeTile &&
                    pipeTile.getPipeType().getClass() == this.getPipeType().getClass()) {
                connections = pipeTile.getConnections();
                connections &= ~(1 << side.ordinal());
                connections &= ~(1 << side.getOpposite().ordinal());
                if(connections != 0) return;
            }
        }

        if (!getLevel().isClientSide) {
            if (isConnected(side) == connected) {
                return;
            }
            BlockEntity tile = getNeighbor(side);
            // block connections if Pipe Types do not match
            if (connected &&
                    tile instanceof IPipeNode<?, ?> pipeTile &&
                    pipeTile.getPipeType().getClass() != this.getPipeType().getClass()) {
                return;
            }

            if (!connected) {
                var cover = getCoverContainer().getCoverAtSide(side);
                if (cover != null && cover.canPipePassThrough()) return;
            }

            connections = withSideConnection(connections, side, connected);

            updateNetworkConnection(side, connected);
            setChanged();

            if (!fromNeighbor && tile instanceof IPipeNode<?, ?> pipeTile) {
                syncPipeConnections(side, pipeTile);
            }
        }
    }

    @Override
    public GTToolType getPipeTuneTool() {
        return GTToolType.WIRE_CUTTER;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }
}
