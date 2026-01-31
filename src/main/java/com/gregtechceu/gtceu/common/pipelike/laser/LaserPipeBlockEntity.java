package com.gregtechceu.gtceu.common.pipelike.laser;

import com.gregtechceu.gtceu.api.pipenet.PipeBlockEntity;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.ILaserContainer;
import com.gregtechceu.gtceu.api.item.tool.GTToolType;
import com.gregtechceu.gtceu.common.pipelike.GTPipeNetworks;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.utils.TaskHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;

public class LaserPipeBlockEntity extends PipeBlockEntity<LaserPipeType, LaserPipeProperties> {

    // the LaserNetHandler can only be created on the server, so we have an empty placeholder for the client
    public final ILaserContainer clientCapability = new DefaultLaserContainer();

    private int ticksActive = 0;
    private int activeDuration = 0;
    @Getter
    @SaveField
    @SyncToClient
    private boolean active = false;

    protected LaserPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, GTPipeNetworks.LASER, pos, blockState);
    }

    public static LaserPipeBlockEntity create(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        return new LaserPipeBlockEntity(type, pos, blockState);
    }

    public static void onBlockEntityRegister(BlockEntityType<LaserPipeBlockEntity> cableBlockEntityBlockEntityType) {}

    @Override
    public boolean canHaveBlockedFaces() {
        return false;
    }

    /**
     * @param active   if the pipe should become active
     * @param duration how long the pipe should be active for
     */
    public void setActive(boolean active, int duration) {
        if (this.active != active) {
            this.active = active;
            syncDataHolder.markClientSyncFieldDirty("active");
            notifyBlockUpdate();
            setChanged();
            if (active && duration != this.activeDuration) {
                TaskHandler.enqueueServerTask((ServerLevel) getLevel(), this::queueDisconnect, 0);
            }
        }

        this.activeDuration = duration;
        if (duration > 0 && active) {
            this.ticksActive = 0;
        }
    }

    public boolean queueDisconnect() {
        if (++this.ticksActive % activeDuration == 0) {
            this.ticksActive = 0;
            setActive(false, -1);
            return false;
        }
        return true;
    }

    public boolean canAttachTo(Direction side) {
        if (level != null) {
            if (level.getBlockEntity(getBlockPos().relative(side)) instanceof LaserPipeBlockEntity) {
                return false;
            }
            return GTCapabilityHelper.getLaser(level, getBlockPos().relative(side), side.getOpposite()) != null;
        }
        return false;
    }

    @Override
    public void setConnection(Direction side, boolean connected, boolean fromNeighbor) {
        if (!getLevel().isClientSide && connected) {
            int connections = getConnections();
            // block connection if any side other than the requested side and its opposite side are already connected.
            connections &= ~(1 << side.ordinal());
            connections &= ~(1 << side.getOpposite().ordinal());
            if (connections != 0) return;

            // check the same for the targeted pipe
            BlockEntity tile = getLevel().getBlockEntity(getBlockPos().relative(side));
            if (tile instanceof PipeBlockEntity<?, ?> pipeTile &&
                    pipeTile.getPipeType().getClass() == this.getPipeType().getClass()) {
                connections = pipeTile.getConnections();
                connections &= ~(1 << side.ordinal());
                connections &= ~(1 << side.getOpposite().ordinal());
                if (connections != 0) return;
            }
        }
        super.setConnection(side, connected, fromNeighbor);
    }

    @Override
    public GTToolType getPipeTuneTool() {
        return GTToolType.WIRE_CUTTER;
    }

    private static class DefaultLaserContainer implements ILaserContainer {

        @Override
        public long acceptEnergyFromNetwork(Direction side, long voltage, long amperage) {
            return 0;
        }

        @Override
        public boolean inputsEnergy(Direction side) {
            return false;
        }

        @Override
        public long changeEnergy(long differenceAmount) {
            return 0;
        }

        @Override
        public long getEnergyStored() {
            return 0;
        }

        @Override
        public long getEnergyCapacity() {
            return 0;
        }

        @Override
        public long getInputAmperage() {
            return 0;
        }

        @Override
        public long getInputVoltage() {
            return 0;
        }
    }
}
