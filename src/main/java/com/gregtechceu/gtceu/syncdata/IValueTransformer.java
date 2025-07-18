package com.gregtechceu.gtceu.syncdata;

import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;

import org.jetbrains.annotations.Nullable;

/**
 * Represents an object that provides a set of methods for encoding/decoding a value of type {@code <T>} into a {@link Tag}
 */
public interface IValueTransformer<T> {

    default boolean mustProvideObject() {
        return false;
    }

    void writeBufferPayload(FriendlyByteBuf buffer, T value);

    T readBufferPayload(FriendlyByteBuf buffer, @Nullable T currentVal);

    default Tag getClientSyncNBT(T value, boolean fullSync) {
        return serializeNBT(value);
    }

    default T loadClientSyncNBT(Tag tag, @Nullable T currentVal) {
        return deserializeNBT(tag, currentVal);
    }

    Tag serializeNBT(T value);

    T deserializeNBT(Tag tag, @Nullable T currentVal);
}
