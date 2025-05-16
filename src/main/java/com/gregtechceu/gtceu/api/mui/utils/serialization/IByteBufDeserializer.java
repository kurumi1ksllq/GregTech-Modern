package com.gregtechceu.gtceu.api.mui.utils.serialization;

import net.minecraft.network.FriendlyByteBuf;

/**
 * A function to read an object from a {@link FriendlyByteBuf}.
 *
 * @param <T> object type
 */
public interface IByteBufDeserializer<T> {

    /**
     * Reads the object from the buffer.
     *
     * @param buffer buffer to read from
     * @return the read object
     */
    T deserialize(FriendlyByteBuf buffer);
}
