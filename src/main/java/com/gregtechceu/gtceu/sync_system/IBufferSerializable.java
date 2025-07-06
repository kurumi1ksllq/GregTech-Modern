package com.gregtechceu.gtceu.sync_system;

import net.minecraft.network.FriendlyByteBuf;

public interface IBufferSerializable {

    void writeToBuffer(FriendlyByteBuf buf);

    void readFromBuffer(FriendlyByteBuf buf);
}
