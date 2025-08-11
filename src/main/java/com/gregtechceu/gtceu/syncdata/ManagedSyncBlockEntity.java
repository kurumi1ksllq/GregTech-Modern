package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.syncdata.network.SPacketUpdateBESyncValue;
import lombok.Setter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * A BlockEntity that manages sync and save data via the {@code ISyncManaged} syncdata system.
 * 
 * @see ISyncManaged
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ManagedSyncBlockEntity extends BlockEntity implements ISyncManaged {

    @Getter
    protected final SyncDataHolder syncDataHolder = new SyncDataHolder(this);
    @Getter
    @Setter
    private boolean isDirty;

    public ManagedSyncBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    protected ISyncManaged @NotNull [] getSyncObjects() {
        return new ISyncManaged[] { this };
    }

    // Called when this BlockEntity is saved or loaded

    @Override
    protected final void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        for (ISyncManaged obj: getSyncObjects()) {
            tag.merge(obj.getSyncDataHolder().serializeNBT(false));
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        for (ISyncManaged obj: getSyncObjects()) {
            obj.getSyncDataHolder().deserializeNBT(tag, false);
        }
    }

    // Called when a client loads this BlockEntity

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        for (ISyncManaged obj: getSyncObjects()) {
            tag.merge(obj.getSyncDataHolder().serializeNBT(true));
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        for (ISyncManaged obj: getSyncObjects()) {
            obj.getSyncDataHolder().deserializeNBT(tag, true);
        }
    }

    @Override
    public final void markAsChanged() {
        isDirty = true;
    }

    public final void updateTick() {
        setChanged();
        if (isDirty) {
            var level = getLevel();
            if (level == null) return;
            GTNetwork.sendToAllPlayersTrackingChunk(level.getChunkAt(getBlockPos()), new SPacketUpdateBESyncValue(this));
            isDirty = false;
        }
    }
}
