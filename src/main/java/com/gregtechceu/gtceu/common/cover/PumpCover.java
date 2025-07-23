package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IIOCover;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.cover.filter.CoverWithFluidFilter;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandlers;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.graphnet.GraphNetUtility;
import com.gregtechceu.gtceu.api.graphnet.net.NetEdge;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.path.NetPath;
import com.gregtechceu.gtceu.api.graphnet.pipenet.NodeExposingCapabilities;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.FluidTestObject;
import com.gregtechceu.gtceu.api.graphnet.traverse.EdgeDirection;
import com.gregtechceu.gtceu.api.graphnet.traverse.EdgeSelector;
import com.gregtechceu.gtceu.api.graphnet.traverse.NetClosestIterator;
import com.gregtechceu.gtceu.api.graphnet.traverse.ResilientNetClosestIterator;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelectorWidget;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.gui.widget.NumberInputWidget;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerDelegate;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.client.renderer.cover.CoverRenderer;
import com.gregtechceu.gtceu.common.cover.data.BucketMode;
import com.gregtechceu.gtceu.common.cover.data.DistributionMode;
import com.gregtechceu.gtceu.common.cover.data.FilterMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;
import com.gregtechceu.gtceu.common.cover.filter.MatchResult;
import com.gregtechceu.gtceu.common.pipelike.net.fluid.*;
import com.gregtechceu.gtceu.common.pipelike.net.item.ItemCapabilityObject;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.collections.ListHashSet;
import com.gregtechceu.gtceu.utils.function.BiIntConsumer;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.RequireRerender;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.*;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PumpCover extends CoverBehavior implements IIOCover, IUICover, IControllable, CoverWithFluidFilter {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PumpCover.class,
            CoverBehavior.MANAGED_FIELD_HOLDER);

    // .5b 2b 8b
    public static final Int2IntFunction PUMP_SCALING = tier -> 64 * (int) Math.pow(4, Math.min(tier - 1, GTValues.IV));

    public final int tier;
    public final int maxFluidTransferRate;
    @Persisted
    @DescSynced
    @Getter
    protected int transferRate;
    @Persisted
    @DescSynced
    @Getter
    @RequireRerender
    protected IO io = IO.OUT;
    @Persisted
    @DescSynced
    @Getter
    protected DistributionMode distributionMode = DistributionMode.FLOOD;
    @Persisted
    @DescSynced
    @Getter
    protected BucketMode bucketMode = BucketMode.MILLI_BUCKET;
    @Persisted
    @DescSynced
    @Getter
    protected ManualIOMode manualIOMode = ManualIOMode.DISABLED;

    @Persisted
    @DescSynced
    @Getter
    protected boolean isWorkingEnabled = true;
    protected int mBLeftToTransferLastSecond;

    @Persisted
    @DescSynced
    @Getter
    protected final FilterHandler<FluidStack, FluidFilter> filterHandler;
    protected final ConditionalSubscriptionHandler subscriptionHandler;
    private NumberInputWidget<Integer> transferRateWidget;

    protected final ObjectLinkedOpenHashSet<IFluidHandler> extractionRoundRobinCache = new ObjectLinkedOpenHashSet<>();
    protected final ObjectLinkedOpenHashSet<IFluidHandler> insertionRoundRobinCache = new ObjectLinkedOpenHashSet<>();

    protected @Nullable CoverRenderer rendererInverted;

    public PumpCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide, int tier,
                     int maxTransferRate) {
        super(definition, coverHolder, attachedSide);
        this.tier = tier;

        this.maxFluidTransferRate = maxTransferRate;
        this.transferRate = maxFluidTransferRate;
        this.mBLeftToTransferLastSecond = transferRate * 20;

        subscriptionHandler = new ConditionalSubscriptionHandler(coverHolder, this::update, this::isSubscriptionActive);
        filterHandler = FilterHandlers.fluid(this)
                .onFilterLoaded(f -> configureFilter())
                .onFilterUpdated(f -> configureFilter())
                .onFilterRemoved(f -> configureFilter());
    }

    public PumpCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide, int tier) {
        this(definition, coverHolder, attachedSide, tier, PUMP_SCALING.applyAsInt(tier));
    }

    protected boolean isSubscriptionActive() {
        return isWorkingEnabled() && getAdjacentFluidHandler() != null;
    }

    protected @Nullable IFluidHandler getOwnFluidHandler() {
        return coverHolder.getFluidHandlerCap(attachedSide, false);
    }

    protected @Nullable IFluidHandler getAdjacentFluidHandler() {
        BlockEntity blockEntity = coverHolder.getNeighbor(attachedSide);
        if (blockEntity == null) {
            return null;
        }
        var opt = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, attachedSide.getOpposite());
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

    @Override
    public boolean canAttach(@NotNull ICoverable coverable, @NotNull Direction side) {
        return super.canAttach(coverable, side) &&
                coverable.getCapability(ForgeCapabilities.FLUID_HANDLER, side).isPresent();
    }

    public void setIo(IO io) {
        if (io == IO.IN || io == IO.OUT) {
            this.io = io;
        }
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

    //////////////////////////////////////
    // ***** Transfer Logic *****//
    //////////////////////////////////////

    public void setTransferRate(int milliBucketsPerTick) {
        this.transferRate = Math.min(Math.max(milliBucketsPerTick, 0), maxFluidTransferRate);
    }

    public void setBucketMode(BucketMode bucketMode) {
        var oldMultiplier = this.bucketMode.multiplier;
        var newMultiplier = bucketMode.multiplier;

        this.bucketMode = bucketMode;

        if (transferRateWidget == null) return;

        if (oldMultiplier > newMultiplier) {
            transferRateWidget.setValue(getCurrentBucketModeTransferRate());
        }

        transferRateWidget.setMax(maxFluidTransferRate / bucketMode.multiplier);

        if (newMultiplier > oldMultiplier) {
            transferRateWidget.setValue(getCurrentBucketModeTransferRate());
        }
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

    public void update() {
        long timer = coverHolder.getOffsetTimer();
        if (isWorkingEnabled && getFluidsLeftToTransfer() > 0) {
            IFluidHandler fluidHandler = getAdjacentFluidHandler();
            IFluidHandler myFluidHandler = getOwnFluidHandler();
            // if we're on a pipe, we shouldn't see said pipe's sided handler, but instead its unsided handler.
            IFluidHandler h = FluidCapabilityObject.instanceOf(myFluidHandler);
            if (h != null) myFluidHandler = h;
            if (myFluidHandler != null && fluidHandler != null) {
                if (io == IO.OUT) {
                    performTransferOnUpdate(myFluidHandler, fluidHandler);
                } else {
                    performTransferOnUpdate(fluidHandler, myFluidHandler);
                }
            }
        }
        if (timer % 20 == 0) {
            refreshBuffer(transferRate);
        }
    }

    public int getFluidsLeftToTransfer() {
        return mBLeftToTransferLastSecond;
    }

    public void reportFluidsTransfer(int transferred) {
        mBLeftToTransferLastSecond -= transferred;
    }

    protected void refreshBuffer(int transferRate) {
        this.mBLeftToTransferLastSecond = transferRate;
    }

    protected void performTransferOnUpdate(@NotNull IFluidHandler sourceHandler, @NotNull IFluidHandler destHandler) {
        reportFluidsTransfer(performTransfer(sourceHandler, destHandler, false, i -> 0,
                i -> getFluidsLeftToTransfer(), null));
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
    protected int performTransfer(@NotNull IFluidHandler sourceHandler, @NotNull IFluidHandler destHandler,
                                  boolean byFilterSlot, @NotNull IntUnaryOperator minTransfer,
                                  @NotNull IntUnaryOperator maxTransfer, @Nullable BiIntConsumer transferReport) {
        FilterHandler<FluidStack, FluidFilter> filter = this.getFilterHandler();
        byFilterSlot = byFilterSlot && filter.isFilterPresent(); // can't be by filter slot if there is no filter
        Object2IntLinkedOpenHashMap<FluidTestObject> contained = new Object2IntLinkedOpenHashMap<>();
        for (int i = 0; i < sourceHandler.getTanks(); i++) {
            FluidStack contents = sourceHandler.getFluidInTank(i);
            if (!contents.isEmpty()) contained.addTo(new FluidTestObject(contents), contents.getAmount());
        }
        var iter = contained.object2IntEntrySet().fastIterator();
        int totalTransfer = 0;
        while (iter.hasNext()) {
            var content = iter.next();
            MatchResult match = null;
            if (!filter.isFilterPresent() ||
                    (match = filter.getFilter().match(content.getKey().recombine(content.getIntValue()))).isMatched()) {
                int filterSlot = -1;
                if (byFilterSlot) {
                    // we know it is not null, because if it were byFilterSlot would be false.
                    assert filter.isFilterPresent();
                    filterSlot = match.getFilterIndex();
                }
                int min = Math.max(minTransfer.applyAsInt(filterSlot), 1);
                int max = maxTransfer.applyAsInt(filterSlot);
                if (max < min) continue;

                if (content.getIntValue() < min) continue;
                int transfer = Math.min(content.getIntValue(), max);
                transfer = doInsert(destHandler, content.getKey(), transfer, true);
                if (transfer < min) continue;
                transfer = doExtract(sourceHandler, content.getKey(), transfer, true);
                if (transfer < min) continue;
                doExtract(sourceHandler, content.getKey(), transfer, false);
                doInsert(destHandler, content.getKey(), transfer, false);
                if (transferReport != null) transferReport.accept(filterSlot, transfer);
                totalTransfer += transfer;
            }
        }
        return totalTransfer;
    }

    protected ObjectLinkedOpenHashSet<IFluidHandler> getRoundRobinCache(boolean extract, boolean simulate) {
        ObjectLinkedOpenHashSet<IFluidHandler> set = extract ? extractionRoundRobinCache : insertionRoundRobinCache;
        return simulate ? set.clone() : set;
    }

    protected int doExtract(@NotNull IFluidHandler handler, FluidTestObject testObject, int count,
                            boolean simulate) {
        FluidCapabilityObject cap;
        if (distributionMode == DistributionMode.FLOOD || (cap = FluidCapabilityObject.instanceOf(handler)) == null)
            return simpleExtract(handler, testObject, count, simulate);
        NetNode origin = cap.getNode();
        // if you find yourself here because you added a new distribution mode, and now it won't compile, good luck.
        return switch (distributionMode) {
            case ROUND_ROBIN -> {
                FluidNetworkView view = cap.getNetworkView(FluidCapabilityObject.facingOf(handler));
                Iterator<IFluidHandler> iter = view.getHandler().getBackingHandlers().iterator();
                ObjectLinkedOpenHashSet<IFluidHandler> cache = getRoundRobinCache(true, simulate);
                Set<IFluidHandler> backlog = new ObjectOpenHashSet<>();
                Reference2IntOpenHashMap<NetNode> flowLimitCache = new Reference2IntOpenHashMap<>();
                Reference2BooleanOpenHashMap<NetNode> lossyCache = new Reference2BooleanOpenHashMap<>();
                List<Runnable> postActions = new ObjectArrayList<>();
                int available = count;
                while (available > 0) {
                    if (!cache.isEmpty() && backlog.remove(cache.first())) {
                        IFluidHandler candidate = cache.first();
                        NetNode linked = view.getBiMap().get(candidate);
                        if (linked == null) {
                            cache.removeFirst();
                            continue;
                        } else {
                            cache.addAndMoveToLast(candidate);
                        }
                        available = rrExtract(testObject, simulate, origin, flowLimitCache, available, candidate,
                                linked, lossyCache, postActions);
                        continue;
                    }
                    if (iter.hasNext()) {
                        IFluidHandler candidate = iter.next();
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
                                    linked, lossyCache, postActions);
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
                if (!simulate) {
                    postActions.forEach(Runnable::run);
                }
                yield count - available;
            }
            case EQUALIZED -> {
                NetClosestIterator gather = new NetClosestIterator(origin,
                        EdgeSelector.filtered(EdgeDirection.INCOMING,
                                GraphNetUtility.edgeSelectorBlacklist(testObject)));
                Object2ObjectOpenHashMap<NetNode, IFluidHandler> candidates = new Object2ObjectOpenHashMap<>();
                while (gather.hasNext()) {
                    NetNode node = gather.next();
                    if (node instanceof NodeExposingCapabilities exposer) {
                        IFluidHandler h = exposer.getProvider().getCapability(
                                ForgeCapabilities.FLUID_HANDLER,
                                exposer.exposedFacing()).resolve().orElse(null);
                        if (h != null && FluidCapabilityObject.instanceOf(h) == null) {
                            candidates.put(node, h);
                        }
                    }
                }
                if (candidates.isEmpty()) yield 0;
                int largestMin = count / candidates.size();
                if (largestMin <= 0) yield 0;
                for (IFluidHandler value : candidates.values()) {
                    largestMin = Math.min(largestMin, simpleExtract(value, testObject, largestMin, true));
                    if (largestMin <= 0) yield 0;
                }
                // binary search for largest scale that doesn't exceed flow limits
                Reference2IntOpenHashMap<NetNode> flowLimitCache = new Reference2IntOpenHashMap<>();
                Int2ObjectArrayMap<Reference2IntOpenHashMap<NetNode>> flows = new Int2ObjectArrayMap<>();
                Int2IntOpenHashMap losses = new Int2IntOpenHashMap();
                Int2ObjectArrayMap<List<Runnable>> postActions = new Int2ObjectArrayMap<>();
                Reference2BooleanOpenHashMap<NetNode> lossyCache = new Reference2BooleanOpenHashMap<>();
                largestMin = GTUtil.binarySearchInt(0, largestMin, l -> {
                    if (flows.containsKey(l) && flows.get(l) == null) return false;
                    ResilientNetClosestIterator backwardFrontier = null;
                    Reference2IntOpenHashMap<NetNode> localFlows = new Reference2IntOpenHashMap<>();
                    List<Runnable> postAction = new ObjectArrayList<>();
                    for (NetNode node : candidates.keySet()) {
                        ListHashSet<NetPath> pathCache = FluidCapabilityObject.getNetworkView(node)
                                .getPathCache(origin);
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
                                        n -> FluidCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) <=
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
                                    n -> FluidCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) -
                                            localFlows.getInt(n),
                                    e -> !e.test(testObject),
                                    n -> FluidCapabilityObject.isLossyNodeCached(lossyCache, n, testObject));
                            if (extract > 0) {
                                needed -= extract;
                                ImmutableList<NetNode> asList = path.getOrderedNodes().asList();
                                for (int j = 0; j < asList.size(); j++) {
                                    NetNode n = asList.get(j);
                                    localFlows.put(n, localFlows.getInt(n) + extract);
                                    if (FluidCapabilityObject.isLossyNodeCached(lossyCache, n, testObject)) {
                                        postAction.add(() -> FluidCapabilityObject.handleLoss(n, extract, testObject));
                                        IFluidHandler h = candidates.get(node);
                                        losses.put(l, losses.get(l) + extract);
                                    }
                                }
                            }
                        }
                        if (needed > 0) {
                            flows.put(l, null);
                            return false;
                        }
                    }
                    flows.put(l, localFlows);
                    postActions.put(l, postAction);
                    return true;
                }, false);
                if (largestMin <= 0 || flows.get(largestMin) == null) yield 0;
                if (!simulate) {
                    for (IFluidHandler value : candidates.values()) {
                        simpleExtract(value, testObject, largestMin, false);
                    }
                    postActions.get(largestMin).forEach(Runnable::run);
                    for (var e : flows.get(largestMin).reference2IntEntrySet()) {
                        Runnable post = FluidCapabilityObject.reportFlow(e.getKey(), e.getIntValue(), testObject);
                        if (post != null) post.run();
                    }
                }
                yield largestMin * candidates.size() - losses.get(largestMin);
            }
            case FLOOD -> 0; // how are you here?
        };
    }

    protected int rrExtract(FluidTestObject testObject, boolean simulate, NetNode origin,
                            Reference2IntOpenHashMap<NetNode> flowLimitCache, int available, IFluidHandler candidate,
                            NetNode linked, Reference2BooleanOpenHashMap<NetNode> lossyCache,
                            List<Runnable> postActions) {
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
                            n -> FluidCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) <= 0,
                            e -> !e.test(testObject), forwardFrontier, backwardFrontier);
                    if (path == null) break;
                    int i = pathCache.size();
                    while (i > 0 && pathCache.get(i - 1).getWeight() > path.getWeight()) {
                        i--;
                    }
                    if (!pathCache.addSensitive(i, path)) break;
                }
                int extract = attemptPath(path, extractable,
                        n -> FluidCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject),
                        e -> !e.test(testObject),
                        n -> FluidCapabilityObject.isLossyNodeCached(lossyCache, n, testObject));
                if (extract > 0) {
                    extractable -= extract;
                    available -= extract;
                    ImmutableList<NetNode> asList = path.getOrderedNodes().asList();
                    for (int j = 0; j < asList.size(); j++) {
                        NetNode n = asList.get(j);
                        if (!simulate) {
                            // reporting temp change can cause temperature pipe destruction which causes
                            // graph modification while iterating.
                            Runnable post = FluidCapabilityObject.reportFlow(n, extract, testObject);
                            if (post != null) postActions.add(post);
                        }
                        flowLimitCache.put(n, flowLimitCache.getInt(n) - extract);
                        if (FluidCapabilityObject.isLossyNodeCached(lossyCache, n, testObject)) {
                            // reporting loss can cause misc pipe destruction which causes
                            // graph modification while iterating.
                            if (!simulate)
                                postActions.add(() -> FluidCapabilityObject.handleLoss(n, extract, testObject));
                            // a lossy node will prevent receiving extracted fluid
                            extractable += extract;
                            available += extract;
                            break;
                        }
                    }
                    if (!simulate) simpleExtract(candidate, testObject, extract, false);
                }
            }
        }
        return available;
    }

    protected int simpleExtract(@NotNull IFluidHandler destHandler, FluidTestObject testObject, int count,
                                boolean simulate) {
        FluidStack ext = destHandler.drain(testObject.recombine(count),
                !simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
        return ext.isEmpty() ? 0 : ext.getAmount();
    }

    protected int doInsert(@NotNull IFluidHandler handler, FluidTestObject testObject, int count,
                           boolean simulate) {
        FluidCapabilityObject cap;
        if (distributionMode == DistributionMode.FLOOD || (cap = FluidCapabilityObject.instanceOf(handler)) == null)
            return simpleInsert(handler, testObject, count, simulate);
        NetNode origin = cap.getNode();
        // if you find yourself here because you added a new distribution mode, and now it won't compile, ood luck.
        return switch (distributionMode) {
            case ROUND_ROBIN -> {
                FluidNetworkView view = cap.getNetworkView(FluidCapabilityObject.facingOf(handler));
                Iterator<IFluidHandler> iter = view.getHandler().getBackingHandlers().iterator();
                ObjectLinkedOpenHashSet<IFluidHandler> cache = getRoundRobinCache(false, simulate);
                Set<IFluidHandler> backlog = new ObjectOpenHashSet<>();
                Reference2IntOpenHashMap<NetNode> flowLimitCache = new Reference2IntOpenHashMap<>();
                Reference2BooleanOpenHashMap<NetNode> lossyCache = new Reference2BooleanOpenHashMap<>();
                List<Runnable> postActions = new ObjectArrayList<>();
                int available = count;
                while (available > 0) {
                    if (!cache.isEmpty() && backlog.remove(cache.first())) {
                        IFluidHandler candidate = cache.first();
                        NetNode linked = view.getBiMap().get(candidate);
                        if (linked == null) {
                            cache.removeFirst();
                            continue;
                        } else {
                            cache.addAndMoveToLast(candidate);
                        }
                        available = rrInsert(testObject, simulate, origin, flowLimitCache, available, candidate,
                                linked, lossyCache, postActions);
                        continue;
                    }
                    if (iter.hasNext()) {
                        IFluidHandler candidate = iter.next();
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
                                    linked, lossyCache, postActions);
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
                if (!simulate) {
                    postActions.forEach(Runnable::run);
                }
                yield count - available;
            }
            case EQUALIZED -> {
                // only consider destinations that are not on the other side of a filter that rejects our test object
                NetClosestIterator gather = new NetClosestIterator(origin,
                        EdgeSelector.filtered(EdgeDirection.OUTGOING,
                                GraphNetUtility.edgeSelectorBlacklist(testObject)));
                Object2ObjectOpenHashMap<NetNode, IFluidHandler> candidates = new Object2ObjectOpenHashMap<>();
                while (gather.hasNext()) {
                    NetNode node = gather.next();
                    if (node instanceof NodeExposingCapabilities exposer) {
                        IFluidHandler h = exposer.getProvider().getCapability(
                                ForgeCapabilities.FLUID_HANDLER,
                                exposer.exposedFacing()).resolve().orElse(null);
                        if (h != null && FluidCapabilityObject.instanceOf(h) == null) {
                            candidates.put(node, h);
                        }
                    }
                }
                if (candidates.isEmpty()) yield 0;
                int largestMin = count / candidates.size();
                if (largestMin <= 0) yield 0;
                for (IFluidHandler value : candidates.values()) {
                    largestMin = Math.min(largestMin, simpleInsert(value, testObject, largestMin, true));
                    if (largestMin <= 0) yield 0;
                }
                // binary search for largest scale that doesn't exceed flow limits
                Reference2IntOpenHashMap<NetNode> flowLimitCache = new Reference2IntOpenHashMap<>();
                Int2ObjectArrayMap<Reference2IntOpenHashMap<NetNode>> flows = new Int2ObjectArrayMap<>();
                Int2ObjectArrayMap<Reference2IntOpenHashMap<IFluidHandler>> losses = new Int2ObjectArrayMap<>();
                Int2ObjectArrayMap<List<Runnable>> postActions = new Int2ObjectArrayMap<>();
                Reference2BooleanOpenHashMap<NetNode> lossyCache = new Reference2BooleanOpenHashMap<>();
                FluidNetworkView view = FluidCapabilityObject.getNetworkView(origin);
                largestMin = GTUtil.binarySearchInt(0, largestMin, l -> {
                    if (flows.containsKey(l) && flows.get(l) == null) return false;
                    ResilientNetClosestIterator forwardFrontier = null;
                    Reference2IntOpenHashMap<NetNode> localFlows = new Reference2IntOpenHashMap<>();
                    List<Runnable> postAction = new ObjectArrayList<>();
                    Reference2IntOpenHashMap<IFluidHandler> loss = new Reference2IntOpenHashMap<>();
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
                                        n -> FluidCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) <=
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
                                    n -> FluidCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) -
                                            localFlows.getInt(n),
                                    e -> !e.test(testObject),
                                    n -> FluidCapabilityObject.isLossyNodeCached(lossyCache, n, testObject));
                            if (insert > 0) {
                                needed -= insert;
                                ImmutableList<NetNode> asList = path.getOrderedNodes().asList();
                                for (int j = 0; j < asList.size(); j++) {
                                    NetNode n = asList.get(j);
                                    localFlows.put(n, localFlows.getInt(n) + insert);
                                    if (FluidCapabilityObject.isLossyNodeCached(lossyCache, n, testObject)) {
                                        postAction.add(() -> FluidCapabilityObject.handleLoss(n, insert, testObject));
                                        IFluidHandler h = candidates.get(node);
                                        loss.put(h, loss.getInt(h) + insert);
                                    }
                                }
                            }
                        }
                        if (needed > 0) {
                            flows.put(l, null);
                            return false;
                        }
                    }
                    flows.put(l, localFlows);
                    postActions.put(l, postAction);
                    losses.put(l, loss);
                    return true;
                }, false);
                if (largestMin <= 0 || flows.get(largestMin) == null) yield 0;
                if (!simulate) {
                    Reference2IntOpenHashMap<IFluidHandler> loss = losses.get(largestMin);
                    for (IFluidHandler value : candidates.values()) {
                        simpleInsert(value, testObject, largestMin - loss.getInt(value), false);
                    }
                    postActions.get(largestMin).forEach(Runnable::run);
                    for (var e : flows.get(largestMin).reference2IntEntrySet()) {
                        Runnable post = FluidCapabilityObject.reportFlow(e.getKey(), e.getIntValue(), testObject);
                        if (post != null) post.run();
                    }
                }
                yield largestMin * candidates.size();
            }
            case FLOOD -> 0; // how are you here?
        };
    }

    protected int rrInsert(FluidTestObject testObject, boolean simulate, NetNode origin,
                           Reference2IntOpenHashMap<NetNode> flowLimitCache, int available, IFluidHandler candidate,
                           NetNode linked, Reference2BooleanOpenHashMap<NetNode> lossyCache,
                           List<Runnable> postActions) {
        int insertable = simpleInsert(candidate, testObject, available, true);
        if (insertable > 0) {
            ListHashSet<NetPath> pathCache = ItemCapabilityObject.getNetworkView(origin).getPathCache(linked);
            Iterator<NetPath> iterator = pathCache.iterator();
            ResilientNetClosestIterator forwardFrontier = null;
            ResilientNetClosestIterator backwardFrontier = null;
            pathloop:
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
                            n -> FluidCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject) <= 0,
                            e -> !e.test(testObject), forwardFrontier, backwardFrontier);
                    if (path == null) break;
                    int i = pathCache.size();
                    while (i > 0 && pathCache.get(i - 1).getWeight() > path.getWeight()) {
                        i--;
                    }
                    if (!pathCache.addSensitive(i, path)) break;
                }
                int insert = attemptPath(path, insertable,
                        n -> FluidCapabilityObject.getFlowLimitCached(flowLimitCache, n, testObject),
                        e -> !e.test(testObject),
                        n -> FluidCapabilityObject.isLossyNodeCached(lossyCache, n, testObject));
                if (insert > 0) {
                    insertable -= insert;
                    available -= insert;
                    ImmutableList<NetNode> asList = path.getOrderedNodes().asList();
                    for (int j = 0; j < asList.size(); j++) {
                        NetNode n = asList.get(j);
                        if (!simulate) {
                            // reporting temp change can cause temperature pipe destruction which causes
                            // graph modification while iterating.
                            Runnable post = FluidCapabilityObject.reportFlow(n, insert, testObject);
                            if (post != null) postActions.add(post);
                        }
                        flowLimitCache.put(n, flowLimitCache.getInt(n) - insert);
                        if (FluidCapabilityObject.isLossyNodeCached(lossyCache, n, testObject)) {
                            // reporting loss can cause misc pipe destruction which causes
                            // graph modification while iterating.
                            if (!simulate)
                                postActions.add(() -> FluidCapabilityObject.handleLoss(n, insert, testObject));
                            // a lossy node will prevent filling the target
                            continue pathloop;
                        }
                    }
                    if (!simulate) simpleInsert(candidate, testObject, insert, false);
                }
            }
        }
        return available;
    }

    protected int simpleInsert(@NotNull IFluidHandler destHandler, FluidTestObject testObject, int count,
                               boolean simulate) {
        return destHandler.fill(testObject.recombine(count),
                simulate ? IFluidHandler.FluidAction.SIMULATE : IFluidHandler.FluidAction.EXECUTE);
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

    protected boolean checkInputFluid(FluidStack fluidStack) {
        return filterHandler.test(fluidStack);
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public Widget createUIWidget() {
        final var group = new WidgetGroup(0, 0, 176, 137);
        group.addWidget(new LabelWidget(10, 5, Component.translatable(getUITitle(), GTValues.VN[tier]).getString()));

        transferRateWidget = new IntInputWidget(10, 20, 134, 20,
                this::getCurrentBucketModeTransferRate, this::setCurrentBucketModeTransferRate).setMin(0);
        setBucketMode(this.bucketMode); // initial input widget config happens here
        group.addWidget(transferRateWidget);

        group.addWidget(new EnumSelectorWidget<>(
                146, 20, 20, 20,
                Arrays.stream(BucketMode.values()).filter(m -> m.multiplier <= maxFluidTransferRate).toList(),
                bucketMode, this::setBucketMode).setTooltipSupplier(this::getBucketModeTooltip));

        group.addWidget(new EnumSelectorWidget<>(10, 45, 20, 20, List.of(IO.IN, IO.OUT), io, this::setIo));

        group.addWidget(new EnumSelectorWidget<>(146, 107, 20, 20,
                ManualIOMode.VALUES, manualIOMode, this::setManualIOMode)
                .setHoverTooltips("cover.universal.manual_import_export.mode.description"));

        group.addWidget(filterHandler.createFilterSlotUI(125, 108));
        group.addWidget(filterHandler.createFilterConfigUI(10, 72, 156, 60));

        buildAdditionalUI(group);

        return group;
    }

    private List<Component> getBucketModeTooltip(BucketMode mode, String langKey) {
        return List.of(
                Component.translatable(langKey).append(Component.translatable("gtceu.gui.content.units.per_tick")));
    }

    private int getCurrentBucketModeTransferRate() {
        return this.transferRate / this.bucketMode.multiplier;
    }

    private void setCurrentBucketModeTransferRate(int transferRate) {
        this.setTransferRate(transferRate * this.bucketMode.multiplier);
    }

    @NotNull
    protected String getUITitle() {
        return "cover.pump.title";
    }

    protected void buildAdditionalUI(WidgetGroup group) {
        // Do nothing in the base implementation. This is intended to be overridden by subclasses.
    }

    protected void configureFilter() {
        // Do nothing in the base implementation. This is intended to be overridden by subclasses.
    }

    /////////////////////////////////////
    // *** CAPABILITY OVERRIDE ***//
    /////////////////////////////////////

    private CoverableFluidHandlerWrapper fluidHandlerWrapper;

    @Nullable
    @Override
    public IFluidHandlerModifiable getFluidHandlerCap(@Nullable IFluidHandlerModifiable defaultValue) {
        if (defaultValue == null) {
            return null;
        }
        if (fluidHandlerWrapper == null || fluidHandlerWrapper.delegate != defaultValue) {
            this.fluidHandlerWrapper = new CoverableFluidHandlerWrapper(defaultValue);
        }
        return fluidHandlerWrapper;
    }

    private class CoverableFluidHandlerWrapper extends FluidHandlerDelegate {

        public CoverableFluidHandlerWrapper(IFluidHandlerModifiable delegate) {
            super(delegate);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (io == IO.OUT && manualIOMode == ManualIOMode.DISABLED) {
                return 0;
            }
            if (!filterHandler.test(resource) && manualIOMode == ManualIOMode.FILTERED) {
                return 0;
            }
            return super.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (io == IO.IN && manualIOMode == ManualIOMode.DISABLED) {
                return FluidStack.EMPTY;
            }
            if (manualIOMode == ManualIOMode.FILTERED && !filterHandler.test(resource)) {
                return FluidStack.EMPTY;
            }
            return super.drain(resource, action);
        }
    }
}
