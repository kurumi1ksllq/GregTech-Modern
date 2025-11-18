package com.gregtechceu.gtceu.common.network;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.common.network.packets.*;
import com.gregtechceu.gtceu.common.network.packets.hazard.*;
import com.gregtechceu.gtceu.common.network.packets.prospecting.SPacketProspectBedrockFluid;
import com.gregtechceu.gtceu.common.network.packets.prospecting.SPacketProspectBedrockOre;
import com.gregtechceu.gtceu.common.network.packets.prospecting.SPacketProspectOre;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class GTNetwork {

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registar = event.registrar(GTCEuAPI.NETWORK_VERSION);
        // spotless:off
        registar.playToServer(CPacketKeysPressed.TYPE, CPacketKeysPressed.CODEC, CPacketKeysPressed::execute);
        registar.playToClient(SPacketNotifyCapeChange.TYPE, SPacketNotifyCapeChange.CODEC, SPacketNotifyCapeChange::execute);

        registar.playToClient(SPacketAddHazardZone.TYPE, SPacketAddHazardZone.CODEC, SPacketAddHazardZone::execute);
        registar.playToClient(SPacketRemoveHazardZone.TYPE, SPacketRemoveHazardZone.CODEC, SPacketRemoveHazardZone::execute);
        registar.playToClient(SPacketSyncHazardZoneStrength.TYPE, SPacketSyncHazardZoneStrength.CODEC, SPacketSyncHazardZoneStrength::execute);
        registar.playToClient(SPacketSyncLevelHazards.TYPE, SPacketSyncLevelHazards.CODEC, SPacketSyncLevelHazards::execute);

// <<<<<<< HEAD
        registar.playToClient(SPacketProspectOre.TYPE, SPacketProspectOre.CODEC, SPacketProspectOre::execute);
        registar.playToClient(SPacketProspectBedrockFluid.TYPE, SPacketProspectBedrockFluid.CODEC, SPacketProspectBedrockFluid::execute);
        registar.playToClient(SPacketProspectBedrockOre.TYPE, SPacketProspectBedrockOre.CODEC, SPacketProspectBedrockOre::execute);
        registar.playToClient(SPacketSendWorldID.TYPE, SPacketSendWorldID.CODEC, SPacketSendWorldID::execute);
        registar.playBidirectional(SCPacketShareProspection.TYPE, SCPacketShareProspection.CODEC, SCPacketShareProspection::execute);
        
    }
    
        // spotless:on
// =======
//     public static void sendToServer(INetPacket packet) {
//         INSTANCE.sendToServer(packet);
//     }

//     public static void sendToPlayersInLevel(ResourceKey<Level> level, INetPacket packet) {
//         INSTANCE.send(PacketDistributor.DIMENSION.with(() -> level), packet);
//     }

//     public static void sendToPlayersNearPoint(PacketDistributor.TargetPoint point, INetPacket packet) {
//         INSTANCE.send(PacketDistributor.NEAR.with(() -> point), packet);
//     }

//     public static void sendToAllPlayersTrackingEntity(Entity entity, boolean includeSelf, INetPacket packet) {
//         INSTANCE.send(includeSelf ? PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity) :
//                 PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
//     }

//     public static void sendToAllPlayersTrackingChunk(LevelChunk chunk, INetPacket packet) {
//         INSTANCE.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), packet);
//     }

//     public static void sendToAll(INetPacket packet) {
//         INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
//     }

//     public static void sendToPlayer(ServerPlayer player, INetPacket packet) {
//         INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
//     }

//     public static void reply(NetworkEvent.Context context, INetPacket packet) {
//         INSTANCE.reply(packet, context);
//     }

//     public interface INetPacket {

//         void encode(FriendlyByteBuf buffer);

//         void execute(NetworkEvent.Context context);
//     }

//     public static <T extends INetPacket> void register(Class<T> cls, Function<FriendlyByteBuf, T> decode,
//                                                        NetworkDirection direction) {
//         INSTANCE.registerMessage(nextPacketId++, cls, INetPacket::encode, decode, (msg, ctx) -> {
//             ctx.get().enqueueWork(() -> msg.execute(ctx.get()));
//             ctx.get().setPacketHandled(true);
//         }, Optional.ofNullable(direction));
//     }

//     public static void init() {
//         register(SCPacketMonitorGroupNBTChange.class, SCPacketMonitorGroupNBTChange::new, null);
//         register(CPacketImageRequest.class, CPacketImageRequest::new, NetworkDirection.PLAY_TO_SERVER);
//         register(SPacketImageResponse.class, SPacketImageResponse::new, NetworkDirection.PLAY_TO_CLIENT);

//         register(CPacketKeysPressed.class, CPacketKeysPressed::new, NetworkDirection.PLAY_TO_SERVER);
//         register(CPacketKeyDown.class, CPacketKeyDown::new, NetworkDirection.PLAY_TO_SERVER);

//         register(SPacketSyncOreVeins.class, SPacketSyncOreVeins::new, NetworkDirection.PLAY_TO_CLIENT);
//         register(SPacketSyncFluidVeins.class, SPacketSyncFluidVeins::new, NetworkDirection.PLAY_TO_CLIENT);
//         register(SPacketSyncBedrockOreVeins.class, SPacketSyncBedrockOreVeins::new, NetworkDirection.PLAY_TO_CLIENT);

//         register(SPacketAddHazardZone.class, SPacketAddHazardZone::new, NetworkDirection.PLAY_TO_CLIENT);
//         register(SPacketRemoveHazardZone.class, SPacketRemoveHazardZone::new, NetworkDirection.PLAY_TO_CLIENT);
//         register(SPacketSyncHazardZoneStrength.class, SPacketSyncHazardZoneStrength::new,
//                 NetworkDirection.PLAY_TO_CLIENT);
//         register(SPacketSyncLevelHazards.class, SPacketSyncLevelHazards::new, NetworkDirection.PLAY_TO_CLIENT);
//         register(SPacketProspectOre.class, SPacketProspectOre::new, NetworkDirection.PLAY_TO_CLIENT);
//         register(SPacketProspectBedrockOre.class, SPacketProspectBedrockOre::new, NetworkDirection.PLAY_TO_CLIENT);
//         register(SPacketProspectBedrockFluid.class, SPacketProspectBedrockFluid::new, NetworkDirection.PLAY_TO_CLIENT);
//         register(SPacketSendWorldID.class, SPacketSendWorldID::new, NetworkDirection.PLAY_TO_CLIENT);
//         register(SPacketNotifyCapeChange.class, SPacketNotifyCapeChange::new, NetworkDirection.PLAY_TO_CLIENT);
//         register(SCPacketShareProspection.class, SCPacketShareProspection::new, null);
// >>>>>>> v7.1.0-1.20.1
//     }
}
