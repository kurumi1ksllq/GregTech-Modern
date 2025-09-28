package com.gregtechceu.gtceu.api.pipenet;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.machine.TickableSubscription;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.common.blockentity.FluidPipeBlockEntity;
import com.gregtechceu.gtceu.common.blockentity.ItemPipeBlockEntity;
import com.gregtechceu.gtceu.syncdata.ISyncManaged;
import com.gregtechceu.gtceu.syncdata.ManagedSyncBlockEntity;
import com.gregtechceu.gtceu.syncdata.SyncDataHolder;
import com.gregtechceu.gtceu.syncdata.annotations.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.EmptyHandler;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PipeCoverContainer implements ICoverable, ISyncManaged {

    @Getter
    private final SyncDataHolder syncDataHolder = new SyncDataHolder(this);

    private final IPipeNode<?, ?> pipeTile;

    @SyncToClient
    @SaveField
    @CustomDataField
    @RerenderOnChanged
    private CoverBehavior up, down, north, south, west, east;

    public PipeCoverContainer(IPipeNode<?, ?> pipeTile) {
        this.pipeTile = pipeTile;
    }

    @Override
    public void markAsChanged() {
        if (pipeTile instanceof ManagedSyncBlockEntity syncBlockEntity) {
            syncBlockEntity.markAsChanged();
        }
    }

    @Override
    public Level getLevel() {
        return pipeTile.getPipeLevel();
    }

    @Override
    public BlockPos getPos() {
        return pipeTile.getPipePos();
    }

    @Override
    public BlockState getState() {
        return pipeTile.getState();
    }

    @Override
    public long getOffsetTimer() {
        return pipeTile.getOffsetTimer();
    }

    @Override
    public void notifyBlockUpdate() {
        pipeTile.notifyBlockUpdate();
    }

    @Override
    public void scheduleRenderUpdate() {
        pipeTile.scheduleRenderUpdate();
    }

    @Override
    public void scheduleNeighborShapeUpdate() {
        pipeTile.scheduleNeighborShapeUpdate();
    }

    @Override
    public boolean isInValid() {
        return pipeTile.isInValid();
    }

    @Override
    public boolean canPlaceCoverOnSide(CoverDefinition definition, Direction side) {
        return getCoverAtSide(side) == null;
    }

    @Override
    public double getCoverPlateThickness() {
        float thickness = pipeTile.getPipeType().getThickness();
        // no cover plate for pipes >= 1 block thick
        if (thickness >= 1) return 0;

        // If the available space for the cover is less than the regular cover plate thickness, use that

        // need to divide by 2 because thickness is centered on the block, so the space is half on each side of the pipe
        return Math.min(1.0 / 16.0, (1.0 - thickness) / 2);
    }

    @Override
    public Direction getFrontFacing() {
        return Direction.NORTH;
    }

    @Override
    public boolean shouldRenderBackSide() {
        return true;
    }

    @Nullable
    @Override
    public TickableSubscription subscribeServerTick(Runnable runnable) {
        return pipeTile.subscribeServerTick(runnable);
    }

    @Override
    public void unsubscribe(@Nullable TickableSubscription current) {
        pipeTile.unsubscribe(current);
    }

    @Override
    public IItemHandlerModifiable getItemHandlerCap(Direction side, boolean useCoverCapability) {
        if (pipeTile instanceof ItemPipeBlockEntity itemPipe) {
            return getLevel() instanceof ServerLevel ? itemPipe.getHandler(side, useCoverCapability) :
                    (IItemHandlerModifiable) EmptyHandler.INSTANCE;
        } else {
            return null;
        }
    }

    @Override
    public IFluidHandlerModifiable getFluidHandlerCap(@Nullable Direction side, boolean useCoverCapability) {
        if (pipeTile instanceof FluidPipeBlockEntity fluidPipe) {
            return fluidPipe.getTankList(side);
        } else {
            return null;
        }
    }

    @Override
    public CoverBehavior getCoverAtSide(Direction side) {
        return switch (side) {
            case UP -> up;
            case SOUTH -> south;
            case WEST -> west;
            case DOWN -> down;
            case EAST -> east;
            case NORTH -> north;
        };
    }

    public void setCoverAtSide(@Nullable CoverBehavior coverBehavior, Direction side) {
        var previousCover = getCoverAtSide(side);
        switch (side) {
            case UP -> up = coverBehavior;
            case SOUTH -> south = coverBehavior;
            case WEST -> west = coverBehavior;
            case DOWN -> down = coverBehavior;
            case EAST -> east = coverBehavior;
            case NORTH -> north = coverBehavior;
        }
        if (coverBehavior != null) {
            if (coverBehavior.canPipePassThrough()) {
                pipeTile.setConnection(side, true, false);
            }
        } else if (previousCover != null && previousCover.canPipePassThrough()) {
            pipeTile.setConnection(side, false, false);
        }
        getSyncDataHolder().resyncAllFields();
    }

    // Because cover behaviors have to be instantiated based on synced data, they need custom logic

    private CompoundTag serialiseCoverNBT(Direction side, CompoundTag tag, boolean saveClientData) {
        var compound = new CompoundTag();
        var cover = getCoverAtSide(side);
        if (cover == null) return compound;

        compound.putString("coverType", cover.coverDefinition.getId().toString());
        CompoundTag serialisedCover = cover.getSyncDataHolder().serializeNBT(saveClientData);
        compound.merge(serialisedCover);

        return compound;
    }

    private void deserialiseCoverNBT(Direction side, CompoundTag tag, boolean readClientData) {
        var cover = getCoverAtSide(side);
        if (tag.isEmpty()) {
            setCoverAtSide(null, side);
            return;
        }
        ResourceLocation coverType = ResourceLocation.tryParse(tag.getString("coverType"));
        if (cover == null || cover.coverDefinition.getId() != coverType) {
            setCoverAtSide(Objects.requireNonNull(GTRegistries.COVERS.get(coverType)).createCoverBehavior(this, side),
                    side);
        }

        Objects.requireNonNull(getCoverAtSide(side)).getSyncDataHolder().deserializeNBT(tag, readClientData);
    }

    @FieldDataModifier(fieldName = "up", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseUpCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.UP, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "up", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseUpCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.UP, tag, readClientData);
    }

    @FieldDataModifier(fieldName = "down", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseDownCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.DOWN, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "down", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseDownCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.DOWN, tag, readClientData);
    }

    @FieldDataModifier(fieldName = "north", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseNorthCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.NORTH, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "north", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseNorthCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.NORTH, tag, readClientData);
    }

    @FieldDataModifier(fieldName = "south", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseSouthCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.SOUTH, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "south", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseSouthCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.SOUTH, tag, readClientData);
    }

    @FieldDataModifier(fieldName = "east", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseEastCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.EAST, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "east", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseEastCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.EAST, tag, readClientData);
    }

    @FieldDataModifier(fieldName = "west", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseWestCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.WEST, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "west", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseWestCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.WEST, tag, readClientData);
    }
}
