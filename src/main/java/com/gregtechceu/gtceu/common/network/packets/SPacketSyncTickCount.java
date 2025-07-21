package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.utils.TickTracker;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class SPacketSyncTickCount implements GTNetwork.INetPacket {

    private final int serverTickCount;

    public SPacketSyncTickCount() {
        serverTickCount = TickTracker.getTick();
    }

    public SPacketSyncTickCount(FriendlyByteBuf buf) {
        this.serverTickCount = buf.readVarInt();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(serverTickCount);
    }

    @Override
    public void execute(NetworkEvent.Context context) {
        TickTracker.setClientTick(serverTickCount);
    }
}
