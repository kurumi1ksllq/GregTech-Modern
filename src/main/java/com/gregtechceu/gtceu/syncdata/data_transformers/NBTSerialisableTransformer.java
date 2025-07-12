package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.common.util.INBTSerializable;

public class NBTSerialisableTransformer implements IValueTransformer<INBTSerializable<Tag>> {

    @Override
    public boolean mustProvideObject() {
        return true;
    }

    @Override
    public void writeBufferPayload(FriendlyByteBuf buffer, INBTSerializable<Tag> value) {
        var tag = new CompoundTag();
        tag.put("v", value.serializeNBT());
        buffer.writeNbt(tag);
    }

    @Override
    public INBTSerializable<Tag> readBufferPayload(FriendlyByteBuf buffer, INBTSerializable<Tag> currentVal) {
        if (currentVal == null) return null;
        var val = buffer.readNbt();
        if (val != null) currentVal.deserializeNBT(val.get("v"));
        return currentVal;
    }

    @Override
    public Tag serializeNBT(INBTSerializable<Tag> value) {
        return value.serializeNBT();
    }

    @Override
    public INBTSerializable<Tag> deserializeNBT(Tag tag, INBTSerializable<Tag> currentVal) {
        if (currentVal == null) return null;
        currentVal.deserializeNBT(tag);
        return currentVal;
    }
}
