package com.gregtechceu.gtceu.common.cover.voiding;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.cover.filter.SimpleFluidFilter;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelectorWidget;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.gui.widget.NumberInputWidget;
import com.gregtechceu.gtceu.client.renderer.pipe.cover.CoverRenderer;
import com.gregtechceu.gtceu.client.renderer.pipe.cover.CoverRendererBuilder;
import com.gregtechceu.gtceu.common.cover.data.BucketMode;
import com.gregtechceu.gtceu.common.cover.data.VoidingMode;
import com.gregtechceu.gtceu.utils.GTTransferUtils;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import it.unimi.dsi.fastutil.objects.Object2LongMaps;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class AdvancedFluidVoidingCover extends FluidVoidingCover {

    @Persisted
    @DescSynced
    @Getter
    private VoidingMode voidingMode = VoidingMode.VOID_ANY;

    @Persisted
    @DescSynced
    @Getter
    protected int globalTransferSizeMillibuckets = 1;
    @Persisted
    @DescSynced
    @Getter
    private BucketMode transferBucketMode = BucketMode.MILLI_BUCKET;

    private NumberInputWidget<Integer> stackSizeInput;
    private EnumSelectorWidget<BucketMode> stackSizeBucketModeInput;

    public AdvancedFluidVoidingCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
    }

    @Override
    protected CoverRenderer buildRenderer() {
        return new CoverRendererBuilder(GTCEu.id("block/cover/overlay_fluid_voiding_advanced"),
                GTCEu.id("block/cover/overlay_fluid_voiding_advanced_emissive")).build();
    }

    //////////////////////////////////////////////
    // *********** COVER LOGIC ***********//
    //////////////////////////////////////////////

    @Override
    protected void doTransferFluids() {
        IFluidHandler myFluidHandler = coverHolder.getCapability(ForgeCapabilities.FLUID_HANDLER,
                attachedSide).resolve().orElse(null);
        if (myFluidHandler == null) {
            return;
        }
        switch (voidingMode) {
            case VOID_ANY -> GTTransferUtils.transferFluidsFiltered(myFluidHandler, nullFluidTank,
                    getFilterHandler()::test);
            case VOID_OVERFLOW -> voidOverflow(myFluidHandler, getFilterHandler()::test,
                    this.globalTransferSizeMillibuckets);
        }
        subscriptionHandler.updateSubscription();
    }

    /**
     * Performs one tick worth of Keep Exact behavior.
     *
     * @param sourceHandler source(s) to move fluids from
     * @param fluidFilter   a predicate which determines what fluids may be moved
     * @param keepAmount    the desired amount in milliBuckets of a particular fluid in the destination
     */
    protected void voidOverflow(final IFluidHandler sourceHandler,
                                final Predicate<FluidStack> fluidFilter,
                                int keepAmount) {
        if (sourceHandler == null || fluidFilter == null) {
            return;
        }
        var fluidAmounts = enumerateDistinctFluids(sourceHandler, TransferDirection.EXTRACT);

        for (var entry : Object2LongMaps.fastIterable(fluidAmounts)) {
            var stack = entry.getKey();
            long presentAmount = entry.getLongValue();
            if (this.getFilterHandler().isFilterPresent() &&
                    voidingMode == VoidingMode.VOID_OVERFLOW) {
                keepAmount = this.getFilterHandler().getFilter()
                        .getTransferLimit(stack, maxFluidTransferRate);
            }
            if (keepAmount <= 0L || keepAmount > presentAmount || !getFilterHandler().test(stack)) {
                continue;
            }

            long diff = presentAmount - targetAmount;
            for (int op : GTMath.split(diff)) {
                var toDrain = new FluidStack(stack, op);
                sourceHandler.drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private int getFilteredFluidAmount(FluidStack fluidStack) {
        if (!filterHandler.isFilterPresent())
            return globalTransferSizeMillibuckets;

        FluidFilter filter = filterHandler.getFilter();
        return filter.isBlackList() ? globalTransferSizeMillibuckets : filter.testFluidAmount(fluidStack);
    }

    public void setVoidingMode(VoidingMode voidingMode) {
        this.voidingMode = voidingMode;

        configureStackSizeInput();

        if (!this.isRemote()) {
            configureFilter();
        }
    }

    private void setTransferBucketMode(BucketMode transferBucketMode) {
        var oldMultiplier = this.transferBucketMode.multiplier;
        var newMultiplier = transferBucketMode.multiplier;

        this.transferBucketMode = transferBucketMode;

        if (stackSizeInput == null) return;
        stackSizeInput.setValue(getCurrentBucketModeTransferSize());
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    protected @NotNull String getUITitle() {
        return "cover.fluid.voiding.advanced.title";
    }

    @Override
    protected void buildAdditionalUI(WidgetGroup group) {
        group.addWidget(
                new EnumSelectorWidget<>(146, 20, 20, 20, VoidingMode.values(), voidingMode, this::setVoidingMode));

        this.stackSizeInput = new IntInputWidget(35, 20, 84, 20,
                this::getCurrentBucketModeTransferSize, this::setCurrentBucketModeTransferSize).setMin(1)
                .setMax(Integer.MAX_VALUE);
        configureStackSizeInput();
        group.addWidget(this.stackSizeInput);

        this.stackSizeBucketModeInput = new EnumSelectorWidget<>(121, 20, 20, 20, BucketMode.values(),
                transferBucketMode, this::setTransferBucketMode);
        group.addWidget(this.stackSizeBucketModeInput);
    }

    private int getCurrentBucketModeTransferSize() {
        return this.globalTransferSizeMillibuckets / this.transferBucketMode.multiplier;
    }

    private void setCurrentBucketModeTransferSize(int transferSize) {
        this.globalTransferSizeMillibuckets = Math.max(transferSize * this.transferBucketMode.multiplier, 0);
    }

    @Override
    protected void configureFilter() {
        if (filterHandler.getFilter() instanceof SimpleFluidFilter filter) {
            filter.setMaxStackSize(voidingMode == VoidingMode.VOID_ANY ? 1 : Integer.MAX_VALUE);
        }

        configureStackSizeInput();
    }

    private void configureStackSizeInput() {
        if (this.stackSizeInput == null || stackSizeBucketModeInput == null)
            return;

        this.stackSizeInput.setVisible(shouldShowStackSize());
        this.stackSizeBucketModeInput.setVisible(shouldShowStackSize());
    }

    private boolean shouldShowStackSize() {
        if (this.voidingMode == VoidingMode.VOID_ANY)
            return false;

        if (!this.filterHandler.isFilterPresent())
            return true;

        return this.filterHandler.getFilter().isBlackList();
    }

    //////////////////////////////////////
    // ***** LDLib SyncData ******//
    //////////////////////////////////////

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            AdvancedFluidVoidingCover.class, FluidVoidingCover.MANAGED_FIELD_HOLDER);

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }
}
