package com.gregtechceu.gtceu.api.graphnet.net;

import com.gregtechceu.gtceu.api.graphnet.GraphNetBacker;
import com.gregtechceu.gtceu.api.graphnet.graph.INetGraph;
import com.gregtechceu.gtceu.api.graphnet.graph.NetDirectedGraph;
import com.gregtechceu.gtceu.api.graphnet.graph.NetUndirectedGraph;
import com.gregtechceu.gtceu.api.graphnet.logic.WeightFactorLogic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/**
 * Unused demonstration net that would allow for edges bridging dimensions inside the graph representation.
 */
@SuppressWarnings("unused")
public abstract class WorldSavedNet extends SavedData implements IGraphNet {

    protected final GraphNetBacker backer;

    //@formatter:off
    /* USERS: ADD THESE METHODS TO YOUR CLASS
    public static WorldSavedNet get(ServerLevel level, Function<IGraphNet, INetGraph> graphBuilder) {
        return level.getDataStorage().computeIfAbsent(tag -> new WorldSavedNet(tag, graphBuilder), () ->
                new WorldSavedNet(graphBuilder), name);
    }

    public static WorldSavedNet get(ServerLevel level, boolean directed) {
        return get(level, directed ? NetDirectedGraph.standardBuilder() : NetUndirectedGraph.standardBuilder());
    }
    */
    //@formatter:on

    public WorldSavedNet(Function<IGraphNet, INetGraph> graphBuilder) {
        this.backer = new GraphNetBacker(this, graphBuilder.apply(this));
    }

    public WorldSavedNet(boolean directed) {
        this(directed ? NetDirectedGraph.standardBuilder() : NetUndirectedGraph.standardBuilder());
    }

    @Override
    public void addNode(@NotNull NetNode node) {
        this.backer.addNode(node);
    }

    @Override
    public @Nullable NetNode getNode(@NotNull Object equivalencyData) {
        return backer.getNode(equivalencyData);
    }

    @Override
    public void removeNode(@NotNull NetNode node) {
        this.backer.removeNode(node);
    }

    @Override
    public NetEdge addEdge(@NotNull NetNode source, @NotNull NetNode target, boolean bothWays) {
        double weight = source.getData().getLogicEntryDefaultable(WeightFactorLogic.TYPE).getValue() +
                target.getData().getLogicEntryDefaultable(WeightFactorLogic.TYPE).getValue();
        NetEdge edge = backer.addEdge(source, target, weight);
        if (bothWays) {
            if (this.getGraph().isDirected()) {
                backer.addEdge(target, source, weight);
            }
            return null;
        } else return edge;
    }

    @Override
    public @Nullable NetEdge getEdge(@NotNull NetNode source, @NotNull NetNode target) {
        return backer.getEdge(source, target);
    }

    @Override
    public void removeEdge(@NotNull NetNode source, @NotNull NetNode target, boolean bothWays) {
        this.backer.removeEdge(source, target);
        if (bothWays && this.getGraph().isDirected()) {
            this.backer.removeEdge(target, source);
        }
    }

    public void load(@NotNull CompoundTag nbt) {
        backer.readFromNBT(nbt);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compound) {
        return backer.writeToNBT(compound);
    }

    @Override
    public GraphNetBacker getBacker() {
        return backer;
    }
}
