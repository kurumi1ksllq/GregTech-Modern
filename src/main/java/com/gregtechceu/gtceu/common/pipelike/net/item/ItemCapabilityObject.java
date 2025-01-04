package com.gregtechceu.gtceu.common.pipelike.net.item;

import com.gregtechceu.gtceu.api.graphnet.GraphNetUtility;
import com.gregtechceu.gtceu.api.graphnet.logic.ChannelCountLogic;
import com.gregtechceu.gtceu.api.graphnet.logic.ThroughputLogic;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.NodeManagingPCW;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.ItemTestObject;
import com.gregtechceu.gtceu.api.graphnet.traverse.EdgeDirection;
import com.gregtechceu.gtceu.api.graphnet.traverse.EdgeSelector;
import com.gregtechceu.gtceu.api.graphnet.traverse.ResilientNetClosestIterator;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.function.Predicate;

public class ItemCapabilityObject implements IPipeCapabilityObject, IItemHandler {

    private PipeBlockEntity tile;
    private NodeManagingPCW capabilityWrapper;

    private final EnumMap<Direction, Wrapper> wrappers = new EnumMap<>(Direction.class);
    private final WorldPipeNode node;

    private boolean transferring = false;

    public ItemCapabilityObject(WorldPipeNode node) {
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
        this.tile = tile;
        if (!(wrapper instanceof NodeManagingPCW p))
            throw new IllegalArgumentException("ItemCapabilityObjects must be initialized to NodeManagingPCWs!");
        this.capabilityWrapper = p;
    }

    private boolean inputDisallowed(Direction side) {
        if (side == null) return false;
        else return tile.isBlocked(side);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction facing) {
        return ForgeCapabilities.ITEM_HANDLER.orEmpty(cap,
                LazyOptional.of(() -> facing == null ? this : wrappers.get(facing)));
    }

    protected @Nullable NetNode getRelevantNode(Direction facing) {
        return facing == null ? node : capabilityWrapper.getNodeForFacing(facing);
    }

