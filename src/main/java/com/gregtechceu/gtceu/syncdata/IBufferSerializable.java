package com.gregtechceu.gtceu.syncdata;

import net.minecraft.network.FriendlyByteBuf;

public interface IBufferSerializable {

    void writeToBuffer(FriendlyByteBuf buf);

    void readFromBuffer(FriendlyByteBuf buf);
}
