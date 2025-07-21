package com.gregtechceu.gtceu.common.pipelike.net.item;

import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.filter.CoverWithItemFilter;
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
import com.gregtechceu.gtceu.common.pipelike.net.fluid.WorldFluidNet;

import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WorldItemNet extends WorldPipeNet {

    private static final String DATA_ID = "gtceu_world_item_net";

    public static @NotNull WorldItemNet getWorldNet(ServerLevel serverLevel) {
        WorldItemNet net = serverLevel.getDataStorage().computeIfAbsent(tag -> {
            WorldItemNet netx = new WorldItemNet();
            netx.load(tag);
            return netx;
        }, WorldItemNet::new, DATA_ID);
        net.setLevel(serverLevel);
        return net;
    }

    public WorldItemNet() {
        super(true);
    }

    @Override
    protected void coverPredication(@NotNull NetEdge edge, @Nullable CoverBehavior a, @Nullable CoverBehavior b) {
        super.coverPredication(edge, a, b);
        if (edge.getPredicateHandler().hasPredicate(BlockedPredicate.TYPE)) return;
        FilterPredicate predicate = null;
        if (a instanceof CoverWithItemFilter filter) {
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
        if (b instanceof CoverWithItemFilter filter) {
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
        return net instanceof WorldFluidNet;
    }

    @Override
    public PipeCapabilityWrapper buildCapabilityWrapper(@NotNull PipeBlockEntity owner, @NotNull WorldPipeNode node) {
        Object2ObjectOpenHashMap<Capability<?>, IPipeCapabilityObject> map = new Object2ObjectOpenHashMap<>();
        map.put(ForgeCapabilities.ITEM_HANDLER, new ItemCapabilityObject(node));
        return new NodeManagingPCW(owner, node, map, 0, 0);
    }

    public static int getBufferTicks() {
        return 10;
    }

    @Override
    public int getNetworkID() {
        return 2;
    }

    @Override
    public @Nullable GroupData getBlankGroupData() {
        return new ItemNetworkViewGroupData();
    }
}
