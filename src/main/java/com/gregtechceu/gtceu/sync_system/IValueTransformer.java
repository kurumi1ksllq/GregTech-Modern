package com.gregtechceu.gtceu.sync_system;

import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

public interface IValueTransformer<T> {

    void writeBufferPayload(FriendlyByteBuf buffer, T value);

    T readBufferPayload(FriendlyByteBuf buffer);

    Tag serializeNBT(T value);

    T deserializeNBT(Tag tag);
}
