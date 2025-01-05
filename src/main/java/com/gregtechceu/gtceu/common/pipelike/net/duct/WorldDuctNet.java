package com.gregtechceu.gtceu.common.pipelike.net.duct;

import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNet;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.NodeManagingPCW;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;

public class WorldDuctNet extends WorldPipeNet {

    private static final String DATA_ID = "gtceu_world_duct_net";

    public static WorldDuctNet getWorldNet(ServerLevel serverLevel) {
        WorldDuctNet net = serverLevel.getDataStorage().computeIfAbsent(tag -> {
            WorldDuctNet netx = new WorldDuctNet();
            netx.load(tag);
            return netx;
        }, WorldDuctNet::new, DATA_ID);
        net.setLevel(serverLevel);
        return net;
    }

    public WorldDuctNet() {
        super(false);
    }

    @Override
    public PipeCapabilityWrapper buildCapabilityWrapper(@NotNull PipeBlockEntity owner, @NotNull WorldPipeNode node) {
        Object2ObjectOpenHashMap<Capability<?>, IPipeCapabilityObject> map = new Object2ObjectOpenHashMap<>();
        map.put(GTCapability.CAPABILITY_HAZARD_CONTAINER, new DuctCapabilityObject(node));
        return new NodeManagingPCW(owner, node, map, 0, DuctCapabilityObject.ACTIVE_KEY);
    }

    public static int getBufferTicks() {
        return 10;
    }

    @Override
    public int getNetworkID() {
        return 5;
    }

    @Override
    public void setDirty() {
        super.setDirty();
    }
}
