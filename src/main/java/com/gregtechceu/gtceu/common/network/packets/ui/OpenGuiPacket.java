package com.gregtechceu.gtceu.common.network.packets.ui;

import com.gregtechceu.gtceu.api.mui.base.UIFactory;
import com.gregtechceu.gtceu.api.mui.factory.GuiData;
import com.gregtechceu.gtceu.api.mui.factory.GuiManager;
import com.gregtechceu.gtceu.utils.NetworkUtils;

import com.lowdragmc.lowdraglib.networking.IHandlerContext;
import com.lowdragmc.lowdraglib.networking.IPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

public class OpenGuiPacket<T extends GuiData> implements IPacket {

    private int windowId;
    private UIFactory<T> factory;
    private FriendlyByteBuf data;

    private final boolean shouldRelease;

    @SuppressWarnings("unused")
    public OpenGuiPacket() {
        // We are the owner of the buffer, release it when we are done.
        this.shouldRelease = true;
    }

    public OpenGuiPacket(int windowId, UIFactory<T> factory, FriendlyByteBuf data) {
        this.windowId = windowId;
        this.factory = factory;
        this.data = data;
        // We are not the owner of the buffer, don't release it, it might be reused.
        this.shouldRelease = false;
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.windowId);
        buf.writeResourceLocation(this.factory.getFactoryName());
        NetworkUtils.writeByteBuf(buf, this.data);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        this.windowId = buf.readVarInt();
        this.factory = (UIFactory<T>) GuiManager.getFactory(buf.readResourceLocation());
        this.data = NetworkUtils.readFriendlyByteBuf(buf, this.shouldRelease);
    }

    @Override
    public void execute(IHandlerContext handler) {
        if (handler.isClient()) {
            GuiManager.open(this.windowId, this.factory, this.data, Minecraft.getInstance().player);
        }
    }
}
