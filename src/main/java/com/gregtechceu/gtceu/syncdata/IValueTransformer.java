package com.gregtechceu.gtceu.syncdata;

import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * Represents an object that provides a set of methods for encoding/decoding a value of type {@code <T>} into a {@link FriendlyByteBuf} or {@link Tag}
* */
public interface IValueTransformer<T> {

    void writeBufferPayload(FriendlyByteBuf buffer, T value);

    T readBufferPayload(FriendlyByteBuf buffer);

    Tag serializeNBT(T value);

    T deserializeNBT(Tag tag);
}
