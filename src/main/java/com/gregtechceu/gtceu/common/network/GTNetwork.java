package com.gregtechceu.gtceu.common.network;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.common.network.packets.*;
import com.gregtechceu.gtceu.common.network.packets.hazard.SPacketAddHazardZone;
import com.gregtechceu.gtceu.common.network.packets.hazard.SPacketRemoveHazardZone;
import com.gregtechceu.gtceu.common.network.packets.hazard.SPacketSyncHazardZoneStrength;
import com.gregtechceu.gtceu.common.network.packets.hazard.SPacketSyncLevelHazards;
import com.gregtechceu.gtceu.common.network.packets.prospecting.SPacketProspectBedrockFluid;
import com.gregtechceu.gtceu.common.network.packets.prospecting.SPacketProspectBedrockOre;
import com.gregtechceu.gtceu.common.network.packets.prospecting.SPacketProspectOre;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public class GTNetwork {

    private static final String PROTOCOL_VERSION = "1.0.0";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(GTCEu.id("network"),
            () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);

    private static int nextPacketId = 0;

    public interface INetPacket {

        void encode(FriendlyByteBuf buffer);

        void decode(FriendlyByteBuf buffer);

        void execute(NetworkEvent.Context context);
    }

    public static <T extends INetPacket> void register(Class<T> cls, NetworkDirection direction) {
        INSTANCE.registerMessage(nextPacketId++, cls, INetPacket::encode, (buf) -> {
            try {
                var p = cls.getDeclaredConstructor().newInstance();
                p.decode(buf);
                return p;
            } catch (InvocationTargetException | NoSuchMethodException | InstantiationException |
                     IllegalAccessException e) {
                e.printStackTrace();
                return null;
            }
        }, (msg, ctx) -> {
            ctx.get().enqueueWork(() -> msg.execute(ctx.get()));
            ctx.get().setPacketHandled(true);
        }, Optional.ofNullable(direction));
    }

    public static void init() {
        register(CPacketKeysPressed.class, NetworkDirection.PLAY_TO_SERVER);
        register(SPacketSyncOreVeins.class, NetworkDirection.PLAY_TO_CLIENT);
        register(SPacketSyncFluidVeins.class, NetworkDirection.PLAY_TO_CLIENT);
        register(SPacketSyncBedrockOreVeins.class, NetworkDirection.PLAY_TO_CLIENT);

        register(SPacketAddHazardZone.class, NetworkDirection.PLAY_TO_CLIENT);
        register(SPacketRemoveHazardZone.class, NetworkDirection.PLAY_TO_CLIENT);
        register(SPacketSyncHazardZoneStrength.class, NetworkDirection.PLAY_TO_CLIENT);
        register(SPacketSyncLevelHazards.class, NetworkDirection.PLAY_TO_CLIENT);
        register(SPacketProspectOre.class, NetworkDirection.PLAY_TO_CLIENT);
        register(SPacketProspectBedrockOre.class, NetworkDirection.PLAY_TO_CLIENT);
        register(SPacketProspectBedrockFluid.class, NetworkDirection.PLAY_TO_CLIENT);
        register(SPacketSendWorldID.class, NetworkDirection.PLAY_TO_CLIENT);
        register(SPacketNotifyCapeChange.class, NetworkDirection.PLAY_TO_CLIENT);

        register(SCPacketShareProspection.class, null);
    }
}
