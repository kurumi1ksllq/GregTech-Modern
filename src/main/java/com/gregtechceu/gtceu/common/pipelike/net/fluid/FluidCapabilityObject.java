package com.gregtechceu.gtceu.common.pipelike.net.fluid;

import com.gregtechceu.gtceu.api.fluids.FluidState;
import com.gregtechceu.gtceu.api.fluids.attribute.FluidAttribute;
import com.gregtechceu.gtceu.api.graphnet.GraphNetUtility;
import com.gregtechceu.gtceu.api.graphnet.logic.ChannelCountLogic;
import com.gregtechceu.gtceu.api.graphnet.logic.ThroughputLogic;
import com.gregtechceu.gtceu.api.graphnet.net.NetEdge;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.NodeExposingCapabilities;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.logic.TemperatureLogic;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.IWorldPipeNetTile;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.NodeManagingPCW;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.FluidTestObject;
import com.gregtechceu.gtceu.api.graphnet.traverse.EdgeDirection;
import com.gregtechceu.gtceu.api.graphnet.traverse.EdgeSelector;
import com.gregtechceu.gtceu.api.graphnet.traverse.ResilientNetClosestIterator;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.MapUtil;

import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.List;

public class FluidCapabilityObject implements IPipeCapabilityObject, IFluidHandler {

    private PipeBlockEntity blockEntity;
    private NodeManagingPCW capabilityWrapper;

    private final EnumMap<Direction, Wrapper> wrappers = new EnumMap<>(Direction.class);
    private final WorldPipeNode node;
    @Getter
    private final int tanks;

    private boolean transferring = false;

