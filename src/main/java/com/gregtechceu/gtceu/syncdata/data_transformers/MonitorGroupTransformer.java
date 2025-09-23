package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.monitor.MonitorGroup;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

public class MonitorGroupTransformer implements IValueTransformer<MonitorGroup> {

    @Override
    public void writeToBuffer(MonitorGroup value, FriendlyByteBuf buf) {
        buf.writeNbt(serializeNBT(value));
    }

    @Override
    public MonitorGroup readFromBuffer(FriendlyByteBuf buf, MonitorGroup currentValue) {
        return deserializeNBT(buf.readNbt(), null);
    }

    @Override
    public CompoundTag serializeNBT(MonitorGroup value) {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", value.getName());
        ListTag list = new ListTag();
        value.getRelativePositions().forEach(pos -> {
            list.add(NbtUtils.writeBlockPos(pos));
        });
        if (value.getTargetRaw() != null) {
            tag.put("targetPos", NbtUtils.writeBlockPos(value.getTargetRaw()));
            if (value.getTargetCoverSide() != null) {
                tag.putString("targetSide", value.getTargetCoverSide().getSerializedName());
            }
        }
        tag.put("positions", list);
        tag.putInt("dataSlot", value.getDataSlot());
        tag.put("items", value.getItemStackHandler().serializeNBT());
        tag.put("placeholderSlots", value.getPlaceholderSlotsHandler().serializeNBT());
        return tag;

    }

    @Override
    public MonitorGroup deserializeNBT(Tag tag, @Nullable MonitorGroup currentVal) {
        if (tag instanceof CompoundTag compoundTag) {
            CustomItemStackHandler handler = new CustomItemStackHandler(),
                    placeholderSlotsHandler = new CustomItemStackHandler();
            handler.deserializeNBT(compoundTag.getCompound("items"));
            placeholderSlotsHandler.deserializeNBT(compoundTag.getCompound("placeholderSlots"));
            var group = new MonitorGroup(compoundTag.getString("name"), handler, placeholderSlotsHandler);
            ListTag list = compoundTag.getList("positions", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                group.add(NbtUtils.readBlockPos(list.getCompound(i)));
            }
            if (compoundTag.contains("targetPos", Tag.TAG_COMPOUND)) {
                group.setTarget(NbtUtils.readBlockPos(compoundTag.getCompound("targetPos")));
                if (compoundTag.contains("targetSide", Tag.TAG_STRING)) {
                    group.setTargetCoverSide(Direction.byName(compoundTag.getString("targetSide")));
                }
                if (compoundTag.contains("dataSlot", Tag.TAG_INT)) {
                    group.setDataSlot(compoundTag.getInt("dataSlot"));
                }
            }
            return group;
        }
        return null;
    }
}
