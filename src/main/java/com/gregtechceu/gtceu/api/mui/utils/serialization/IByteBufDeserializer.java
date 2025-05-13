package com.gregtechceu.gtceu.api.mui.utils.serialization;

import com.gregtechceu.gtceu.api.mui.ModularUI;
import net.minecraft.network.FriendlyByteBuf;

import java.io.IOException;

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
     * @throws IOException if reading failed
     */
    T deserialize(FriendlyByteBuf buffer) throws IOException;

    default T deserializeSafe(FriendlyByteBuf buffer) {
        try {
            return deserialize(buffer);
        } catch (IOException e) {
            GTCEu.LOGGER.catching(e);
            return null;
        }
    }
}
