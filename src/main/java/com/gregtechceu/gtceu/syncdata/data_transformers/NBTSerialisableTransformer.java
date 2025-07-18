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
