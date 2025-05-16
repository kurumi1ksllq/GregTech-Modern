package com.gregtechceu.gtceu.common.network.packets.ui;

import com.gregtechceu.gtceu.api.mui.base.UIFactory;
import com.gregtechceu.gtceu.api.mui.factory.GuiData;
import com.gregtechceu.gtceu.api.mui.factory.GuiManager;
import com.gregtechceu.gtceu.utils.NetworkUtils;
import com.lowdragmc.lowdraglib.networking.IHandlerContext;
import com.lowdragmc.lowdraglib.networking.IPacket;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;

@NoArgsConstructor
@AllArgsConstructor
public class OpenGuiPacket<T extends GuiData> implements IPacket {

    private int windowId;
    private UIFactory<T> factory;
    private FriendlyByteBuf data;

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(this.windowId);
        buf.writeUtf(this.factory.getFactoryName());
        NetworkUtils.writeByteBuf(buf, this.data);
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        this.windowId = buf.readVarInt();
        this.factory = (UIFactory<T>) GuiManager.getFactory(buf.readUtf(32));
        this.data = NetworkUtils.readFriendlyByteBuf(buf);
    }

    @Override
    public void execute(IHandlerContext handler) {
        if (handler.isClient()) {
            GuiManager.open(this.windowId, this.factory, this.data, Minecraft.getInstance().player);
        }
    }
}