    protected @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate, Direction side) {
        @NotNull
        ItemStack result = stack;
        if (!this.transferring && !inputDisallowed(side)) {
            NetNode node = getRelevantNode(side);
            if (node == null) node = this.node;
            this.transferring = true;
            ItemNetworkView networkView = getNetworkView();
            IItemHandler targetHandler = networkView.handler().getHandlerBySlot(slot);
            NetNode targetNode = networkView.handlerNetNodeBiMap().get(targetHandler);
            if (targetNode != null) {
                int handlerSlot = slot - networkView.handler().getOffsetByHandler(targetHandler);
                int insertable = stack.getCount() - targetHandler.insertItem(handlerSlot, stack, true).getCount();
                if (insertable > 0) {
                    final ItemTestObject testObject = new ItemTestObject(stack);
                    Predicate<Object> filter = GraphNetUtility.standardEdgeBlacklist(testObject);
                    ResilientNetClosestIterator forwardFrontier = new ResilientNetClosestIterator(node,
                            EdgeSelector.filtered(EdgeDirection.OUTGOING, filter));
                    ResilientNetClosestIterator backwardFrontier = new ResilientNetClosestIterator(targetNode,
                            EdgeSelector.filtered(EdgeDirection.INCOMING, filter));
                    insertable = GraphNetUtility.p2pWalk(simulate, insertable, n -> getFlowLimit(n, testObject),
                            (n, i) -> reportFlow(n, i, testObject), forwardFrontier, backwardFrontier);
                    if (!simulate) targetHandler.insertItem(handlerSlot, testObject.recombine(insertable), false);
                    result = testObject.recombine(stack.getCount() - insertable);
                }
            }
            this.transferring = false;
        }

        return result;
    }

    protected @NotNull ItemStack extractItem(int slot, int amount, boolean simulate, Direction side) {
        @NotNull
        ItemStack result = ItemStack.EMPTY;
        if (!this.transferring && !inputDisallowed(side)) {
            NetNode node = getRelevantNode(side);
            if (node == null) node = this.node;
            this.transferring = true;
            ItemNetworkView networkView = getNetworkView();
            IItemHandler targetHandler = networkView.handler().getHandlerBySlot(slot);
            NetNode targetNode = networkView.handlerNetNodeBiMap().get(targetHandler);
            if (targetNode != null) {
                int handlerSlot = slot - networkView.handler().getOffsetByHandler(targetHandler);
                ItemStack stack = targetHandler.extractItem(handlerSlot, amount, true);
                int extractable = stack.getCount();
                if (extractable > 0) {
                    final ItemTestObject testObject = new ItemTestObject(stack);
                    Predicate<Object> filter = GraphNetUtility.standardEdgeBlacklist(testObject);
                    ResilientNetClosestIterator forwardFrontier = new ResilientNetClosestIterator(node,
                            EdgeSelector.filtered(EdgeDirection.INCOMING, filter));
                    ResilientNetClosestIterator backwardFrontier = new ResilientNetClosestIterator(targetNode,
                            EdgeSelector.filtered(EdgeDirection.OUTGOING, filter));
                    extractable = GraphNetUtility.p2pWalk(simulate, extractable, n -> getFlowLimit(n, testObject),
                            (n, i) -> reportFlow(n, i, testObject), forwardFrontier, backwardFrontier);
                    if (!simulate) targetHandler.extractItem(handlerSlot, extractable, false);
                    result = testObject.recombine(extractable);
                }
            }
            this.transferring = false;
        }

        return result;
    }

    public static int getFlowLimit(NetNode node, ItemTestObject testObject) {
        ThroughputLogic throughput = node.getData().getLogicEntryNullable(ThroughputLogic.TYPE);
        if (throughput == null) return Integer.MAX_VALUE;
        ItemFlowLogic history = node.getData().getLogicEntryNullable(ItemFlowLogic.TYPE);
        if (history == null) return GTMath.saturatedCast(throughput.getValue() * ItemFlowLogic.BUFFER_MULT);
        Object2LongMap<ItemTestObject> sum = history.getSum();
        if (sum.isEmpty()) return GTMath.saturatedCast(throughput.getValue() * ItemFlowLogic.BUFFER_MULT);
        if (sum.size() < node.getData().getLogicEntryDefaultable(ChannelCountLogic.TYPE).getValue() ||
                sum.containsKey(testObject)) {
            return GTMath.saturatedCast(throughput.getValue() * ItemFlowLogic.BUFFER_MULT - sum.getLong(testObject));
        }
        return 0;
    }

    public static void reportFlow(NetNode node, int flow, ItemTestObject testObject) {
        ItemFlowLogic logic = node.getData().getLogicEntryNullable(ItemFlowLogic.TYPE);
        if (logic == null) {
            logic = ItemFlowLogic.TYPE.getNew();
            node.getData().setLogicEntry(logic);
        }
        logic.recordFlow(GTUtil.getCurrentServerTick(), testObject.recombine(flow));
    }

    public @NotNull ItemNetworkView getNetworkView() {
        if (node.getGroupSafe().getData() instanceof ItemNetworkViewGroupData data) {
            return data.getOrCreate(node);
        }
        return ItemNetworkView.EMPTY;
    }

    @Override
    public int getSlots() {
        return getNetworkView().handler().getSlots();
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return getNetworkView().handler().getStackInSlot(slot);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        return insertItem(slot, stack, simulate, null);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        return extractItem(slot, amount, simulate, null);
    }

    @Override
    public int getSlotLimit(int slot) {
        return getNetworkView().handler().getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return getNetworkView().handler().isItemValid(slot, stack);
    }

    @Nullable
    public static ItemCapabilityObject instanceOf(IItemHandler handler) {
        if (handler instanceof ItemCapabilityObject i) return i;
        if (handler instanceof Wrapper w) return w.getParent();
        return null;
    }

    protected class Wrapper implements IItemHandler {

        private final Direction facing;

        public Wrapper(Direction facing) {
            this.facing = facing;
        }

        @Override
        public int getSlots() {
            return ItemCapabilityObject.this.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return ItemCapabilityObject.this.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return ItemCapabilityObject.this.insertItem(slot, stack, simulate, facing);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemCapabilityObject.this.extractItem(slot, amount, simulate, facing);
        }

        @Override
        public int getSlotLimit(int slot) {
            return ItemCapabilityObject.this.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return ItemCapabilityObject.this.isItemValid(slot, stack);
        }

        public ItemCapabilityObject getParent() {
            return ItemCapabilityObject.this;
        }
    }
}
