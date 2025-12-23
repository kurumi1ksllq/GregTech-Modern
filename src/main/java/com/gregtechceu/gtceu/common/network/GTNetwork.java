package com.gregtechceu.gtceu.common.network;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.common.network.packets.*;
import com.gregtechceu.gtceu.common.network.packets.hazard.*;
import com.gregtechceu.gtceu.common.network.packets.prospecting.SPacketProspectBedrockFluid;
import com.gregtechceu.gtceu.common.network.packets.prospecting.SPacketProspectBedrockOre;
import com.gregtechceu.gtceu.common.network.packets.prospecting.SPacketProspectOre;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.network.PacketDistributor;
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

        registar.playToClient(SPacketProspectOre.TYPE, SPacketProspectOre.CODEC, SPacketProspectOre::execute);
        registar.playToClient(SPacketProspectBedrockFluid.TYPE, SPacketProspectBedrockFluid.CODEC, SPacketProspectBedrockFluid::execute);
        registar.playToClient(SPacketProspectBedrockOre.TYPE, SPacketProspectBedrockOre.CODEC, SPacketProspectBedrockOre::execute);
        registar.playToClient(SPacketSendWorldID.TYPE, SPacketSendWorldID.CODEC, SPacketSendWorldID::execute);
        registar.playBidirectional(SCPacketShareProspection.TYPE, SCPacketShareProspection.CODEC, SCPacketShareProspection::execute);
        // spotless:on        
    }

    public static void sendToServer(CustomPacketPayload packet) {
        PacketDistributor.sendToServer(packet);
    }

    // public static void sendToPlayersInLevel(ResourceKey<Level> level, INetPacket packet) {
    // INSTANCE.send(PacketDistributor.DIMENSION.with(() -> level), packet);
    // }

    // public static void sendToPlayersNearPoint(PacketDistributor.TargetPoint point, INetPacket packet) {
    // INSTANCE.send(PacketDistributor.NEAR.with(() -> point), packet);
    // }

    // public static void sendToAllPlayersTrackingEntity(Entity entity, boolean includeSelf, INetPacket packet) {
    // INSTANCE.send(includeSelf ? PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> entity) :
    // PacketDistributor.TRACKING_ENTITY.with(() -> entity), packet);
    // }

    public static void sendToAllPlayersTrackingChunk(LevelChunk chunk, CustomPacketPayload packet) {
        PacketDistributor.sendToPlayersTrackingChunk((ServerLevel) chunk.getLevel(), chunk.getPos(), packet);
    }

    // public static void sendToAll(INetPacket packet) {
    // INSTANCE.send(PacketDistributor.ALL.noArg(), packet);
    // }

    // public static void sendToPlayer(ServerPlayer player, INetPacket packet) {
    // INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
    // }

    // public static void reply(NetworkEvent.Context context, INetPacket packet) {
    // INSTANCE.reply(packet, context);
    // }

    // public interface INetPacket {
}
