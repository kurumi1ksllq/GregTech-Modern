package com.gregtechceu.gtceu.common.pipelike.net.fluid;

import com.gregtechceu.gtceu.api.fluids.FluidState;
import com.gregtechceu.gtceu.api.fluids.attribute.FluidAttribute;
import com.gregtechceu.gtceu.api.graphnet.GraphNetUtility;
import com.gregtechceu.gtceu.api.graphnet.logic.ChannelCountLogic;
import com.gregtechceu.gtceu.api.graphnet.logic.ThroughputLogic;
import com.gregtechceu.gtceu.api.graphnet.net.NetEdge;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.path.NetPath;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.logic.TemperatureLogic;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.IWorldPipeNetTile;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.NodeManagingPCW;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.FluidTestObject;
import com.gregtechceu.gtceu.api.graphnet.traverse.EdgeDirection;
import com.gregtechceu.gtceu.api.graphnet.traverse.ResilientNetClosestIterator;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.TickTracker;
import com.gregtechceu.gtceu.utils.collections.ListHashSet;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

public class FluidCapabilityObject implements IPipeCapabilityObject, IFluidHandler {

    private PipeBlockEntity blockEntity;
    private NodeManagingPCW capabilityWrapper;

    private final EnumMap<Direction, Wrapper> wrappers = new EnumMap<>(Direction.class);
    private final WorldPipeNode node;

    private boolean transferring = false;

    public FluidCapabilityObject(WorldPipeNode node) {
        this.node = node;
        for (Direction facing : GTUtil.DIRECTIONS) {
            wrappers.put(facing, new Wrapper(facing));
        }
    }

    public WorldPipeNode getNode() {
        return node;
    }

    @Override
    public void init(@NotNull PipeBlockEntity tile, @NotNull PipeCapabilityWrapper wrapper) {
        this.blockEntity = tile;
        if (!(wrapper instanceof NodeManagingPCW p))
            throw new IllegalArgumentException("FluidCapabilityObjects must be initialized to NodeManagingPCWs!");
        this.capabilityWrapper = p;
    }

    private boolean inputDisallowed(Direction side) {
        if (side == null) return false;
        else return blockEntity.isBlocked(side);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction facing) {
        // can't expose the sided capability if there is no node to interact with
        if (facing != null && capabilityWrapper.getNodeForFacing(facing) == null) return LazyOptional.empty();
        return ForgeCapabilities.FLUID_HANDLER.orEmpty(cap,
                LazyOptional.of(() -> facing == null ? this : wrappers.get(facing)));
    }

    protected @Nullable NetNode getRelevantNode(@Nullable Direction facing) {
        return facing == null ? node : capabilityWrapper.getNodeForFacing(facing);
    }

