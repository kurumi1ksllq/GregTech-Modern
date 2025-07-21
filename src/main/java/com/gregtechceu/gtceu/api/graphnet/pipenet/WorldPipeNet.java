package com.gregtechceu.gtceu.api.graphnet.pipenet;

import com.gregtechceu.gtceu.api.blockentity.IDirtyNotifiable;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.graphnet.GraphClassType;
import com.gregtechceu.gtceu.api.graphnet.MultiNodeHelper;
import com.gregtechceu.gtceu.api.graphnet.graph.INetGraph;
import com.gregtechceu.gtceu.api.graphnet.net.IGraphNet;
import com.gregtechceu.gtceu.api.graphnet.net.NetEdge;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.net.WorldSavedNet;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.api.graphnet.pipenet.predicate.BlockedPredicate;
import com.gregtechceu.gtceu.api.graphnet.predicate.EdgePredicate;
import com.gregtechceu.gtceu.api.graphnet.predicate.NetPredicateType;
import com.gregtechceu.gtceu.api.graphnet.traverse.EdgeDirection;
import com.gregtechceu.gtceu.common.cover.ShutterCover;
import com.gregtechceu.gtceu.utils.collections.WeakHashSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import it.unimi.dsi.fastutil.Hash;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

public abstract class WorldPipeNet extends WorldSavedNet {

    public static final int MULTI_NET_TIMEOUT = 10;

    private static final Reference2ObjectMap<ResourceKey<Level>, Set<WorldPipeNet>> dimensionNets = new Reference2ObjectOpenHashMap<>();

    @Getter
    private ServerLevel level;
    private ResourceKey<Level> fallbackDimension;

    public WorldPipeNet(Function<IGraphNet, INetGraph> graphBuilder) {
        super(graphBuilder);
    }

    public WorldPipeNet(boolean directed) {
        super(directed);
    }

    public void setLevel(ServerLevel level) {
        if (getLevel() == level) return;
        this.level = level;
        dimensionNets.compute(getDimension(), (k, v) -> {
            if (v == null) v = new WeakHashSet<>();
            v.add(this);
            return v;
        });
    }

    protected ResourceKey<Level> getDimension() {
        if (level == null) {
            return Objects.requireNonNullElse(fallbackDimension, Level.OVERWORLD);
        } else return level.dimension();
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        fallbackDimension = ResourceKey.create(Registries.DIMENSION, new ResourceLocation(nbt.getString("Dimension")));
        super.load(nbt);
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag compound) {
        compound.putString("Dimension", getDimension().location().toString());
        return super.save(compound);
    }

    /**
     * Called when a PipeTileEntity is marked dirty through {@link IDirtyNotifiable#markAsDirty()}, which is generally
     * when the state of its covers is changed.
     *
     * @param blockEntity the block entity that's been marked dirty.
     * @param node        the associated node.
     */
    public void updatePredication(@NotNull WorldPipeNode node, @NotNull PipeBlockEntity blockEntity) {
        boolean dirty = false;
        for (NetEdge edge : getBacker().getTouchingEdges(node, EdgeDirection.ALL)) {
            NetNode neighbor = edge.getOppositeNode(node);
            if (neighbor == null) continue;
            CoverBehavior cNode = null;
            CoverBehavior cNeighbor = null;
            if (neighbor instanceof NodeWithFacingToOthers n) {
                Direction facing = n.getFacingToOther(node);
                if (facing != null) {
                    cNode = node.getBlockEntity().getCoverHolder().getCoverAtSide(facing.getOpposite());
                    ICoverable view;
                    if (neighbor instanceof NodeWithCovers c && (view = c.getCoverable()) != null) {
                        cNeighbor = view.getCoverAtSide(facing);
                    }
                }
            }
            dirty |= predicateEdge(edge, node, cNode, neighbor, cNeighbor);
        }
        if (dirty) this.markAsDirty();
    }

