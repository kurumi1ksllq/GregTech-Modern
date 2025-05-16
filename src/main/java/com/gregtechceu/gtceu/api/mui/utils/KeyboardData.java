package com.gregtechceu.gtceu.api.mui.utils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public record KeyboardData(Dist side, int keyCode, int scanCode, int modifiers) {

    public boolean isClient() {
        return this.side.isClient();
    }

    public void writeToPacket(FriendlyByteBuf buffer) {
        buffer.writeVarInt(this.keyCode);
        buffer.writeVarInt(this.scanCode);
        buffer.writeVarInt(this.modifiers);
    }

    public static KeyboardData readPacket(FriendlyByteBuf buffer) {
        int keyCode = buffer.readVarInt();
        int scanCode = buffer.readVarInt();
        int modifiers = buffer.readVarInt();
        return new KeyboardData(Dist.DEDICATED_SERVER, keyCode, scanCode, modifiers);
    }

    @OnlyIn(Dist.CLIENT)
    public static KeyboardData create(int keyCode, int scanCode, int modifiers) {
        return new KeyboardData(Dist.CLIENT, keyCode, scanCode, modifiers);
    }
}