    protected int fill(FluidStack resource, FluidAction action, Direction side) {
        if (this.transferring || inputDisallowed(side)) return 0;
        NetNode node = getRelevantNode(side);
        if (node == null) node = this.node;
        this.transferring = true;

        int flow = resource.getAmount();
        FluidNetworkView networkView = getNetworkView(node);
        FluidTestObject testObject = new FluidTestObject(resource);

        int maxPredictedSize = node.getGroupSafe().getNodes().size();
        Reference2IntOpenHashMap<NetNode> flowLimitCache = new Reference2IntOpenHashMap<>(maxPredictedSize);
        Reference2BooleanOpenHashMap<NetNode> lossyCache = new Reference2BooleanOpenHashMap<>(maxPredictedSize);
        List<Runnable> postActions = new ObjectArrayList<>();

        for (IFluidHandler targetHandler : networkView.getHandler().getBackingHandlers()) {
            NetNode targetNode = networkView.getBiMap().get(targetHandler);
            if (targetNode == null) continue;
            final int filled = targetHandler.fill(testObject.recombine(flow), FluidAction.SIMULATE);
            int insertable = filled;
            if (insertable <= 0) continue;

            ListHashSet<NetPath> pathCache = networkView.getPathCache(targetNode);
            ResilientNetClosestIterator forwardFrontier = null;
            ResilientNetClosestIterator backwardFrontier = null;

            Iterator<NetPath> iterator = pathCache.iterator();
            PATH_LOOP:
            while (insertable > 0) {
                NetPath path;
                if (iterator != null && iterator.hasNext()) path = iterator.next();
                else {
                    iterator = null;
                    if (forwardFrontier == null) {
                        forwardFrontier = new ResilientNetClosestIterator(node, EdgeDirection.OUTGOING);
                        backwardFrontier = new ResilientNetClosestIterator(targetNode, EdgeDirection.INCOMING);
                    }
                    path = GraphNetUtility.p2pNextPath(
                            n -> getFlowLimitCached(flowLimitCache, n, testObject) <= 0,
                            e -> !e.test(testObject), forwardFrontier, backwardFrontier);
                    if (path == null) break;

                    int i = pathCache.size();
                    while (i > 0 && pathCache.get(i - 1).getWeight() > path.getWeight()) {
                        i--;
                    }
                    if (!pathCache.addSensitive(i, path)) break;
                }
                int insert = attemptPath(path, insertable,
                        n -> getFlowLimitCached(flowLimitCache, n, testObject),
                        e -> !e.test(testObject),
                        n -> isLossyNodeCached(lossyCache, n, testObject));
                if (insert > 0) {
                    insertable -= insert;
                    ImmutableList<NetNode> asList = path.getOrderedNodes().asList();
                    for (int j = 0; j < asList.size(); j++) {
                        NetNode n = asList.get(j);
                        if (action.execute()) {
                            // reporting temp change can cause temperature pipe destruction which causes
                            // graph modification while iterating.
                            Runnable post = reportFlow(n, insert, testObject);
                            if (post != null) postActions.add(post);
                        }
                        flowLimitCache.put(n, flowLimitCache.getInt(n) - insert);
                        if (isLossyNodeCached(lossyCache, n, testObject)) {
                            // reporting loss can cause misc pipe destruction which causes
                            // graph modification while iterating.
                            if (action.execute()) postActions.add(() -> handleLoss(n, insert, testObject));
                            // a lossy node will prevent filling the target
                            continue PATH_LOOP;
                        }
                    }
                    if (action.execute()) targetHandler.fill(testObject.recombine(insert), FluidAction.EXECUTE);
                }
            }
            flow -= filled - insertable;
        }
        postActions.forEach(Runnable::run);
        this.transferring = false;
        return resource.getAmount() - flow;
    }

    protected @NotNull FluidStack drain(int maxDrain, FluidAction action, Direction side) {
        FluidStack stack = getNetworkView(side).getHandler().drain(maxDrain, FluidAction.SIMULATE);
        if (stack.isEmpty()) return FluidStack.EMPTY;
        return drain(stack, action, side);
    }

