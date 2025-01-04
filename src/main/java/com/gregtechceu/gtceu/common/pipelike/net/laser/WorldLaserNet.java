package com.gregtechceu.gtceu.common.pipelike.net.laser;

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

public class WorldLaserNet extends WorldPipeNet {

    private static final String DATA_ID = "gtceu_world_laser_net";

    public static @NotNull WorldLaserNet getWorldNet(ServerLevel serverLevel) {
        WorldLaserNet net = serverLevel.getDataStorage().computeIfAbsent(tag -> {
            WorldLaserNet netx = new WorldLaserNet();
            netx.load(tag);
            return netx;
        }, WorldLaserNet::new, DATA_ID);
        net.setLevel(serverLevel);
        return net;
    }

    public WorldLaserNet() {
        super(false);
    }

    @Override
    public PipeCapabilityWrapper buildCapabilityWrapper(@NotNull PipeBlockEntity owner, @NotNull WorldPipeNode node) {
        Object2ObjectOpenHashMap<Capability<?>, IPipeCapabilityObject> map = new Object2ObjectOpenHashMap<>();
        map.put(GTCapability.CAPABILITY_LASER, new LaserCapabilityObject(node));
        return new PipeCapabilityWrapper(owner, node, map, 0, LaserCapabilityObject.ACTIVE_KEY);
    }

    @Override
    public @Nullable GroupData getBlankGroupData() {
        return new PathCacheGroupData(NetBreadthIterator::new);
    }

    @Override
    public int getNetworkID() {
        return 3;
    }
}
