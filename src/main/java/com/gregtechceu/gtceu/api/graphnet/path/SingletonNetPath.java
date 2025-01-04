package com.gregtechceu.gtceu.api.graphnet.path;

import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicData;
import com.gregtechceu.gtceu.api.graphnet.logic.WeightFactorLogic;
import com.gregtechceu.gtceu.api.graphnet.net.NetEdge;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

public class SingletonNetPath implements NetPath {

    protected final NetNode node;
    protected final ImmutableSet<NetNode> singleton;

    protected final double weight;

    public SingletonNetPath(NetNode node) {
        this(node, node.getData().getLogicEntryDefaultable(WeightFactorLogic.TYPE).getValue());
    }

    public SingletonNetPath(NetNode node, double weight) {
        this.node = node;
        this.singleton = ImmutableSet.of(node);
        this.weight = weight;
    }

    @Override
    public @NotNull @Unmodifiable <N extends NetNode> ImmutableCollection<N> getOrderedNodes() {
        return (ImmutableCollection<N>) singleton;
    }

    @Override
    public <N extends NetNode> @NotNull N getSourceNode() {
        return (N) node;
    }

    @Override
    public <N extends NetNode> @NotNull N getTargetNode() {
        return (N) node;
    }

    @Override
    public @NotNull @Unmodifiable <E extends NetEdge> ImmutableCollection<E> getOrderedEdges() {
        return ImmutableSet.of();
    }

    @Override
    public double getWeight() {
        return weight;
    }

    @Override
    public @NotNull NetPath reversed() {
        return this;
    }

    @Override
    public NetLogicData getUnifiedNodeData() {
        return node.getData();
    }

    @Override
    public @Nullable NetLogicData getUnifiedEdgeData() {
        return null;
    }
}
