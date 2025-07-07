package com.gregtechceu.gtceu.syncdata;

import lombok.Getter;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;


/**
 * A BlockEntity that manages sync and save data via the {@code ISyncManaged} syncdata system.
 * @see ISyncManaged
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public abstract class ManagedSyncBlockEntity extends BlockEntity implements ISyncManaged {

    @Getter
    protected final SyncDataHolder syncDataHolder = new SyncDataHolder(this);


    public ManagedSyncBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, pos, blockState);
    }

    // Called when this BlockEntity is saved or loaded

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
    }

    // Called to init clientside BlockEntities on chunk load

    @Override
    public CompoundTag getUpdateTag() {
        return super.getUpdateTag();
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
    }

    @Override
    public void onChanged() {
        var level = getLevel();
        if (level != null && !level.isClientSide && level.getServer() != null) {
            level.getServer().execute(this::setChanged);
        }
    }
}
