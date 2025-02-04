package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandlers;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.machine.ConditionalSubscriptionHandler;
import com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerDelegate;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.api.transfer.fluid.ModifiableFluidHandlerWrapper;
import com.gregtechceu.gtceu.api.ui.component.EnumSelectorComponent;
import com.gregtechceu.gtceu.api.ui.component.IntInputComponent;
import com.gregtechceu.gtceu.api.ui.component.NumberInputComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.ParentUIComponent;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;
import com.gregtechceu.gtceu.common.cover.data.BucketMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;
import com.gregtechceu.gtceu.data.lang.LangHandler;
import com.gregtechceu.gtceu.utils.FluidStackHashStrategy;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/3/12
 * @implNote PumpCover
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PumpCover extends CoverBehavior implements IUICover, IControllable {

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
    protected final FilterHandler<FluidStack, FluidFilter> filterHandler;
    protected final ConditionalSubscriptionHandler subscriptionHandler;
    private NumberInputComponent<Integer> transferRateWidget;

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

    protected @Nullable IFluidHandlerModifiable getOwnFluidHandler() {
        return coverHolder.getFluidHandlerCap(attachedSide, false);
    }

    protected @Nullable IFluidHandler getAdjacentFluidHandler() {
        return GTTransferUtils.getAdjacentFluidHandler(coverHolder.getLevel(), coverHolder.getPos(), attachedSide)
                .resolve()
                .orElse(null);
    }

    //////////////////////////////////////
    // ***** Initialization ******//
    //////////////////////////////////////
    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public boolean canAttach() {
        return getOwnFluidHandler() != null;
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

    protected void setManualIOMode(ManualIOMode manualIOMode) {
        this.manualIOMode = manualIOMode;
        coverHolder.markDirty();
    }

    protected void update() {
        long timer = coverHolder.getOffsetTimer();
        if (timer % 5 != 0)
            return;

        if (mBLeftToTransferLastSecond > 0) {
            int platformTransferredFluid = doTransferFluids(mBLeftToTransferLastSecond);
            this.mBLeftToTransferLastSecond -= platformTransferredFluid;
        }

        if (timer % 20 == 0) {
            this.mBLeftToTransferLastSecond = transferRate * 20;
        }

        subscriptionHandler.updateSubscription();
    }

    private int doTransferFluids(int platformTransferLimit) {
        var adjacent = getAdjacentFluidHandler();
        var adjacentModifiable = adjacent instanceof IFluidHandlerModifiable modifiable ? modifiable :
                new ModifiableFluidHandlerWrapper(adjacent);
        var ownFluidHandler = getOwnFluidHandler();

        if (adjacent != null && ownFluidHandler != null) {
            return switch (io) {
                case IN -> doTransferFluidsInternal(adjacentModifiable, ownFluidHandler, platformTransferLimit);
                case OUT -> doTransferFluidsInternal(ownFluidHandler, adjacentModifiable, platformTransferLimit);
                default -> 0;
            };
        }
        return 0;
    }

    protected int doTransferFluidsInternal(IFluidHandlerModifiable source, IFluidHandlerModifiable destination,
                                           int platformTransferLimit) {
        return transferAny(source, destination, platformTransferLimit);
    }

    protected int transferAny(IFluidHandlerModifiable source, IFluidHandlerModifiable destination,
                              int platformTransferLimit) {
        return GTTransferUtils.transferFluidsFiltered(source, destination, filterHandler.getFilter(),
                platformTransferLimit);
    }

    protected enum TransferDirection {
        INSERT,
        EXTRACT
    }

    protected Map<FluidStack, Integer> enumerateDistinctFluids(IFluidHandlerModifiable fluidHandler,
                                                               TransferDirection direction) {
        final Map<FluidStack, Integer> summedFluids = new Object2IntOpenCustomHashMap<>(
                FluidStackHashStrategy.comparingAllButAmount());

        for (int tank = 0; tank < fluidHandler.getTanks(); tank++) {
            if (!canTransfer(fluidHandler, direction, tank))
                continue;

            FluidStack fluidStack = fluidHandler.getFluidInTank(tank);
            if (fluidStack.isEmpty())
                continue;

            summedFluids.putIfAbsent(fluidStack, 0);
            summedFluids.computeIfPresent(fluidStack, (stack, totalAmount) -> {
                return totalAmount + stack.getAmount();
            });
        }

        return summedFluids;
    }

    private static boolean canTransfer(IFluidHandlerModifiable fluidHandler, TransferDirection direction, int tank) {
        return switch (direction) {
            case INSERT -> fluidHandler.supportsFill(tank);
            case EXTRACT -> fluidHandler.supportsDrain(tank);
        };
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    public ParentUIComponent createUIWidget(UIAdapter<StackLayout> adapter) {
        final var group = UIContainers.stack(Sizing.fixed(176), Sizing.fixed(137));
        group.child(UIComponents.label(Component.translatable(getUITitle(), GTValues.VN[tier]))
                .positioning(Positioning.absolute(10, 5)));

        transferRateWidget = new IntInputComponent(Sizing.fixed(134), Sizing.fixed(20),
                this::getCurrentBucketModeTransferRate, this::setCurrentBucketModeTransferRate).setMin(0);
        transferRateWidget.positioning(Positioning.absolute(10, 20));
        setBucketMode(this.bucketMode); // initial input widget config happens here
        group.child(transferRateWidget);

        group.child(new EnumSelectorComponent<>(Sizing.fixed(20), Sizing.fixed(20),
                Arrays.stream(BucketMode.values()).filter(m -> m.multiplier <= maxFluidTransferRate).toList(),
                bucketMode, this::setBucketMode)
                .setTooltipSupplier(this::getBucketModeTooltip)
                .positioning(Positioning.absolute(146, 20)));

        group.child(new EnumSelectorComponent<>(Sizing.fixed(20), Sizing.fixed(20),
                List.of(IO.IN, IO.OUT), io, this::setIo)
                .positioning(Positioning.absolute(10, 45)));

        group.child(new EnumSelectorComponent<>(Sizing.fixed(20), Sizing.fixed(20),
                ManualIOMode.VALUES, manualIOMode, this::setManualIOMode)
                .tooltip(LangHandler.getMultiLang("cover.universal.manual_import_export.mode.description")))
                .positioning(Positioning.absolute(146, 107));

        group.child(filterHandler.createFilterSlotUI(125, 108));
        group.child(filterHandler.createFilterConfigUI(10, 72, 156, 60, adapter));

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

    protected void buildAdditionalUI(StackLayout group) {
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
