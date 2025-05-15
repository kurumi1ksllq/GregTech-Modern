package com.gregtechceu.gtceu.api.mui.utils;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

public class KeyboardData {

    public final Dist side;
    public final int keyCode;
    public final int scanCode;
    public final int modifiers;

    public final boolean shift;
    public final boolean ctrl;
    public final boolean alt;

    public KeyboardData(Dist side, int keyCode, int scanCode, int modifiers) {
        this.side = side;
        this.keyCode = keyCode;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
        this.shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        this.ctrl = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        this.alt = (modifiers & GLFW.GLFW_MOD_ALT) != 0;
    }

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
        byte data = buffer.readByte();
        return new KeyboardData(Dist.DEDICATED_SERVER, keyCode, scanCode, modifiers);
    }

    @OnlyIn(Dist.CLIENT)
    public static KeyboardData create(int keyCode, int scanCode, int modifiers) {
        return new KeyboardData(Dist.CLIENT, keyCode, scanCode, modifiers);
    }
}