    public FluidCapabilityObject(WorldPipeNode node) {
        this.node = node;
        this.tanks = node.getData().getLogicEntryDefaultable(ChannelCountLogic.TYPE)
                .getValue();
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

    protected @Nullable NetNode getRelevantNode(Direction facing) {
        return facing == null ? node : capabilityWrapper.getNodeForFacing(facing);
    }

    protected int fill(FluidStack resource, FluidAction action, Direction side) {
        if (this.transferring || inputDisallowed(side)) return 0;
        NetNode node = getRelevantNode(side);
        if (node == null) node = this.node;
        this.transferring = true;

        int flow = resource.getAmount();
        FluidTestObject testObject = new FluidTestObject(resource);
        ResilientNetClosestIterator iter = new ResilientNetClosestIterator(node,
                EdgeSelector.filtered(EdgeDirection.OUTGOING, GraphNetUtility.standardEdgeBlacklist(testObject)));
        Object2IntOpenHashMap<NetNode> availableDemandCache = new Object2IntOpenHashMap<>();
        Object2IntOpenHashMap<NetNode> flowLimitCache = new Object2IntOpenHashMap<>();
        Object2BooleanOpenHashMap<NetNode> lossyCache = new Object2BooleanOpenHashMap<>();
        List<Runnable> postActions = new ObjectArrayList<>();
        int total = 0;
        main:
        while (iter.hasNext()) {
            if (flow <= 0) break;
            final NetNode next = iter.next();
            int limit = Math.min(MapUtil.computeIfAbsent(flowLimitCache, next, n -> getFlowLimit(n, testObject)), flow);
            if (limit <= 0) {
                iter.markInvalid(next);
                continue;
            }
            int supply = MapUtil.computeIfAbsent(availableDemandCache, next,
                    n -> getSupplyOrDemand(n, testObject, false));
            if (supply <= 0) continue;
            supply = Math.min(supply, limit);
            NetEdge span;
            NetNode trace = next;
            ArrayDeque<NetNode> seen = new ArrayDeque<>();
            seen.add(next);
            while ((span = iter.getSpanningTreeEdge(trace)) != null) {
                trace = span.getOppositeNode(trace);
                if (trace == null) continue main;
                int l = MapUtil.computeIfAbsent(flowLimitCache, trace, n -> getFlowLimit(n, testObject));
                if (l == 0) {
                    iter.markInvalid(node);
                    continue main;
                }
                supply = Math.min(supply, l);
                seen.addFirst(trace);
            }
            total += supply;
            flow -= supply;
            int finalSupply = supply;
            for (NetNode n : seen) {
                // reporting flow can cause temperature pipe destruction which causes graph modification while
                // iterating.
                if (action.execute()) postActions.add(() -> reportFlow(n, finalSupply, testObject));
                int remaining = flowLimitCache.getInt(n) - supply;
                flowLimitCache.put(n, remaining);
                if (remaining <= 0) {
                    iter.markInvalid(n);
                }
                if (MapUtil.computeIfAbsent(lossyCache, n, a -> isLossyNode(a, testObject))) {
                    // reporting loss can cause misc pipe destruction which causes graph modification while iterating.
                    if (action.execute()) postActions.add(() -> handleLoss(n, finalSupply, testObject));
                    continue main;
                }
            }
            if (action.execute()) reportExtractedInserted(next, supply, testObject, false);
            availableDemandCache.put(next, availableDemandCache.getInt(next) - supply);
        }
        postActions.forEach(Runnable::run);
        this.transferring = false;
        return total;
    }

    protected FluidStack drain(int maxDrain, FluidAction action, Direction side) {
        FluidStack stack = getNetworkView().handler().drain(maxDrain, FluidAction.SIMULATE);
        if (stack == null) return null;
        return drain(stack, action, side);
    }

    protected FluidStack drain(FluidStack resource, FluidAction action, Direction side) {
        if (this.transferring) return null;
        NetNode node = getRelevantNode(side);
        if (node == null) node = this.node;
        this.transferring = true;

        int flow = resource.getAmount();
        FluidTestObject testObject = new FluidTestObject(resource);
        ResilientNetClosestIterator iter = new ResilientNetClosestIterator(node,
                EdgeSelector.filtered(EdgeDirection.INCOMING, GraphNetUtility.standardEdgeBlacklist(testObject)));
        Object2IntOpenHashMap<NetNode> availableSupplyCache = new Object2IntOpenHashMap<>();
        Object2IntOpenHashMap<NetNode> flowLimitCache = new Object2IntOpenHashMap<>();
        Object2BooleanOpenHashMap<NetNode> lossyCache = new Object2BooleanOpenHashMap<>();
        List<Runnable> postActions = new ObjectArrayList<>();
        int total = 0;
        main:
        while (iter.hasNext()) {
            if (flow <= 0) break;
            final NetNode next = iter.next();
            int limit = Math.min(MapUtil.computeIfAbsent(flowLimitCache, next, n -> getFlowLimit(n, testObject)), flow);
            if (limit <= 0) {
                iter.markInvalid(next);
                continue;
            }
            int supply = MapUtil.computeIfAbsent(availableSupplyCache, next,
                    n -> getSupplyOrDemand(n, testObject, true));
            if (supply <= 0) continue;
            supply = Math.min(supply, limit);
            NetEdge span;
            NetNode trace = next;
            ArrayDeque<NetNode> seen = new ArrayDeque<>();
            seen.add(next);
            while ((span = iter.getSpanningTreeEdge(trace)) != null) {
                trace = span.getOppositeNode(trace);
                if (trace == null) continue main;
                int l = MapUtil.computeIfAbsent(flowLimitCache, trace, n -> getFlowLimit(n, testObject));
                if (l == 0) {
                    iter.markInvalid(node);
                    continue main;
                }
                supply = Math.min(supply, l);
                seen.addFirst(trace);
            }
            total += supply;
            flow -= supply;
            int finalSupply = supply;
            for (NetNode n : seen) {
                // reporting flow can cause temperature pipe destruction which causes graph modification while
                // iterating.
                if (action.execute()) postActions.add(() -> reportFlow(n, finalSupply, testObject));
                int remaining = flowLimitCache.getInt(n) - supply;
                flowLimitCache.put(n, remaining);
                if (remaining <= 0) {
                    iter.markInvalid(n);
                }
                if (MapUtil.computeIfAbsent(lossyCache, n, a -> isLossyNode(a, testObject))) {
                    // reporting loss can cause misc pipe destruction which causes graph modification while iterating.
                    if (action.execute()) postActions.add(() -> handleLoss(n, finalSupply, testObject));
                    continue main;
                }
            }
            if (action.execute()) reportExtractedInserted(next, supply, testObject, true);
            availableSupplyCache.put(next, availableSupplyCache.getInt(next) - supply);
        }
        postActions.forEach(Runnable::run);
        this.transferring = false;
        return testObject.recombine(total);
    }

    public static int getFlowLimit(NetNode node, FluidTestObject testObject) {
        ThroughputLogic throughput = node.getData().getLogicEntryNullable(ThroughputLogic.TYPE);
        if (throughput == null) return Integer.MAX_VALUE;
        FluidFlowLogic history = node.getData().getLogicEntryNullable(FluidFlowLogic.TYPE);
        if (history == null) return GTMath.saturatedCast(throughput.getValue() * FluidFlowLogic.MEMORY_TICKS);
        Object2LongMap<FluidTestObject> sum = history.getSum();
        if (sum.isEmpty()) return GTMath.saturatedCast(throughput.getValue() * FluidFlowLogic.MEMORY_TICKS);
        if (sum.size() < node.getData().getLogicEntryDefaultable(ChannelCountLogic.TYPE).getValue() ||
                sum.containsKey(testObject)) {
            return GTMath.saturatedCast(throughput.getValue() * FluidFlowLogic.MEMORY_TICKS - sum.getLong(testObject));
        }
        return 0;
    }

    public static boolean isLossyNode(NetNode node, FluidTestObject testObject) {
        FluidContainmentLogic containmentLogic = node.getData().getLogicEntryNullable(FluidContainmentLogic.TYPE);
        return containmentLogic != null && !containmentLogic.handles(testObject);
    }

    public static void reportFlow(NetNode node, int flow, FluidTestObject testObject) {
        FluidFlowLogic logic = node.getData().getLogicEntryNullable(FluidFlowLogic.TYPE);
        if (logic == null) {
            logic = FluidFlowLogic.TYPE.getNew();
            node.getData().setLogicEntry(logic);
        }
        logic.recordFlow(GTUtil.getCurrentServerTick(), testObject, flow);
        TemperatureLogic temp = node.getData().getLogicEntryNullable(TemperatureLogic.TYPE);
        if (temp != null) {
            FluidStack stack = testObject.recombine(flow);
            FluidContainmentLogic cont = node.getData().getLogicEntryDefaultable(FluidContainmentLogic.TYPE);
            int t = stack.getFluid().getFluidType().getTemperature(stack);
            temp.moveTowardsTemperature(t, GTUtil.getCurrentServerTick(), stack.getAmount(),
                    cont.getMaximumTemperature() >= t);
            if (node instanceof WorldPipeNode n) {
                temp.defaultHandleTemperature(n.getNet().getLevel(), n.getEquivalencyData());
            }
        }
    }

    public static void reportExtractedInserted(NetNode node, int flow, FluidTestObject testObject, boolean extracted) {
        if (flow == 0) return;
        if (node instanceof NodeExposingCapabilities exposer) {
            IFluidHandler handler = exposer.getProvider().getCapability(ForgeCapabilities.FLUID_HANDLER,
                    exposer.exposedFacing())
                    .resolve().orElse(null);
            if (handler != null) {
                if (extracted) {
                    handler.drain(testObject.recombine(flow), FluidAction.EXECUTE);
                } else {
                    handler.fill(testObject.recombine(flow), FluidAction.EXECUTE);
                }
            }
        }
    }

    public static void handleLoss(NetNode node, int flow, FluidTestObject testObject) {
        if (flow == 0) return;
        FluidContainmentLogic logic = node.getData().getLogicEntryDefaultable(FluidContainmentLogic.TYPE);
        if (node instanceof WorldPipeNode n) {
            IWorldPipeNetTile tile = n.getBlockEntity();
            FluidStack stack = testObject.recombine(flow);
            // failing attributes take priority over state
            for (FluidAttribute attribute : FluidAttribute.inferAttributes(stack)) {
                if (!logic.contains(attribute)) {
                    attribute.handleFailure(tile.getLevel(), tile.getBlockPos(), stack);
                    return;
                }
            }
            FluidState state = FluidState.inferState(stack);
            if (!logic.contains(state)) state.handleFailure(tile.getLevel(), tile.getBlockPos(), stack);
        }
    }

    public static int getSupplyOrDemand(NetNode node, FluidTestObject testObject, boolean supply) {
        if (node instanceof NodeExposingCapabilities exposer) {
            IFluidHandler handler = exposer.getProvider().getCapability(ForgeCapabilities.FLUID_HANDLER,
                    exposer.exposedFacing())
                    .resolve().orElse(null);
            if (handler != null && instanceOf(handler) == null) {
                if (supply) {
                    FluidStack s = handler.drain(testObject.recombine(Integer.MAX_VALUE), FluidAction.SIMULATE);
                    return s.isEmpty() ? 0 : s.getAmount();
                } else {
                    return handler.fill(testObject.recombine(Integer.MAX_VALUE), FluidAction.SIMULATE);
                }
            }
        }
        return 0;
    }

    public @NotNull FluidNetworkView getNetworkView() {
        if (node.getGroupSafe().getData() instanceof FluidNetworkViewGroupData data) {
            return data.getOrCreate(node);
        }
        return FluidNetworkView.EMPTY;
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return true;
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

    protected class Wrapper implements IFluidHandler {

        private final Direction facing;
        @Getter
        private final int tanks;

        public Wrapper(Direction facing) {
            this.facing = facing;
            this.tanks = FluidCapabilityObject.this.tanks;
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
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return true;
        }

        public FluidCapabilityObject getParent() {
            return FluidCapabilityObject.this;
        }
    }
}
