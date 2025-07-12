package com.gregtechceu.gtceu.syncdata.data_transformers;

import com.gregtechceu.gtceu.syncdata.ISyncManaged;
import com.gregtechceu.gtceu.syncdata.IValueTransformer;

import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

public class SyncManagedTransformer<T extends ISyncManaged> implements IValueTransformer<T> {

    @Override
    public void writeBufferPayload(FriendlyByteBuf buffer, T value) {}

    @Override
    public T readBufferPayload(FriendlyByteBuf buffer, T currentVal) {
        return null;
    }

    @Override
    public Tag serializeNBT(T value) {
        return null;
    }

    @Override
    public T deserializeNBT(Tag tag, T currentVal) {
        return null;
    }
}
