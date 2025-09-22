package com.gregtechceu.gtceu.api.machine;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.syncdata.ISyncManaged;
import com.gregtechceu.gtceu.syncdata.SyncDataHolder;
import com.gregtechceu.gtceu.syncdata.annotations.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.items.IItemHandlerModifiable;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;

public class MachineCoverContainer implements ICoverable, ISyncManaged {

    @Getter
    private final SyncDataHolder syncDataHolder = new SyncDataHolder(this);
    @Getter
    private final MetaMachine machine;
    @SyncToClient
    @SaveField
    @RerenderOnChanged
    @CustomDataField
    private CoverBehavior up, down, north, south, west, east;

    public MachineCoverContainer(MetaMachine machine) {
        this.machine = machine;
    }

    @Override
    public void markAsChanged() {
        machine.markAsChanged();
    }

    @Override
    public BlockState getState() {
        return machine.getBlockState();
    }

    @Override
    public Level getLevel() {
        return machine.getLevel();
    }

    @Override
    public BlockPos getPos() {
        return machine.getPos();
    }

    @Override
    public long getOffsetTimer() {
        return machine.getOffsetTimer();
    }

    @Override
    public void notifyBlockUpdate() {
        machine.notifyBlockUpdate();
    }

    @Override
    public void scheduleRenderUpdate() {
        machine.scheduleRenderUpdate();
    }

    @Override
    public void scheduleNeighborShapeUpdate() {
        machine.scheduleNeighborShapeUpdate();
    }

    @Override
    public boolean isInValid() {
        return machine.isInValid();
    }

    @Override
    public boolean canPlaceCoverOnSide(CoverDefinition definition, Direction side) {
        ArrayList<VoxelShape> collisionList = new ArrayList<>();
        machine.addCollisionBoundingBox(collisionList);
        // noinspection RedundantIfStatement
        if (ICoverable.doesCoverCollide(side, collisionList, getCoverPlateThickness())) {
            // cover collision box overlaps with meta tile entity collision box
            return false;
        }

        return true;
    }

    @Override
    public double getCoverPlateThickness() {
        return 0;
    }

    @Override
    public Direction getFrontFacing() {
        return machine.getFrontFacing();
    }

    @Override
    public boolean shouldRenderBackSide() {
        return !machine.getBlockState().canOcclude();
    }

    @Nullable
    @Override
    public TickableSubscription subscribeServerTick(Runnable runnable) {
        return machine.subscribeServerTick(runnable);
    }

    @Override
    public void unsubscribe(@Nullable TickableSubscription current) {
        machine.unsubscribe(current);
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

    @Override
    public void setCoverAtSide(@Nullable CoverBehavior coverBehavior, Direction side) {
        switch (side) {
            case UP -> up = coverBehavior;
            case SOUTH -> south = coverBehavior;
            case WEST -> west = coverBehavior;
            case DOWN -> down = coverBehavior;
            case EAST -> east = coverBehavior;
            case NORTH -> north = coverBehavior;
        }
    }

    @Override
    public IItemHandlerModifiable getItemHandlerCap(@Nullable Direction side, boolean useCoverCapability) {
        return machine.getItemHandlerCap(side, useCoverCapability);
    }

    @Override
    public IFluidHandlerModifiable getFluidHandlerCap(@Nullable Direction side, boolean useCoverCapability) {
        return machine.getFluidHandlerCap(side, useCoverCapability);
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

    private void serialiseCoverBuf(Direction side, FriendlyByteBuf buf) {
        var cover = getCoverAtSide(side);
        buf.writeBoolean(cover == null);
        if (cover == null) return;
        buf.writeResourceLocation(cover.coverDefinition.getId());
        cover.getSyncDataHolder().writeToNetworkBuffer(buf);
    }

    private void deserialiseCoverNBT(Direction side, CompoundTag tag, boolean readClientData) {
        var cover = getCoverAtSide(side);
        if (tag.isEmpty() || tag.getString("coverType").isEmpty()) {
            setCoverAtSide(null, side);
            return;
        }
        ResourceLocation coverType = ResourceLocation.tryParse(tag.getString("coverType"));
        if (cover == null || cover.coverDefinition.getId() != coverType) {
            var coverReg = GTRegistries.COVERS.get(coverType);
            if (coverReg == null) {
                GTCEu.LOGGER.error("Error during NBT load: unknown cover type {} ({})", coverType, tag.getString("coverType"));
                return;
            }
            setCoverAtSide(coverReg.createCoverBehavior(this, side), side);
        }

        Objects.requireNonNull(getCoverAtSide(side)).getSyncDataHolder().deserializeNBT(tag, readClientData);

    }

    private void deserialiseCoverBuf(Direction side, FriendlyByteBuf buf) {
        var cover = getCoverAtSide(side);
        if (buf.readBoolean()) {
            setCoverAtSide(null, side);
            return;
        }
        ResourceLocation coverType = buf.readResourceLocation();
        if (cover == null || cover.coverDefinition.getId() != coverType) {
            var coverReg = GTRegistries.COVERS.get(coverType);
            if (coverReg == null) {
                GTCEu.LOGGER.error("Error during network buffer read: unknown cover type {}", coverType);
                return;
            }
            setCoverAtSide(coverReg.createCoverBehavior(this, side), side);
        }

        Objects.requireNonNull(getCoverAtSide(side)).getSyncDataHolder().readFromNetworkBuffer(buf);
    }

    @FieldDataModifier(fieldName = "up", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseUpCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.UP, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "up", target = FieldDataModifier.ModifyTarget.WRITE_BUF)
    private void serialiseUpCoverBuf(FriendlyByteBuf buf) {
        serialiseCoverBuf(Direction.UP, buf);
    }

    @FieldDataModifier(fieldName = "up", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseUpCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.UP, tag, readClientData);
    }

    @FieldDataModifier(fieldName = "up", target = FieldDataModifier.ModifyTarget.READ_BUF)
    private void deserialiseUpCoverBuf(FriendlyByteBuf buf) {
        deserialiseCoverBuf(Direction.UP, buf);
    }

    @FieldDataModifier(fieldName = "down", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseDownCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.DOWN, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "down", target = FieldDataModifier.ModifyTarget.WRITE_BUF)
    private void serialiseDownCoverBuf(FriendlyByteBuf buf) {
        serialiseCoverBuf(Direction.DOWN, buf);
    }

    @FieldDataModifier(fieldName = "down", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseDownCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.DOWN, tag, readClientData);
    }

