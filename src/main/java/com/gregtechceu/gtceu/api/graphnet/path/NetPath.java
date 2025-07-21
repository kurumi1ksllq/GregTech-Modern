package com.gregtechceu.gtceu.api.graphnet.path;

import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;
import com.gregtechceu.gtceu.api.graphnet.net.NetEdge;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;

import com.google.common.collect.ImmutableCollection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public interface NetPath {

    @NotNull
    @Unmodifiable
    <N extends NetNode> ImmutableCollection<N> getOrderedNodes();

    @SuppressWarnings("unchecked")
    @NotNull
    default <N extends NetNode> N getSourceNode() {
        ImmutableCollection<NetNode> nodes = getOrderedNodes();
        return (N) nodes.asList().get(0);
    }

    @SuppressWarnings("unchecked")
    @NotNull
    default <N extends NetNode> N getTargetNode() {
        ImmutableCollection<NetNode> nodes = getOrderedNodes();
        return (N) nodes.asList().get(nodes.size() - 1);
    }

    /**
     * Must always contain 1 more element than {@link #getOrderedNodes()}
     */
    @NotNull
    @Unmodifiable
    <E extends NetEdge> ImmutableCollection<E> getOrderedEdges();

    double getWeight();

    @NotNull
    NetPath reversed();

    NetLogicData getUnifiedNodeData();

    @Nullable
    NetLogicData getUnifiedEdgeData();
}
