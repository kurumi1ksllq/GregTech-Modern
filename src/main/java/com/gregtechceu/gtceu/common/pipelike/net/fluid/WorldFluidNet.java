package com.gregtechceu.gtceu.common.pipelike.net.fluid;

import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.filter.CoverWithFluidFilter;
import com.gregtechceu.gtceu.api.graphnet.group.GroupData;
import com.gregtechceu.gtceu.api.graphnet.net.IGraphNet;
import com.gregtechceu.gtceu.api.graphnet.net.NetEdge;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNet;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.NodeManagingPCW;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.api.graphnet.pipenet.predicate.BlockedPredicate;
import com.gregtechceu.gtceu.api.graphnet.pipenet.predicate.FilterPredicate;
import com.gregtechceu.gtceu.common.cover.data.FilterMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;
import com.gregtechceu.gtceu.common.pipelike.net.item.WorldItemNet;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldFluidNet extends WorldPipeNet {

    private static final String DATA_ID = "gtceu_world_fluid_net";

    public static WorldFluidNet getWorldNet(ServerLevel serverLevel) {
        WorldFluidNet net = serverLevel.getDataStorage().computeIfAbsent(tag -> {
            WorldFluidNet netx = new WorldFluidNet();
            netx.load(tag);
            return netx;
        }, WorldFluidNet::new, DATA_ID);
        net.setLevel(serverLevel);
        return net;
    }

    public WorldFluidNet() {
        super(true);
    }

    @Override
    protected void coverPredication(@NotNull NetEdge edge, @Nullable CoverBehavior a, @Nullable CoverBehavior b) {
        super.coverPredication(edge, a, b);
        if (edge.getPredicateHandler().hasPredicate(BlockedPredicate.TYPE)) return;
        FilterPredicate predicate = null;
        if (a instanceof CoverWithFluidFilter filter) {
            if (filter.getManualIOMode() == ManualIOMode.DISABLED) {
                edge.getPredicateHandler().clearPredicates();
                edge.getPredicateHandler().setPredicate(BlockedPredicate.TYPE.getNew());
                return;
            } else if (filter.getManualIOMode() == ManualIOMode.FILTERED &&
                    filter.getFilterMode() != FilterMode.FILTER_INSERT) {
                        predicate = FilterPredicate.TYPE.getNew();
                        predicate.setSourceFilter(filter.getFilterHandler().getFilter());
                    }
        }
        if (b instanceof CoverWithFluidFilter filter) {
            if (filter.getManualIOMode() == ManualIOMode.DISABLED) {
                edge.getPredicateHandler().clearPredicates();
                edge.getPredicateHandler().setPredicate(BlockedPredicate.TYPE.getNew());
                return;
            } else if (filter.getManualIOMode() == ManualIOMode.FILTERED &&
                    filter.getFilterMode() != FilterMode.FILTER_EXTRACT) {
                        if (predicate == null) predicate = FilterPredicate.TYPE.getNew();
                        predicate.setTargetFilter(filter.getFilterHandler().getFilter());
                    }
        }
        if (predicate != null) edge.getPredicateHandler().setPredicate(predicate);
    }

    @Override
    public boolean clashesWith(IGraphNet net) {
        return net instanceof WorldItemNet;
    }

    @Override
    public PipeCapabilityWrapper buildCapabilityWrapper(@NotNull PipeBlockEntity owner, @NotNull WorldPipeNode node) {
        Object2ObjectOpenHashMap<Capability<?>, IPipeCapabilityObject> map = new Object2ObjectOpenHashMap<>();
        map.put(ForgeCapabilities.FLUID_HANDLER, new FluidCapabilityObject(node));
        return new NodeManagingPCW(owner, node, map, 0, 0);
    }

    public static int getBufferTicks() {
        return 10;
    }

    @Override
    public int getNetworkID() {
        return 1;
    }

    @Override
    public @Nullable GroupData getBlankGroupData() {
        return new FluidNetworkViewGroupData();
    }
}
