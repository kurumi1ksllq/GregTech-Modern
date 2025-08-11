package com.gregtechceu.gtceu.syncdata;

import net.minecraft.nbt.Tag;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an object that provides a set of methods for encoding/decoding a value of type {@code <T>} into a
 * {@link Tag}
 */
public interface IValueTransformer<T> {

    default boolean mustProvideObject() {
        return false;
    }

    void writeToBuffer(T value, FriendlyByteBuf buf);

    T readFromBuffer(FriendlyByteBuf buf, T currentValue);

    default Tag serializeClientChunkPayload(T value) {
        return serializeNBT(value);
    }

    default T deserializeClientChunkPayload(Tag tag, @Nullable T currentVal) {
        return deserializeNBT(tag, currentVal);
    }

    Tag serializeNBT(T value);

    T deserializeNBT(Tag tag, @Nullable T currentVal);
}
