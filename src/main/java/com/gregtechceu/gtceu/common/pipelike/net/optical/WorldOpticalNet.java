package com.gregtechceu.gtceu.common.pipelike.net.optical;

import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.graphnet.group.GroupData;
import com.gregtechceu.gtceu.api.graphnet.group.PathCacheGroupData;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNet;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.api.graphnet.traverse.NetBreadthIterator;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldOpticalNet extends WorldPipeNet {

    private static final String DATA_ID = "gtceu_world_optical_net";

    public static WorldOpticalNet getWorldNet(ServerLevel serverLevel) {
        WorldOpticalNet net = serverLevel.getDataStorage().computeIfAbsent(tag -> {
            WorldOpticalNet netx = new WorldOpticalNet();
            netx.load(tag);
            return netx;
        }, WorldOpticalNet::new, DATA_ID);
        net.setLevel(serverLevel);
        return net;
    }

    public WorldOpticalNet() {
        super(false);
    }

    @Override
    public PipeCapabilityWrapper buildCapabilityWrapper(@NotNull PipeBlockEntity owner, @NotNull WorldPipeNode node) {
        Object2ObjectOpenHashMap<Capability<?>, IPipeCapabilityObject> map = new Object2ObjectOpenHashMap<>();
        map.put(GTCapability.CAPABILITY_DATA_ACCESS, new DataCapabilityObject(node));
        return new PipeCapabilityWrapper(owner, node, map, 0, DataCapabilityObject.ACTIVE_KEY);
    }

    @Override
    public @Nullable GroupData getBlankGroupData() {
        return new PathCacheGroupData(NetBreadthIterator::new);
    }

    @Override
    public int getNetworkID() {
        return 4;
    }
}
