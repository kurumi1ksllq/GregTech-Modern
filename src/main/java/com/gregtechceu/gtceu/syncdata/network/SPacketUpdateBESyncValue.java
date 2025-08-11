package com.gregtechceu.gtceu.syncdata.network;

import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.syncdata.ManagedSyncBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class SPacketUpdateBESyncValue implements GTNetwork.INetPacket {
    private ManagedSyncBlockEntity blockEntity = null;
    private final BlockPos entityPos;
    private ResourceKey<Level> dimension = null;
    private FriendlyByteBuf dataBuf;

    public SPacketUpdateBESyncValue(FriendlyByteBuf buf) {
        dataBuf = new FriendlyByteBuf(buf.copy());
        dimension = dataBuf.readResourceKey(Registries.DIMENSION);
        entityPos = dataBuf.readBlockPos();
    }

    public SPacketUpdateBESyncValue(ManagedSyncBlockEntity entity) {
        blockEntity = entity;
        entityPos = entity.getBlockPos();
        Level entityLvl = entity.getLevel();
        if (entityLvl == null) return;
        dimension = entityLvl.dimension();
    }

    @Override
    public void encode(FriendlyByteBuf buffer) {
        var entityLvl = blockEntity.getLevel();
        if (entityLvl == null) return;
        buffer.writeResourceKey(entityLvl.dimension());
        buffer.writeBlockPos(entityPos);
        blockEntity.writeToDataBuffer(buffer);
    }

    @Override
    public void execute(NetworkEvent.Context context) {
        if (context.getDirection() == NetworkDirection.PLAY_TO_CLIENT) {
            ClientLevel cLvl = Minecraft.getInstance().level;
            if (cLvl == null) return;
            if (cLvl.dimension() != dimension) return;
            if (!cLvl.isLoaded(entityPos)) return;
            var entity = cLvl.getExistingBlockEntity(entityPos);
            if (entity instanceof ManagedSyncBlockEntity syncBlockEntity) {
                syncBlockEntity.getSyncDataHolder().readFromNetworkBuffer(dataBuf);
            }
        }
    }
}
