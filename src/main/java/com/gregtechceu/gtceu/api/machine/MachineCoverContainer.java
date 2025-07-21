package com.gregtechceu.gtceu.api.machine;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.client.renderer.cover.CoverRendererPackage;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.gui.editor.runtime.PersistedParser;
import com.lowdragmc.lowdraglib.syncdata.IEnhancedManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.*;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.*;

public class MachineCoverContainer implements ICoverable, IEnhancedManaged {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(MachineCoverContainer.class);
    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    @Getter
    private final MetaMachine machine;
    @DescSynced
    @Persisted
    @RequireRerender
    @ReadOnlyManaged(onDirtyMethod = "onCoversDirty",
                     serializeMethod = "serializeCovers",
                     deserializeMethod = "deserializeCovers")
    private final EnumMap<Direction, CoverBehavior> covers = new EnumMap<>(Direction.class);

    private final int[] sidedRedstoneInput = new int[6];

    public MachineCoverContainer(MetaMachine machine) {
        this.machine = machine;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onChanged() {
        var level = getLevel();
        if (level != null && !level.isClientSide && level.getServer() != null) {
            level.getServer().execute(this::markDirty);
        }
    }

    @Override
    public void onLoad() {
        ICoverable.super.onLoad();
        if (getLevel() instanceof ServerLevel serverLevel) {
            serverLevel.getServer().tell(new TickTask(0, () -> {
                for (Direction side : GTUtil.DIRECTIONS) {
                    this.sidedRedstoneInput[side.get3DDataValue()] = GTUtil.getRedstonePower(getLevel(), getPos(),
                            side);
                }
            }));
        }
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
    public void markDirty() {
        machine.markDirty();
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
    public boolean acceptsCovers() {
        return covers.size() < GTUtil.DIRECTIONS.length;
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
    public @Nullable CoverBehavior getCoverAtSide(Direction side) {
        return covers.get(side);
    }

    @Override
    public @NotNull @UnmodifiableView Collection<CoverBehavior> getAttachedCovers() {
        return Collections.unmodifiableCollection(covers.values());
    }

    @Override
    public @Nullable BlockEntity getNeighbor(@NotNull Direction side) {
        return machine.getNeighbor(side);
    }

    @Override
    public int getInputRedstoneSignal(@NotNull Direction side, boolean ignoreCover) {
        if (!ignoreCover && getCoverAtSide(side) != null) {
            return 0; // covers block input redstone signal for machine
        }
        return sidedRedstoneInput[side.get3DDataValue()];
    }

    public void updateInputRedstoneSignals() {
        for (Direction side : GTUtil.DIRECTIONS) {
            int redstoneValue = GTUtil.getRedstonePower(getLevel(), getPos(), side);
            int currentValue = sidedRedstoneInput[side.get3DDataValue()];
            if (redstoneValue != currentValue) {
                this.sidedRedstoneInput[side.get3DDataValue()] = redstoneValue;
                CoverBehavior cover = getCoverAtSide(side);
                if (cover != null) {
                    cover.onRedstoneInputSignalChange(redstoneValue);
                }
            }
        }
    }

    @Override
    public void setCoverAtSide(@Nullable CoverBehavior coverBehavior, Direction side) {
        covers.put(side, coverBehavior);
        if (coverBehavior != null) {
            if (!getLevel().isClientSide) {
                // do not sync or handle logic on client side
                coverBehavior.getSyncStorage().markAllDirty();
            }

            machine.notifyBlockUpdate();
            machine.markDirty();
        }
    }

    @Override
    public @Nullable IItemHandler getItemHandlerCap(@Nullable Direction side, boolean useCoverCapability) {
        return machine.getItemHandlerCap(side, useCoverCapability);
    }

    @Override
    public @Nullable IFluidHandler getFluidHandlerCap(@Nullable Direction side, boolean useCoverCapability) {
        return machine.getFluidHandlerCap(side, useCoverCapability);
    }

    @SuppressWarnings("unused")
    private boolean onCoversDirty(EnumMap<Direction, CoverBehavior> covers) {
        for (CoverBehavior coverBehavior : covers.values()) {
            if (coverBehavior != null) {
                for (IRef ref : coverBehavior.getSyncStorage().getNonLazyFields()) {
                    ref.update();
                }
                if (coverBehavior.getSyncStorage().hasDirtySyncFields() ||
                        coverBehavior.getSyncStorage().hasDirtyPersistedFields()) {
                    return true;
                }
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    private CompoundTag serializeCovers(EnumMap<Direction, CoverBehavior> covers) {
        CompoundTag tagCompound = new CompoundTag();
        ListTag coversList = new ListTag();
        for (var entry : covers.entrySet()) {
            CoverBehavior cover = entry.getValue();
            if (cover != null) {
                CompoundTag tag = new CompoundTag();
                ResourceLocation coverId = cover.coverDefinition.getId();
                tag.putString("id", coverId.toString());
                tag.putByte("side", (byte) entry.getKey().get3DDataValue());
                PersistedParser.serializeNBT(tag, cover.getClass(), cover);
                coversList.add(tag);
            }
        }
        tagCompound.put("covers", coversList);
        return tagCompound;
    }

    @SuppressWarnings("unused")
    private EnumMap<Direction, CoverBehavior> deserializeCovers(CompoundTag tagCompound) {
        EnumMap<Direction, CoverBehavior> map = new EnumMap<>(Direction.class);

        if (tagCompound.contains("covers", Tag.TAG_LIST)) {
            ListTag coversList = tagCompound.getList("covers", Tag.TAG_COMPOUND);
            for (int index = 0; index < coversList.size(); index++) {
                CompoundTag tag = coversList.getCompound(index);
                deserializeCover(tag, map);
            }
        } else {
            // backwards compat with the old serialization logic (which saved the covers into separate keys)
            for (Direction direction : GTUtil.DIRECTIONS) {
                if (!tagCompound.contains(direction.getSerializedName(), Tag.TAG_COMPOUND)) {
                    continue;
                }
                CompoundTag tag = tagCompound.getCompound(direction.getSerializedName());
                deserializeCover(tag, map);
            }
        }
        return map;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction arg) {
        // FIXME
        return LazyOptional.empty();
    }

    @OnlyIn(Dist.CLIENT)
    public CoverRendererPackage createPackage() {
        if (covers.isEmpty()) return CoverRendererPackage.EMPTY;
        CoverRendererPackage rendererPackage = new CoverRendererPackage(false);
        for (var cover : covers.entrySet()) {
            rendererPackage.addRenderer(cover.getValue().getCoverRenderer().get(), cover.getKey());
            rendererPackage.addModelData(cover.getKey(), cover.getValue().getModelData());
        }
        return rendererPackage;
    }
}