    protected @NotNull FluidStack drain(FluidStack resource, FluidAction action, Direction side) {
        if (this.transferring) return FluidStack.EMPTY;
        NetNode node = getRelevantNode(side);
        if (node == null) node = this.node;
        this.transferring = true;

        int flow = resource.getAmount();
        FluidNetworkView networkView = getNetworkView(node);
        FluidTestObject testObject = new FluidTestObject(resource);

        int maxPredictedSize = node.getGroupSafe().getNodes().size();
        Reference2IntOpenHashMap<NetNode> flowLimitCache = new Reference2IntOpenHashMap<>(maxPredictedSize);
        Reference2BooleanOpenHashMap<NetNode> lossyCache = new Reference2BooleanOpenHashMap<>(maxPredictedSize);
        List<Runnable> postActions = new ObjectArrayList<>();

        for (IFluidHandler targetHandler : networkView.getHandler().getBackingHandlers()) {
            NetNode targetNode = networkView.getBiMap().get(targetHandler);
            if (targetNode == null) continue;
            final FluidStack drained = targetHandler.drain(testObject.recombine(flow), FluidAction.SIMULATE);
            int extractable = drained.getAmount();
            if (extractable <= 0) continue;

            ListHashSet<NetPath> pathCache = getNetworkView(targetNode).getPathCache(node);
            ResilientNetClosestIterator forwardFrontier = null;
            ResilientNetClosestIterator backwardFrontier = null;

            Iterator<NetPath> iterator = pathCache.iterator();
            while (extractable > 0) {
                NetPath path;
                if (iterator != null && iterator.hasNext()) path = iterator.next();
                else {
                    iterator = null;
                    if (forwardFrontier == null) {
                        forwardFrontier = new ResilientNetClosestIterator(targetNode, EdgeDirection.OUTGOING);
                        backwardFrontier = new ResilientNetClosestIterator(node, EdgeDirection.INCOMING);
                    }
                    path = GraphNetUtility.p2pNextPath(
                            n -> getFlowLimitCached(flowLimitCache, n, testObject) <= 0,
                            e -> !e.test(testObject), forwardFrontier, backwardFrontier);
                    if (path == null) break;

                    int i = pathCache.size();
                    while (i > 0 && pathCache.get(i - 1).getWeight() > path.getWeight()) {
                        i--;
                    }
                    if (!pathCache.addSensitive(i, path)) break;
                }
                int extract = attemptPath(path, extractable,
                        n -> getFlowLimitCached(flowLimitCache, n, testObject),
                        e -> !e.test(testObject),
                        n -> isLossyNodeCached(lossyCache, n, testObject));

                if (extract > 0) {
                    extractable -= extract;
                    ImmutableList<NetNode> asList = path.getOrderedNodes().asList();
                    for (int j = 0; j < asList.size(); j++) {
                        NetNode n = asList.get(j);
                        if (action.execute()) {
                            // reporting temp change can cause temperature pipe destruction which causes
                            // graph modification while iterating.
                            Runnable post = reportFlow(n, extract, testObject);
                            if (post != null) postActions.add(post);
                        }
                        flowLimitCache.put(n, flowLimitCache.getInt(n) - extract);
                        if (isLossyNodeCached(lossyCache, n, testObject)) {
                            // reporting loss can cause misc pipe destruction which causes
                            // graph modification while iterating.
                            if (action.execute()) postActions.add(() -> handleLoss(n, extract, testObject));
                            // a lossy node will prevent receiving extracted fluid
                            extractable += extract;
                            break;
                        }
                    }
                    if (action.execute()) targetHandler.drain(testObject.recombine(extract), FluidAction.EXECUTE);
                }
            }
            flow -= drained.getAmount() - extractable;
        }
        postActions.forEach(Runnable::run);
        this.transferring = false;
        return testObject.recombine(resource.getAmount() - flow);
    }

    protected int attemptPath(NetPath path, int available, ToIntFunction<NetNode> limit, Predicate<NetEdge> filter,
                              Predicate<NetNode> lossy) {
        ImmutableList<NetEdge> edges = path.getOrderedEdges().asList();
        for (int i = 0; i < edges.size(); i++) {
            if (filter.test(edges.get(i))) return 0;
        }
        ImmutableList<NetNode> nodes = path.getOrderedNodes().asList();
        for (int i = 0; i < nodes.size(); i++) {
            NetNode n = nodes.get(i);
            if (lossy.test(n)) return available;
            available = Math.min(limit.applyAsInt(n), available);
            if (available <= 0) return 0;
        }
        return available;
    }

    public static int getFlowLimitCached(Reference2IntOpenHashMap<NetNode> cache, NetNode n,
                                         FluidTestObject testObject) {
        return GraphNetUtility.computeIfAbsent(cache, n, z -> getFlowLimit(z, testObject));
    }

    public static int getFlowLimit(NetNode node, FluidTestObject testObject) {
        ThroughputLogic throughput = node.getData().getLogicEntryNullable(ThroughputLogic.TYPE);
        if (throughput == null) return Integer.MAX_VALUE;
        FluidFlowLogic history = node.getData().getLogicEntryNullable(FluidFlowLogic.TYPE);
        if (history == null) return GTMath.saturatedCast(throughput.getValue() * FluidFlowLogic.MEMORY_TICKS);
        Object2LongMap<FluidTestObject> sum = history.getSum(false);
        if (sum.isEmpty()) return GTMath.saturatedCast(throughput.getValue() * FluidFlowLogic.MEMORY_TICKS);
        if (sum.size() < node.getData().getLogicEntryDefaultable(ChannelCountLogic.TYPE).getValue() ||
                sum.containsKey(testObject)) {
            return GTMath.saturatedCast(throughput.getValue() * FluidFlowLogic.MEMORY_TICKS - sum.getLong(testObject));
        }
        return 0;
    }

    public static boolean isLossyNodeCached(Reference2BooleanOpenHashMap<NetNode> cache, NetNode n,
                                            FluidTestObject testObject) {
        return GraphNetUtility.computeIfAbsent(cache, n, z -> isLossyNode(z, testObject));
    }

