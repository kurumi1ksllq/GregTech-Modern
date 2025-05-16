package com.gregtechceu.gtceu.api.mui.utils.serialization;

import net.minecraft.network.FriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public interface IByteBufAdapter<T> extends IByteBufSerializer<T>, IByteBufDeserializer<T>, IEquals<T> {

    @Override
    T deserialize(FriendlyByteBuf buffer);

    @Override
    void serialize(FriendlyByteBuf buffer, T u);

    @Override
    boolean areEqual(@NotNull T t1, @NotNull T t2);
}