    @FieldDataModifier(fieldName = "down", target = FieldDataModifier.ModifyTarget.READ_BUF)
    private void deserialiseDownCoverBuf(FriendlyByteBuf buf) {
        deserialiseCoverBuf(Direction.DOWN, buf);
    }

    @FieldDataModifier(fieldName = "north", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseNorthCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.NORTH, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "north", target = FieldDataModifier.ModifyTarget.WRITE_BUF)
    private void serialiseNorthCoverBuf(FriendlyByteBuf buf) {
        serialiseCoverBuf(Direction.NORTH, buf);
    }

    @FieldDataModifier(fieldName = "north", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseNorthCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.NORTH, tag, readClientData);
    }

    @FieldDataModifier(fieldName = "north", target = FieldDataModifier.ModifyTarget.READ_BUF)
    private void deserialiseNorthCoverBuf(FriendlyByteBuf buf) {
        deserialiseCoverBuf(Direction.NORTH, buf);
    }

    @FieldDataModifier(fieldName = "south", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseSouthCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.SOUTH, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "south", target = FieldDataModifier.ModifyTarget.WRITE_BUF)
    private void serialiseSouthCoverBuf(FriendlyByteBuf buf) {
        serialiseCoverBuf(Direction.SOUTH, buf);
    }

    @FieldDataModifier(fieldName = "south", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseSouthCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.SOUTH, tag, readClientData);
    }

    @FieldDataModifier(fieldName = "south", target = FieldDataModifier.ModifyTarget.READ_BUF)
    private void deserialiseSouthCoverBuf(FriendlyByteBuf buf) {
        deserialiseCoverBuf(Direction.SOUTH, buf);
    }

    @FieldDataModifier(fieldName = "east", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseEastCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.EAST, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "east", target = FieldDataModifier.ModifyTarget.WRITE_BUF)
    private void serialiseEastCoverBuf(FriendlyByteBuf buf) {
        serialiseCoverBuf(Direction.EAST, buf);
    }

    @FieldDataModifier(fieldName = "east", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseEastCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.EAST, tag, readClientData);
    }

    @FieldDataModifier(fieldName = "east", target = FieldDataModifier.ModifyTarget.READ_BUF)
    private void deserialiseEastCoverBuf(FriendlyByteBuf buf) {
        deserialiseCoverBuf(Direction.EAST, buf);
    }

    @FieldDataModifier(fieldName = "west", target = FieldDataModifier.ModifyTarget.SAVE_NBT)
    private CompoundTag serialiseWestCoverNBT(CompoundTag tag, boolean saveClientData) {
        return serialiseCoverNBT(Direction.WEST, tag, saveClientData);
    }

    @FieldDataModifier(fieldName = "west", target = FieldDataModifier.ModifyTarget.WRITE_BUF)
    private void serialiseWestCoverBuf(FriendlyByteBuf buf) {
        serialiseCoverBuf(Direction.WEST, buf);
    }

    @FieldDataModifier(fieldName = "west", target = FieldDataModifier.ModifyTarget.LOAD_NBT)
    private void deserialiseWestCoverNBT(CompoundTag tag, boolean readClientData) {
        deserialiseCoverNBT(Direction.WEST, tag, readClientData);
    }

    @FieldDataModifier(fieldName = "west", target = FieldDataModifier.ModifyTarget.READ_BUF)
    private void deserialiseWestCoverBuf(FriendlyByteBuf buf) {
        deserialiseCoverBuf(Direction.WEST, buf);
    }
}