    public static boolean isLossyNode(NetNode node, FluidTestObject testObject) {
        FluidContainmentLogic containmentLogic = node.getData().getLogicEntryNullable(FluidContainmentLogic.TYPE);
        return containmentLogic != null && !containmentLogic.handles(testObject);
    }

    public static Runnable reportFlow(NetNode node, int flow, FluidTestObject testObject) {
        FluidFlowLogic logic = node.getData().getLogicEntryNullable(FluidFlowLogic.TYPE);
        if (logic == null) {
            logic = FluidFlowLogic.TYPE.getNew();
            node.getData().setLogicEntry(logic);
        }
        logic.recordFlow(TickTracker.getTick(), testObject, flow);
        TemperatureLogic temp = node.getData().getLogicEntryNullable(TemperatureLogic.TYPE);
        if (temp == null) return null;
        FluidContainmentLogic cont = node.getData().getLogicEntryDefaultable(FluidContainmentLogic.TYPE);
        FluidStack stack = testObject.recombine();
        int t = stack.getFluid().getFluidType().getTemperature(stack);
        boolean noParticle = cont.getMaximumTemperature() >= t;
        return () -> {
            temp.moveTowardsTemperature(t, TickTracker.getTick(), flow, noParticle);
            if (node instanceof WorldPipeNode n) {
                temp.defaultHandleTemperature(n.getNet().getLevel(), n.getEquivalencyData());
            }
        };
    }

    public static void handleLoss(NetNode node, int flow, FluidTestObject testObject) {
        if (flow == 0) return;
        FluidContainmentLogic logic = node.getData().getLogicEntryDefaultable(FluidContainmentLogic.TYPE);
        if (node instanceof WorldPipeNode n) {
            IWorldPipeNetTile blockEntity = n.getBlockEntity();
            FluidStack stack = testObject.recombine(flow);
            // failing attributes take priority over state
            for (FluidAttribute attribute : FluidAttribute.inferAttributes(stack)) {
                if (!logic.contains(attribute)) {
                    attribute.handleFailure(blockEntity.getLevel(), blockEntity.getBlockPos(), stack);
                    return;
                }
            }
            FluidState state = FluidState.inferState(stack);
            if (!logic.contains(state)) state.handleFailure(blockEntity.getLevel(), blockEntity.getBlockPos(), stack);
        }
    }

    public @NotNull FluidNetworkView getNetworkView(@Nullable Direction facing) {
        NetNode node = getRelevantNode(facing);
        if (node == null) node = this.node;
        return getNetworkView(node);
    }

    public static @NotNull FluidNetworkView getNetworkView(@NotNull NetNode node) {
        if (node.getGroupSafe().getData() instanceof FluidNetworkViewGroupData data) {
            return data.getOrCreate(node);
        }
        return FluidNetworkView.EMPTY;
    }

    @Override
    public int getTanks() {
        return getNetworkView(node).getHandler().getTanks();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return getNetworkView(node).getHandler().getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return getNetworkView(node).getHandler().getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return getNetworkView(node).getHandler().isFluidValid(tank, stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        return fill(resource, action, null);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return drain(maxDrain, action, null);
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        return drain(resource, action, null);
    }

    @Nullable
    public static FluidCapabilityObject instanceOf(IFluidHandler handler) {
        if (handler instanceof FluidCapabilityObject f) return f;
        if (handler instanceof Wrapper w) return w.getParent();
        return null;
    }

    @Nullable
    public static Direction facingOf(IFluidHandler handler) {
        if (handler instanceof Wrapper w) {
            return w.facing;
        }
        return null;
    }

    protected class Wrapper implements IFluidHandler {

        private final Direction facing;

        public Wrapper(Direction facing) {
            this.facing = facing;
        }

        @Override
        public int getTanks() {
            return getNetworkView(facing).getHandler().getTanks();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return FluidCapabilityObject.this.fill(resource, action, facing);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidCapabilityObject.this.drain(resource, action, facing);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidCapabilityObject.this.drain(maxDrain, action, facing);
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tank) {
            return getNetworkView(facing).getHandler().getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return getNetworkView(facing).getHandler().getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return getNetworkView(facing).getHandler().isFluidValid(tank, stack);
        }

        public FluidCapabilityObject getParent() {
            return FluidCapabilityObject.this;
        }
    }
}
