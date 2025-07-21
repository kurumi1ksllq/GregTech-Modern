package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IIOCover;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.cover.filter.CoverWithItemFilter;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandlers;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.graphnet.GraphNetUtility;
import com.gregtechceu.gtceu.api.graphnet.net.NetEdge;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.path.NetPath;
import com.gregtechceu.gtceu.api.graphnet.pipenet.NodeExposingCapabilities;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.ItemTestObject;
import com.gregtechceu.gtceu.api.graphnet.traverse.EdgeDirection;
import com.gregtechceu.gtceu.api.graphnet.traverse.EdgeSelector;
import com.gregtechceu.gtceu.api.graphnet.traverse.NetClosestIterator;
import com.gregtechceu.gtceu.api.graphnet.traverse.ResilientNetClosestIterator;
import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelectorWidget;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.transfer.item.ItemHandlerDelegate;
import com.gregtechceu.gtceu.client.renderer.cover.CoverRenderer;
import com.gregtechceu.gtceu.common.cover.data.DistributionMode;
import com.gregtechceu.gtceu.common.cover.data.FilterMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;
import com.gregtechceu.gtceu.common.cover.filter.MatchResult;
import com.gregtechceu.gtceu.common.cover.filter.MergabilityInfo;
import com.gregtechceu.gtceu.common.pipelike.net.item.*;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.ItemStackHashStrategy;
import com.gregtechceu.gtceu.utils.collections.ListHashSet;
import com.gregtechceu.gtceu.utils.function.BiIntConsumer;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.utils.LocalizationUtils;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ConveyorCover extends CoverBehavior implements IIOCover, IUICover, IControllable, CoverWithItemFilter {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ConveyorCover.class,
            CoverBehavior.MANAGED_FIELD_HOLDER);

    // 8 32 128 512 1024
    public static final Int2IntFunction CONVEYOR_SCALING = tier -> 2 * (int) Math.pow(4, Math.min(tier, GTValues.LuV));

    public final int tier;
    public final int maxItemTransferRate;
    @Persisted
    @Getter
    protected int transferRate;
    @Persisted
    @DescSynced
    @Getter
    @RequireRerender
    protected IO io;
    @Persisted
    @DescSynced
    @Getter
    protected DistributionMode distributionMode;
    @Persisted
    @DescSynced
    @Getter
    protected ManualIOMode manualIOMode = ManualIOMode.DISABLED;
    @Persisted
    @DescSynced
    @Getter
    protected boolean isWorkingEnabled = true;
    protected int itemsLeftToTransferLastSecond;
    private CoverableItemHandlerWrapper itemHandlerWrapper;
    private Widget ioModeSwitch;

    protected final ObjectLinkedOpenHashSet<IItemHandler> extractionRoundRobinCache = new ObjectLinkedOpenHashSet<>();
    protected final ObjectLinkedOpenHashSet<IItemHandler> insertionRoundRobinCache = new ObjectLinkedOpenHashSet<>();

    protected @Nullable CoverRenderer rendererInverted;

    @Persisted
    @DescSynced
    @Getter
    protected final FilterHandler<ItemStack, ItemFilter> filterHandler;
    protected final ConditionalSubscriptionHandler subscriptionHandler;

    public ConveyorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide, int tier,
                         int maxTransferRate) {
        super(definition, coverHolder, attachedSide);
        this.tier = tier;
        this.maxItemTransferRate = maxTransferRate;
        this.transferRate = maxItemTransferRate;
        this.itemsLeftToTransferLastSecond = transferRate;
        this.io = IO.OUT;
        this.distributionMode = DistributionMode.FLOOD;

        subscriptionHandler = new ConditionalSubscriptionHandler(coverHolder, this::update, this::isSubscriptionActive);
        filterHandler = FilterHandlers.item(this)
                .onFilterLoaded(f -> configureFilter())
                .onFilterUpdated(f -> configureFilter())
                .onFilterRemoved(f -> configureFilter());
    }

    public ConveyorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide, int tier) {
        this(definition, coverHolder, attachedSide, tier, CONVEYOR_SCALING.applyAsInt(tier));
    }

    protected boolean isSubscriptionActive() {
        return isWorkingEnabled() && getAdjacentItemHandler() != null;
    }

    protected @Nullable IItemHandler getOwnItemHandler() {
        return coverHolder.getItemHandlerCap(attachedSide, false);
    }

    protected @Nullable IItemHandler getAdjacentItemHandler() {
        BlockEntity blockEntity = coverHolder.getNeighbor(attachedSide);
        if (blockEntity == null) {
            return null;
        }
        var opt = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, attachedSide.getOpposite());
        if (opt.isPresent()) {
            return opt.resolve().orElse(null);
        } else {
            return null;
        }
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public @NotNull ModelData getModelData() {
        return IIOCover.super.getModelData();
    }

    public void setTransferRate(int transferRate) {
        if (transferRate <= maxItemTransferRate) {
            this.transferRate = transferRate;
        }
    }

    public void setIo(IO io) {
        if (io == IO.IN || io == IO.OUT) {
            this.io = io;
        }
        subscriptionHandler.updateSubscription();
        coverHolder.markDirty();
    }

    public void setDistributionMode(DistributionMode distributionMode) {
        this.distributionMode = distributionMode;
        this.extractionRoundRobinCache.clear();
        this.extractionRoundRobinCache.trim(16);
        this.insertionRoundRobinCache.clear();
        this.insertionRoundRobinCache.trim(16);
        coverHolder.markDirty();
    }

    protected void setManualIOMode(ManualIOMode manualIOMode) {
        this.manualIOMode = manualIOMode;
        coverHolder.markDirty();
    }

    @Override
    public FilterMode getFilterMode() {
        return FilterMode.FILTER_BOTH;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        subscriptionHandler.initialize(coverHolder.getLevel());
    }

    @Override
    public void onRemoved() {
        super.onRemoved();
        subscriptionHandler.unsubscribe();
    }

    @Override
    public List<ItemStack> getAdditionalDrops() {
        var list = super.getAdditionalDrops();
        if (!filterHandler.getFilterItem().isEmpty()) {
            list.add(filterHandler.getFilterItem());
        }
        return list;
    }

    //////////////////////////////////////
    // ***** Transfer Logic *****//
    //////////////////////////////////////

    @Override
    public void onNeighborChanged(Block block, BlockPos fromPos, boolean isMoving) {
        subscriptionHandler.updateSubscription();
    }

    @Override
    public void setWorkingEnabled(boolean isWorkingAllowed) {
        if (this.isWorkingEnabled != isWorkingAllowed) {
            this.isWorkingEnabled = isWorkingAllowed;
            subscriptionHandler.updateSubscription();
        }
    }

    protected void update() {
        long timer = coverHolder.getOffsetTimer();
        if (timer % 5 == 0 && isWorkingEnabled && getItemsLeftToTransfer() > 0) {
            IItemHandler itemHandler = getAdjacentItemHandler();
            IItemHandler myItemHandler = getOwnItemHandler();
            // if we're on a pipe, we shouldn't see said pipe's sided handler, but instead its unsided handler.
            IItemHandler h = ItemCapabilityObject.instanceOf(myItemHandler);
            if (h != null) myItemHandler = h;
            if (itemHandler != null && myItemHandler != null) {
                if (io == IO.OUT) {
                    performTransferOnUpdate(myItemHandler, itemHandler);
                } else {
                    performTransferOnUpdate(itemHandler, myItemHandler);
                }
            }
        }
        if (timer % 20 == 0) {
            refreshBuffer(transferRate);
        }
    }

    protected int getItemsLeftToTransfer() {
        return itemsLeftToTransferLastSecond;
    }

    protected void reportItemsTransfer(int transferred) {
        this.itemsLeftToTransferLastSecond -= transferred;
    }

    protected void refreshBuffer(int transferRate) {
        this.itemsLeftToTransferLastSecond = transferRate;
    }

    protected void performTransferOnUpdate(@NotNull IItemHandler sourceHandler, @NotNull IItemHandler destHandler) {
        reportItemsTransfer(performTransfer(sourceHandler, destHandler, false, i -> 0,
                i -> getItemsLeftToTransfer(), null));
    }

    /**
     * Performs transfer
     *
     * @param sourceHandler  the handler to pull from
     * @param destHandler    the handler to push to
     * @param byFilterSlot   whether to perform the transfer by filter slot.
     * @param minTransfer    the minimum allowed transfer amount, when given a filter slot. If no filter exists or not
     *                       transferring by slot, a filter slot of -1 will be passed in.
     * @param maxTransfer    the maximum allowed transfer amount, when given a filter slot. If no filter exists or not
     *                       transferring by slot, a filter slot of -1 will be passed in.
     * @param transferReport where transfer is reported; a is the filter slot, b is the amount of transfer.
     *                       Each filter slot will report its transfer before the next slot is calculated.
     * @return how much was transferred in total.
     */
    protected int performTransfer(@NotNull IItemHandler sourceHandler, @NotNull IItemHandler destHandler,
                                  boolean byFilterSlot, @NotNull IntUnaryOperator minTransfer,
                                  @NotNull IntUnaryOperator maxTransfer, @Nullable BiIntConsumer transferReport) {
        var filter = this.getFilterHandler();
        byFilterSlot = byFilterSlot && filter.isFilterPresent(); // can't be by filter slot if there is no filter
        Int2IntLinkedOpenHashMap containedByFilterSlot = new Int2IntLinkedOpenHashMap();
        Int2ObjectArrayMap<MergabilityInfo<ItemTestObject>> filterSlotToMergability = new Int2ObjectArrayMap<>();
        for (int i = 0; i < sourceHandler.getSlots(); i++) {
            ItemStack stack = sourceHandler.getStackInSlot(i);
            int extracted = stack.getCount();
            if (extracted == 0) continue;
            MatchResult match = null;
            if (!filter.isFilterPresent() || (match = filter.getFilter().match(stack)).isMatched()) {
                int filterSlot = -1;
                if (byFilterSlot) {
                    filterSlot = match.getFilterIndex();
                }
                containedByFilterSlot.addTo(filterSlot, extracted);
                final int handlerSlot = i;
                filterSlotToMergability.compute(filterSlot, (k, v) -> {
                    if (v == null) v = new MergabilityInfo<>();
                    v.add(handlerSlot, new ItemTestObject(stack), extracted);
                    return v;
                });
            }
        }
        var iter = containedByFilterSlot.int2IntEntrySet().fastIterator();
        int totalTransfer = 0;
        while (iter.hasNext()) {
            var next = iter.next();
            int filterSlot = next.getIntKey();
            int min = Math.max(minTransfer.applyAsInt(filterSlot), 1);
            int max = maxTransfer.applyAsInt(filterSlot);
            if (max < min) continue;
            int slotTransfer = 0;
            if (next.getIntValue() >= min) {
                MergabilityInfo<ItemTestObject> mergabilityInfo = filterSlotToMergability.get(filterSlot);
                var merges = mergabilityInfo.getMerges();
                for (var merge : merges) {
                    if (merge.getCount() + slotTransfer < min) break;
                    int transfer = Math.min(merge.getCount(), max - slotTransfer);
                    transfer = doInsert(destHandler, merge.getTestObject(), transfer, true);
                    if (transfer < min) continue;
                    transfer = doExtract(sourceHandler, merge.getTestObject(), transfer, true);
                    if (transfer < min) continue;
                    doExtract(sourceHandler, merge.getTestObject(), transfer, false);
                    doInsert(destHandler, merge.getTestObject(), transfer, false);
                    int remaining = max - transfer;
                    slotTransfer += transfer;
                    if (remaining <= 0 || slotTransfer >= max) break;
                }
                if (transferReport != null) transferReport.accept(filterSlot, slotTransfer);
                totalTransfer += slotTransfer;
            }
        }
        return totalTransfer;
    }

    protected ObjectLinkedOpenHashSet<IItemHandler> getRoundRobinCache(boolean extract, boolean simulate) {
        ObjectLinkedOpenHashSet<IItemHandler> set = extract ? extractionRoundRobinCache : insertionRoundRobinCache;
        return simulate ? set.clone() : set;
    }

    protected int doExtract(@NotNull IItemHandler handler, ItemTestObject testObject, int count, boolean simulate) {
        ItemCapabilityObject cap;
        if (distributionMode == DistributionMode.FLOOD || (cap = ItemCapabilityObject.instanceOf(handler)) == null)
            return simpleExtract(handler, testObject, count, simulate);
        NetNode origin = cap.getNode();
        // if you find yourself here because you added a new distribution mode and now it won't compile,
        // good luck.
        return switch (distributionMode) {
            case ROUND_ROBIN -> {
                ItemNetworkView view = cap.getNetworkView(ItemCapabilityObject.facingOf(handler));
                Iterator<IItemHandler> iter = view.getHandler().getBackingHandlers().iterator();
                ObjectLinkedOpenHashSet<IItemHandler> cache = getRoundRobinCache(true, simulate);
                Set<IItemHandler> backlog = new ObjectOpenHashSet<>();
                Reference2IntOpenHashMap<NetNode> flowLimitCache = new Reference2IntOpenHashMap<>(
                        origin.getGroupSafe().getNodes().size());
                int available = count;
                while (available > 0) {
                    if (!cache.isEmpty() && backlog.remove(cache.first())) {
                        IItemHandler candidate = cache.first();
                        NetNode linked = view.getBiMap().get(candidate);
                        if (linked == null) {
                            cache.removeFirst();
                            continue;
                        } else {
                            cache.addAndMoveToLast(candidate);
                        }
                        available = rrExtract(testObject, simulate, origin, flowLimitCache, available, candidate,
                                linked);
                        continue;
                    }
                    if (iter.hasNext()) {
                        IItemHandler candidate = iter.next();
                        boolean frontOfCache = !cache.isEmpty() && cache.first() == candidate;
                        if (frontOfCache || !cache.contains(candidate)) {
                            NetNode linked = view.getBiMap().get(candidate);
                            if (linked == null) {
                                if (frontOfCache) cache.removeFirst();
                                continue;
                            } else {
                                cache.addAndMoveToLast(candidate);
                            }
                            available = rrExtract(testObject, simulate, origin, flowLimitCache, available, candidate,
                                    linked);
                        } else {
                            backlog.add(candidate);
                        }
                    } else if (backlog.isEmpty()) {
                        // we have finished the iterator and backlog
                        break;
                    } else {
                        if (!cache.isEmpty()) {
                            if (view.getHandler().getBackingHandlers().contains(cache.first()))
                                break; // we've already visited the next node in the cache
                            else {
                                // the network view does not contain the node in the front of the cache, so yeet it.
                                cache.removeFirst();
                            }
                        } else {
                            break; // cache is empty and iterator is empty, something is weird, just exit.
                        }
                    }
                }
                while (iter.hasNext()) {
                    cache.add(iter.next());
                }
                yield count - available;
            }
            case EQUALIZED -> {
                // only consider destinations that are not on the other side of a filter that rejects our test object
                NetClosestIterator gather = new NetClosestIterator(origin,
                        EdgeSelector.filtered(EdgeDirection.INCOMING,
                                GraphNetUtility.edgeSelectorBlacklist(testObject)));
                Object2ObjectOpenHashMap<NetNode, IItemHandler> candidates = new Object2ObjectOpenHashMap<>();
                while (gather.hasNext()) {
                    NetNode node = gather.next();
                    if (node instanceof NodeExposingCapabilities exposer) {
                        IItemHandler h = exposer.getProvider().getCapability(
                                ForgeCapabilities.ITEM_HANDLER,
                                exposer.exposedFacing()).resolve().orElse(null);
                        if (h != null && ItemCapabilityObject.instanceOf(h) == null) {
                            candidates.put(node, h);
                        }
                    }
                }
                if (candidates.isEmpty()) yield 0;
                int largestMin = count / candidates.size();
                if (largestMin <= 0) yield 0;
                for (IItemHandler value : candidates.values()) {
                    largestMin = Math.min(largestMin, simpleExtract(value, testObject, largestMin, true));
                    if (largestMin <= 0) yield 0;
                }
                // binary search for largest scale that doesn't exceed flow limits
                Reference2IntOpenHashMap<NetNode> flowLimitCache = new Reference2IntOpenHashMap<>();
                Int2ObjectArrayMap<Reference2IntOpenHashMap<NetNode>> flows = new Int2ObjectArrayMap<>();
                largestMin = GTUtil.binarySearchInt(0, largestMin, l -> {
                    if (flows.containsKey(l) && flows.get(l) == null) return false;
                    ResilientNetClosestIterator backwardFrontier = null;
                    Reference2IntOpenHashMap<NetNode> localFlows = new Reference2IntOpenHashMap<>();
                    for (NetNode node : candidates.keySet()) {
                        ListHashSet<NetPath> pathCache = ItemCapabilityObject.getNetworkView(node).getPathCache(origin);
                        ResilientNetClosestIterator forwardFrontier = null;
                        Iterator<NetPath> iterator = pathCache.iterator();
                        int needed = l;
                        while (needed > 0) {
                            NetPath path;
                            if (iterator != null && iterator.hasNext()) path = iterator.next();
                            else {
                                iterator = null;
                                if (backwardFrontier == null) {
                                    backwardFrontier = new ResilientNetClosestIterator(origin, EdgeDirection.INCOMING);
                                }
                                if (forwardFrontier == null) {
                                    forwardFrontier = new ResilientNetClosestIterator(node, EdgeDirection.OUTGOING);
                                }
                                path = GraphNetUtility.p2pNextPath(
                                        n -> ItemCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) <=
                                                localFlows.getInt(n),
                                        e -> !e.test(testObject), forwardFrontier, backwardFrontier,
                                        (f, b) -> f.hasNext());
                                if (path == null) break;
                                int i = pathCache.size();
                                while (i > 0 && pathCache.get(i - 1).getWeight() > path.getWeight()) {
                                    i--;
                                }
                                if (!pathCache.addSensitive(i, path)) break;
                            }
                            int extract = attemptPath(path, needed,
                                    n -> ItemCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) -
                                            localFlows.getInt(n),
                                    e -> !e.test(testObject));
                            if (extract > 0) {
                                needed -= extract;
                                ImmutableList<NetNode> asList = path.getOrderedNodes().asList();
                                for (int j = 0; j < asList.size(); j++) {
                                    NetNode n = asList.get(j);
                                    localFlows.put(n, localFlows.getInt(n) + extract);
                                }
                            }
                        }
                        if (needed > 0) {
                            flows.put(l, null);
                            return false;
                        }
                    }
                    flows.put(l, localFlows);
                    return true;
                }, false);
                if (largestMin <= 0 || flows.get(largestMin) == null) yield 0;
                if (!simulate) {
                    for (IItemHandler value : candidates.values()) {
                        simpleExtract(value, testObject, largestMin, false);
                    }
                    for (var e : flows.get(largestMin).reference2IntEntrySet()) {
                        ItemCapabilityObject.reportFlow(e.getKey(), e.getIntValue(), testObject);
                    }
                }
                yield largestMin * candidates.size();
            }
            case FLOOD -> 0; // how are you here?
        };
    }

    protected int rrExtract(ItemTestObject testObject, boolean simulate, NetNode origin,
                            Reference2IntOpenHashMap<NetNode> flowLimitCache, int available,
                            IItemHandler candidate, NetNode linked) {
        int extractable = simpleExtract(candidate, testObject, available, true);
        if (extractable > 0) {
            ListHashSet<NetPath> pathCache = ItemCapabilityObject.getNetworkView(linked).getPathCache(origin);
            Iterator<NetPath> iterator = pathCache.iterator();
            ResilientNetClosestIterator forwardFrontier = null;
            ResilientNetClosestIterator backwardFrontier = null;
            while (extractable > 0) {
                NetPath path;
                if (iterator != null && iterator.hasNext()) path = iterator.next();
                else {
                    iterator = null;
                    if (forwardFrontier == null) {
                        forwardFrontier = new ResilientNetClosestIterator(linked, EdgeDirection.OUTGOING);
                        backwardFrontier = new ResilientNetClosestIterator(origin, EdgeDirection.INCOMING);
                    }
                    path = GraphNetUtility.p2pNextPath(
                            n -> ItemCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) <= 0,
                            e -> !e.test(testObject), forwardFrontier, backwardFrontier);
                    if (path == null) break;
                    int i = pathCache.size();
                    while (i > 0 && pathCache.get(i - 1).getWeight() > path.getWeight()) {
                        i--;
                    }
                    if (!pathCache.addSensitive(i, path)) break;
                }
                int extract = attemptPath(path, extractable,
                        n -> ItemCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject),
                        e -> !e.test(testObject));
                if (extract > 0) {
                    extractable -= extract;
                    available -= extract;
                    ImmutableList<NetNode> asList = path.getOrderedNodes().asList();
                    for (int j = 0; j < asList.size(); j++) {
                        NetNode n = asList.get(j);
                        if (!simulate) ItemCapabilityObject.reportFlow(n, extract, testObject);
                        flowLimitCache.put(n, flowLimitCache.getInt(n) - extract);
                    }
                    if (!simulate) simpleExtract(candidate, testObject, extract, false);
                }
            }
        }
        return available;
    }

    protected int simpleExtract(@NotNull IItemHandler handler, ItemTestObject testObject, int count,
                                boolean simulate) {
        int available = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack slot = handler.getStackInSlot(i);
            if (testObject.test(slot)) {
                available += handler.extractItem(i, count - available, simulate).getCount();
                if (available == count) return count;
            }
        }
        return available;
    }

    protected int doInsert(@NotNull IItemHandler handler, ItemTestObject testObject, final int count,
                           boolean simulate) {
        ItemCapabilityObject cap;
        if (distributionMode == DistributionMode.FLOOD || (cap = ItemCapabilityObject.instanceOf(handler)) == null)
            return simpleInsert(handler, testObject, count, simulate);
        NetNode origin = cap.getNode();
        // if you find yourself here because you added a new distribution mode and now it won't compile,
        // good luck.
        return switch (distributionMode) {
            case ROUND_ROBIN -> {
                ItemNetworkView view = cap.getNetworkView(ItemCapabilityObject.facingOf(handler));
                Iterator<IItemHandler> iter = view.getHandler().getBackingHandlers().iterator();
                ObjectLinkedOpenHashSet<IItemHandler> cache = getRoundRobinCache(false, simulate);
                Set<IItemHandler> backlog = new ObjectOpenHashSet<>();
                Reference2IntOpenHashMap<NetNode> flowLimitCache = new Reference2IntOpenHashMap<>(
                        origin.getGroupSafe().getNodes().size());
                int available = count;
                while (available > 0) {
                    if (!cache.isEmpty() && backlog.remove(cache.first())) {
                        IItemHandler candidate = cache.first();
                        NetNode linked = view.getBiMap().get(candidate);
                        if (linked == null) {
                            cache.removeFirst();
                            continue;
                        } else {
                            cache.addAndMoveToLast(candidate);
                        }
                        available = rrInsert(testObject, simulate, origin, flowLimitCache, available, candidate,
                                linked);
                        continue;
                    }
                    if (iter.hasNext()) {
                        IItemHandler candidate = iter.next();
                        boolean frontOfCache = !cache.isEmpty() && cache.first() == candidate;
                        if (frontOfCache || !cache.contains(candidate)) {
                            NetNode linked = view.getBiMap().get(candidate);
                            if (linked == null) {
                                if (frontOfCache) cache.removeFirst();
                                continue;
                            } else {
                                cache.addAndMoveToLast(candidate);
                            }
                            available = rrInsert(testObject, simulate, origin, flowLimitCache, available, candidate,
                                    linked);
                        } else {
                            backlog.add(candidate);
                        }
                    } else if (backlog.isEmpty()) {
                        // we have finished the iterator and backlog
                        break;
                    } else {
                        if (!cache.isEmpty()) {
                            if (view.getHandler().getBackingHandlers().contains(cache.first()))
                                break; // we've already visited the next node in the cache
                            else {
                                // the network view does not contain the node in the front of the cache, so yeet it.
                                cache.removeFirst();
                            }
                        } else {
                            break; // cache is empty and iterator is empty, something is weird, just exit.
                        }
                    }
                }
                while (iter.hasNext()) {
                    cache.add(iter.next());
                }
                yield count - available;
            }
            case EQUALIZED -> {
                // only consider destinations that are not on the other side of a filter that rejects our test object
                NetClosestIterator gather = new NetClosestIterator(origin,
                        EdgeSelector.filtered(EdgeDirection.OUTGOING,
                                GraphNetUtility.edgeSelectorBlacklist(testObject)));
                Object2ObjectOpenHashMap<NetNode, IItemHandler> candidates = new Object2ObjectOpenHashMap<>();
                while (gather.hasNext()) {
                    NetNode node = gather.next();
                    if (node instanceof NodeExposingCapabilities exposer) {
                        IItemHandler h = exposer.getProvider().getCapability(
                                ForgeCapabilities.ITEM_HANDLER,
                                exposer.exposedFacing()).resolve().orElse(null);
                        if (h != null && ItemCapabilityObject.instanceOf(h) == null) {
                            candidates.put(node, h);
                        }
                    }
                }
                if (candidates.isEmpty()) yield 0;
                int largestMin = count / candidates.size();
                if (largestMin <= 0) yield 0;
                for (IItemHandler value : candidates.values()) {
                    largestMin = Math.min(largestMin, simpleInsert(value, testObject, largestMin, true));
                    if (largestMin <= 0) yield 0;
                }
                // binary search for largest scale that doesn't exceed flow limits
                Reference2IntOpenHashMap<NetNode> flowLimitCache = new Reference2IntOpenHashMap<>();
                Int2ObjectArrayMap<Reference2IntOpenHashMap<NetNode>> flows = new Int2ObjectArrayMap<>();
                ItemNetworkView view = ItemCapabilityObject.getNetworkView(origin);
                largestMin = GTUtil.binarySearchInt(0, largestMin, l -> {
                    if (flows.containsKey(l) && flows.get(l) == null) return false;
                    ResilientNetClosestIterator forwardFrontier = null;
                    Reference2IntOpenHashMap<NetNode> localFlows = new Reference2IntOpenHashMap<>();
                    for (NetNode node : candidates.keySet()) {
                        ListHashSet<NetPath> pathCache = view.getPathCache(node);
                        ResilientNetClosestIterator backwardFrontier = null;
                        Iterator<NetPath> iterator = pathCache.iterator();
                        int needed = l;
                        while (needed > 0) {
                            NetPath path;
                            if (iterator != null && iterator.hasNext()) path = iterator.next();
                            else {
                                iterator = null;
                                if (forwardFrontier == null) {
                                    forwardFrontier = new ResilientNetClosestIterator(origin, EdgeDirection.OUTGOING);
                                }
                                if (backwardFrontier == null) {
                                    backwardFrontier = new ResilientNetClosestIterator(node, EdgeDirection.INCOMING);
                                }
                                path = GraphNetUtility.p2pNextPath(
                                        n -> ItemCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) <=
                                                localFlows.getInt(n),
                                        e -> !e.test(testObject), forwardFrontier, backwardFrontier,
                                        (f, b) -> b.hasNext());
                                if (path == null) break;
                                int i = pathCache.size();
                                while (i > 0 && pathCache.get(i - 1).getWeight() > path.getWeight()) {
                                    i--;
                                }
                                if (!pathCache.addSensitive(i, path)) break;
                            }
                            int insert = attemptPath(path, needed,
                                    n -> ItemCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) -
                                            localFlows.getInt(n),
                                    e -> !e.test(testObject));
                            if (insert > 0) {
                                needed -= insert;
                                ImmutableList<NetNode> asList = path.getOrderedNodes().asList();
                                for (int j = 0; j < asList.size(); j++) {
                                    NetNode n = asList.get(j);
                                    localFlows.put(n, localFlows.getInt(n) + insert);
                                }
                            }
                        }
                        if (needed > 0) {
                            flows.put(l, null);
                            return false;
                        }
                    }
                    flows.put(l, localFlows);
                    return true;
                }, false);
                if (largestMin <= 0 || flows.get(largestMin) == null) yield 0;
                if (!simulate) {
                    for (IItemHandler value : candidates.values()) {
                        simpleInsert(value, testObject, largestMin, false);
                    }
                    for (var e : flows.get(largestMin).reference2IntEntrySet()) {
                        ItemCapabilityObject.reportFlow(e.getKey(), e.getIntValue(), testObject);
                    }
                }
                yield largestMin * candidates.size();
            }
            case FLOOD -> 0; // how are you here?
        };
    }

    protected int rrInsert(ItemTestObject testObject, boolean simulate, NetNode origin,
                           Reference2IntOpenHashMap<NetNode> flowLimitCache, int available, IItemHandler candidate,
                           NetNode linked) {
        int insertable = simpleInsert(candidate, testObject, available, true);
        if (insertable > 0) {
            ListHashSet<NetPath> pathCache = ItemCapabilityObject.getNetworkView(origin).getPathCache(linked);
            Iterator<NetPath> iterator = pathCache.iterator();
            ResilientNetClosestIterator forwardFrontier = null;
            ResilientNetClosestIterator backwardFrontier = null;
            while (insertable > 0) {
                NetPath path;
                if (iterator != null && iterator.hasNext()) path = iterator.next();
                else {
                    iterator = null;
                    if (forwardFrontier == null) {
                        forwardFrontier = new ResilientNetClosestIterator(origin, EdgeDirection.OUTGOING);
                        backwardFrontier = new ResilientNetClosestIterator(linked, EdgeDirection.INCOMING);
                    }
                    path = GraphNetUtility.p2pNextPath(
                            n -> ItemCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) <= 0,
                            e -> !e.test(testObject), forwardFrontier, backwardFrontier);
                    if (path == null) break;
                    int i = pathCache.size();
                    while (i > 0 && pathCache.get(i - 1).getWeight() > path.getWeight()) {
                        i--;
                    }
                    if (!pathCache.addSensitive(i, path)) break;
                }
                int insert = attemptPath(path, insertable,
                        n -> ItemCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject),
                        e -> !e.test(testObject));
                if (insert > 0) {
                    insertable -= insert;
                    available -= insert;
                    ImmutableList<NetNode> asList = path.getOrderedNodes().asList();
                    for (int j = 0; j < asList.size(); j++) {
                        NetNode n = asList.get(j);
                        if (!simulate) ItemCapabilityObject.reportFlow(n, insert, testObject);
                        flowLimitCache.put(n, flowLimitCache.getInt(n) - insert);
                    }
                    if (!simulate) simpleInsert(candidate, testObject, insert, false);
                }
            }
        }
        return available;
    }

    protected int simpleInsert(@NotNull IItemHandler handler, ItemTestObject testObject, int count,
                               boolean simulate) {
        int available = count;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack toInsert = testObject.recombine(Math.min(available, handler.getSlotLimit(i)));
            available -= toInsert.getCount() - handler.insertItem(i, toInsert, simulate).getCount();
            if (available <= 0) return count;
        }
        return count - available;
    }

    protected int attemptPath(NetPath path, int available, ToIntFunction<NetNode> limit, Predicate<NetEdge> filter) {
        ImmutableList<NetEdge> edges = path.getOrderedEdges().asList();
        for (int i = 0; i < edges.size(); i++) {
            if (filter.test(edges.get(i))) return 0;
        }
        ImmutableList<NetNode> nodes = path.getOrderedNodes().asList();
        for (int i = 0; i < nodes.size(); i++) {
            available = Math.min(limit.applyAsInt(nodes.get(i)), available);
            if (available <= 0) return 0;
        }
        return available;
    }

    protected static class TypeItemInfo {

        public final ItemStack itemStack;
        public final int filterSlot;
        public final IntList slots;
        public int totalCount;

        public TypeItemInfo(ItemStack itemStack, int filterSlot, IntList slots, int totalCount) {
            this.itemStack = itemStack;
            this.filterSlot = filterSlot;
            this.slots = slots;
            this.totalCount = totalCount;
        }
    }

    @NotNull
    protected Map<ItemStack, TypeItemInfo> countInventoryItemsByType(@NotNull IItemHandler inventory) {
        Map<ItemStack, TypeItemInfo> result = new Object2ObjectOpenCustomHashMap<>(
                ItemStackHashStrategy.comparingAllButCount());
        for (int srcIndex = 0; srcIndex < inventory.getSlots(); srcIndex++) {
            ItemStack itemStack = inventory.getStackInSlot(srcIndex);
            if (itemStack.isEmpty()) {
                continue;
            }

            var matchResult = getFilterHandler().getFilter().match(itemStack);
            if (!matchResult.isMatched()) continue;

            if (!result.containsKey(itemStack)) {
                TypeItemInfo itemInfo = new TypeItemInfo(itemStack.copy(), matchResult.getFilterIndex(),
                        new IntArrayList(), 0);
                itemInfo.totalCount += itemStack.getCount();
                itemInfo.slots.add(srcIndex);
                result.put(itemStack.copy(), itemInfo);
            } else {
                TypeItemInfo itemInfo = result.get(itemStack);
                itemInfo.totalCount += itemStack.getCount();
                itemInfo.slots.add(srcIndex);
            }
        }
        return result;
    }

    @Override
    public boolean canAttach(@NotNull ICoverable coverable, @NotNull Direction side) {
        return super.canAttach(coverable, side) &&
                coverable.getCapability(ForgeCapabilities.ITEM_HANDLER, attachedSide).isPresent();
    }

    @Override
    public InteractionResult onScrewdriverClick(Player playerIn, InteractionHand hand, BlockHitResult hitResult) {
        if (!coverHolder.getLevel().isClientSide) {
            createUI(playerIn);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, LazyOptional<T> defaultValue) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            if (!defaultValue.isPresent()) {
                return LazyOptional.empty();
            }
            IItemHandler delegate = defaultValue.<IItemHandler>cast().resolve().orElse(null);
            if (itemHandlerWrapper == null || itemHandlerWrapper.delegate != delegate) {
                this.itemHandlerWrapper = new CoverableItemHandlerWrapper(delegate);
            }
            return ForgeCapabilities.ITEM_HANDLER.orEmpty(capability, LazyOptional.of(() -> itemHandlerWrapper));
        }
        if (capability == GTCapability.CAPABILITY_CONTROLLABLE) {
            return GTCapability.CAPABILITY_CONTROLLABLE.orEmpty(capability, LazyOptional.of(() -> this));
        }
        return defaultValue;
    }

    @NotNull
    protected String getUITitle() {
        return "cover.conveyor.title";
    }

    protected void buildAdditionalUI(WidgetGroup group) {
        // Do nothing in the base implementation. This is intended to be overridden by subclasses.
    }

    protected void configureFilter() {
        // Do nothing in the base implementation. This is intended to be overridden by subclasses.
    }

    @Override
    public Widget createUIWidget() {
        final var group = new WidgetGroup(0, 0, 176, 137);
        group.addWidget(new LabelWidget(10, 5, Component.translatable(getUITitle(), GTValues.VN[tier]).getString()));

        if (createThroughputRow()) {
            group.addWidget(new IntInputWidget(10, 20, 156, 20, () -> this.transferRate, this::setTransferRate)
                    .setMin(1).setMax(maxItemTransferRate));
        }

        if (createConveyorModeRow()) {
            ioModeSwitch = new SwitchWidget(10, 45, 20, 20,
                    (clickData, value) -> {
                        setIo(value ? IO.IN : IO.OUT);
                        ioModeSwitch.setHoverTooltips(
                                LocalizationUtils.format("cover.conveyor.mode", LocalizationUtils.format(io.tooltip)));
                    })
                    .setTexture(
                            new GuiTextureGroup(GuiTextures.VANILLA_BUTTON, IO.OUT.icon),
                            new GuiTextureGroup(GuiTextures.VANILLA_BUTTON, IO.IN.icon))
                    .setPressed(io == IO.IN)
                    .setHoverTooltips(
                            LocalizationUtils.format("cover.conveyor.mode", LocalizationUtils.format(io.tooltip)));
            group.addWidget(ioModeSwitch);
        }

        if (createDistributionModeRow()) {
            group.addWidget(new EnumSelectorWidget<>(146, 67, 20, 20,
                    DistributionMode.VALUES, distributionMode, this::setDistributionMode));
        }

        if (createManualIOModeRow()) {
            group.addWidget(new EnumSelectorWidget<>(146, 107, 20, 20,
                    ManualIOMode.VALUES, manualIOMode, this::setManualIOMode)
                    .setHoverTooltips("cover.universal.manual_import_export.mode.description"));
        }

        if (createFilterRow()) {
            group.addWidget(filterHandler.createFilterSlotUI(125, 108));
            group.addWidget(filterHandler.createFilterConfigUI(10, 72, 156, 60));
        }

        buildAdditionalUI(group);

        return group;
    }

    protected boolean createThroughputRow() {
        return true;
    }

    protected boolean createFilterRow() {
        return true;
    }

    protected boolean createManualIOModeRow() {
        return true;
    }

    protected boolean createConveyorModeRow() {
        return true;
    }

    protected boolean createDistributionModeRow() {
        return true;
    }

    protected int getMaxStackSize() {
        return 1;
    }

    private class CoverableItemHandlerWrapper extends ItemHandlerDelegate {

        public CoverableItemHandlerWrapper(IItemHandler delegate) {
            super(delegate);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (io == IO.OUT && manualIOMode == ManualIOMode.DISABLED) {
                return stack;
            }
            if (manualIOMode == ManualIOMode.FILTERED && !filterHandler.test(stack)) {
                return stack;
            }
            return super.insertItem(slot, stack, simulate);
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (io == IO.IN && manualIOMode == ManualIOMode.DISABLED) {
                return ItemStack.EMPTY;
            }
            if (manualIOMode == ManualIOMode.FILTERED) {
                ItemStack result = super.extractItem(slot, amount, true);
                if (result.isEmpty() || !filterHandler.test(result)) {
                    return ItemStack.EMPTY;
                }
                return simulate ? result : super.extractItem(slot, amount, false);
            }
            return super.extractItem(slot, amount, simulate);
        }
    }
}
