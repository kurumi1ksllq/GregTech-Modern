package com.gregtechceu.gtceu.common.pipelike.net.energy;

import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.graphnet.group.GroupData;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNet;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.api.graphnet.traverse.NetClosestIterator;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

public final class WorldEnergyNet extends WorldPipeNet {

    private static final String DATA_ID = "gtceu_world_energy_net";

    public static @NotNull WorldEnergyNet getWorldNet(ServerLevel serverLevel) {
        WorldEnergyNet net = serverLevel.getDataStorage().computeIfAbsent(tag -> {
            WorldEnergyNet netx = new WorldEnergyNet();
            netx.load(tag);
            return netx;
        }, WorldEnergyNet::new, DATA_ID);
        net.setLevel(serverLevel);
        return net;
    }

    public WorldEnergyNet() {
        super(false);
    }

    @Override
    public PipeCapabilityWrapper buildCapabilityWrapper(@NotNull PipeBlockEntity owner, @NotNull WorldPipeNode node) {
        Object2ObjectOpenHashMap<Capability<?>, IPipeCapabilityObject> map = new Object2ObjectOpenHashMap<>();
        map.put(GTCapability.CAPABILITY_ENERGY_CONTAINER, new EnergyCapabilityObject(node));
        return new PipeCapabilityWrapper(owner, node, map, 0, EnergyCapabilityObject.ACTIVE_KEY);
    }

    @Override
    public GroupData getBlankGroupData() {
        return new EnergyGroupData(NetClosestIterator::new);
    }

    @Override
    public int getNetworkID() {
        return 0;
    }
}