    /**
     * Preferred method to override if your net has complex custom predication rules. If the net is directed,
     * this method will <b>not</b> be called twice, so special handling for directedness is needed.
     *
     * @param source      the source of the edge.
     * @param coverSource the cover on the source facing the target.
     * @param target      the target of the edge.
     * @param coverTarget the cover on the target facing the source.
     * @return whether the predication state has changed and this net needs to be marked dirty.
     */
    protected boolean predicateEdge(@NotNull NetEdge edge, @NotNull NetNode source,
                                    @Nullable CoverBehavior coverSource,
                                    @NotNull NetNode target, @Nullable CoverBehavior coverTarget) {
        Map<NetPredicateType<?>, EdgePredicate<?, ?>> prevValue = new Object2ObjectOpenHashMap<>(
                edge.getPredicateHandler().getPredicateSet());
        edge.getPredicateHandler().clearPredicates();
        coverPredication(edge, coverSource, coverTarget);
        boolean edgeDifferent = !prevValue.equals(edge.getPredicateHandler().getPredicateSet());
        if (getGraph().isDirected()) {
            edge = getEdge(target, source);
            if (edge == null) return edgeDifferent;
            if (edgeDifferent) {
                prevValue.clear();
                prevValue.putAll(edge.getPredicateHandler().getPredicateSet());
            }
            edge.getPredicateHandler().clearPredicates();
            coverPredication(edge, coverSource, coverTarget);
            if (!edgeDifferent) {
                edgeDifferent = !prevValue.equals(edge.getPredicateHandler().getPredicateSet());
            }
        }
        return edgeDifferent;
    }

    /**
     * Preferred method to override if your net has custom predication rules that only depend on covers.
     * If the net is directed, this method <b>will</b> be called twice, so no special handling for directedness is
     * needed.
     *
     * @param edge the edge to predicate
     * @param a    the cover on the source of the edge
     * @param b    the cover on the sink of the edge
     */
    protected void coverPredication(@NotNull NetEdge edge, @Nullable CoverBehavior a, @Nullable CoverBehavior b) {
        if (a instanceof ShutterCover aS && aS.isWorkingEnabled() ||
                b instanceof ShutterCover bS && bS.isWorkingEnabled()) {
            edge.getPredicateHandler().setPredicate(BlockedPredicate.TYPE.getNew());
        }
    }

    public abstract PipeCapabilityWrapper buildCapabilityWrapper(@NotNull PipeBlockEntity owner,
                                                                 @NotNull WorldPipeNode node);

    @Override
    public @NotNull GraphClassType<? extends NetNode> getDefaultNodeType() {
        return WorldPipeNode.TYPE;
    }

    public @Nullable WorldPipeNode getNode(@NotNull BlockPos equivalencyData) {
        return (WorldPipeNode) getNode((Object) equivalencyData);
    }

    public @NotNull WorldPipeNode getOrCreateNode(@NotNull BlockPos pos) {
        WorldPipeNode node = getNode(pos);
        if (node == null) {
            node = new WorldPipeNode(this);
            node.setPos(pos);
            addNode(node);
        }
        return node;
    }

    protected Stream<@NotNull WorldPipeNet> sameDimensionNetsStream() {
        return dimensionNets.getOrDefault(this.getDimension(), Collections.emptySet()).stream()
                .filter(Objects::nonNull);
    }

    public void synchronizeNode(WorldPipeNode node) {
        // basically, if another net has a node in the exact same position, then we know it's the same block.
        // thus, we set up a multi net node handler for the node in order to manage the overlap
        // this is disk-load safe, since this method is called during nbt deserialization.
        sameDimensionNetsStream().map(n -> n.getNode(node.getEquivalencyData())).filter(Objects::nonNull)
                .forEach(n -> {
                    if (n.overlapHelper != node.overlapHelper) {
                        if (node.overlapHelper == null) {
                            // n handler is not null
                            node.overlapHelper = n.overlapHelper;
                            n.overlapHelper.addNode(node);
                            return;
                        }
                    } else if (n.overlapHelper == null) {
                        // both handlers are null
                        node.overlapHelper = new MultiNodeHelper(MULTI_NET_TIMEOUT);
                        node.overlapHelper.addNode(n);
                    }
                    // n handler does not match cast handler
                    n.overlapHelper = node.overlapHelper;
                    n.overlapHelper.addNode(node);
                });
    }

    /**
     * Get the network ID for this net. Must be unique and deterministic between server and client, but can change
     * between mod versions.
     *
     * @return the net's network id.
     */
    public abstract int getNetworkID();

    @Contract(value = " -> new", pure = true)
    public static <T> @NotNull Object2ObjectOpenCustomHashMap<NetNode, T> getSensitiveHashMap() {
        return new Object2ObjectOpenCustomHashMap<>(SensitiveStrategy.INSTANCE);
    }

    protected static class SensitiveStrategy implements Hash.Strategy<NetNode> {

        public static final SensitiveStrategy INSTANCE = new SensitiveStrategy();

        @Override
        public int hashCode(NetNode o) {
            return o.hashCode() * 31 + o.getNet().hashCode();
        }

        @Override
        public boolean equals(NetNode a, NetNode b) {
            return a.equals(b) && a.getNet().equals(b.getNet());
        }
    }
}
