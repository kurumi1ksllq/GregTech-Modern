package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.cover.filter.SimpleFluidFilter;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.FluidTestObject;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelectorWidget;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.api.gui.widget.NumberInputWidget;
import com.gregtechceu.gtceu.common.cover.data.BucketMode;
import com.gregtechceu.gtceu.common.cover.data.TransferMode;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntUnaryOperator;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidRegulatorCover extends PumpCover {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidRegulatorCover.class,
            PumpCover.MANAGED_FIELD_HOLDER);

    private static final int MAX_STACK_SIZE = 2_048_000_000; // Capacity of quantum tank IX

    @Persisted
    @DescSynced
    @Getter
    private TransferMode transferMode = TransferMode.TRANSFER_ANY;
    protected boolean noTransferDueToMinimum = false;
    @Persisted
    @DescSynced
    @Getter
    protected int globalTransferLimit;

    private NumberInputWidget<Integer> transferSizeInput;
    private EnumSelectorWidget<BucketMode> bucketModeInput;

    public FluidRegulatorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide, int tier,
                               int maxTransferRate) {
        super(definition, coverHolder, attachedSide, tier, maxTransferRate);
    }

    public FluidRegulatorCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide, int tier) {
        this(definition, coverHolder, attachedSide, tier, PUMP_SCALING.applyAsInt(tier));
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    //////////////////////////////////////
    // ***** Transfer Logic ******//
    //////////////////////////////////////

    @Override
    protected void refreshBuffer(int transferRate) {
        if (this.transferMode == TransferMode.TRANSFER_EXACT && noTransferDueToMinimum) {
            FluidFilter filter = this.getFilterHandler().getFilter();
            if (filter != FluidFilter.EMPTY) {
                this.noTransferDueToMinimum = false;
                this.mBLeftToTransferLastSecond += transferRate;
                int max = filter.getMaxTransferSize();
                if (this.mBLeftToTransferLastSecond > max) {
                    this.mBLeftToTransferLastSecond = max;
                }
                return;
            }
        }
        super.refreshBuffer(transferRate);
    }

    @Override
    protected void performTransferOnUpdate(@NotNull IFluidHandler sourceHandler, @NotNull IFluidHandler destHandler) {
        if (transferMode != TransferMode.TRANSFER_EXACT) {
            super.performTransferOnUpdate(sourceHandler, destHandler);
            return;
        }
        FilterHandler<FluidStack, FluidFilter> filter = this.getFilterHandler();
        if (filter == null) return;
        IntUnaryOperator maxflow = s -> {
            int limit = filter.getFilter().getTransferLimit(s);
            if (getFluidsLeftToTransfer() < limit) {
                noTransferDueToMinimum = true;
                return 0;
            } else return limit;
        };
        performTransfer(sourceHandler, destHandler, true, maxflow, maxflow, (a, b) -> reportFluidsTransfer(b));
    }

    @Override
    protected int simpleInsert(@NotNull IFluidHandler destHandler, FluidTestObject testObject, int count,
                               boolean simulate) {
        if (transferMode == TransferMode.KEEP_EXACT) {
            assert getFilterHandler().isFilterPresent();
            int kept = getFilterHandler().getFilter().getTransferLimit(testObject.recombine());
            count = Math.min(count, kept - computeContained(destHandler, testObject));
        }
        return super.simpleInsert(destHandler, testObject, count, simulate);
    }

    public void setTransferMode(TransferMode transferMode) {
        if (this.transferMode != transferMode) {
            this.transferMode = transferMode;
            this.coverHolder.markDirty();
            this.getFilterHandler().getFilter().setMaxTransferSize(this.transferMode.maxStackSize);
        }
    }

    @Override
    protected void configureFilter() {
        if (filterHandler.getFilter() instanceof SimpleFluidFilter filter) {
            filter.setMaxStackSize(transferMode == TransferMode.TRANSFER_ANY ? 1 : MAX_STACK_SIZE);
        }

        configureTransferSizeInput();
    }

    ///////////////////////////
    // ***** GUI ******//
    ///////////////////////////

    @Override
    protected @NotNull String getUITitle() {
        return "cover.fluid_regulator.title";
    }

    @Override
    protected void buildAdditionalUI(WidgetGroup group) {
        group.addWidget(
                new EnumSelectorWidget<>(146, 45, 20, 20, TransferMode.values(), transferMode, this::setTransferMode));

        this.transferSizeInput = new IntInputWidget(35, 45, 84, 20,
                this::getCurrentBucketModeTransferSize, this::setCurrentBucketModeTransferSize).setMin(0)
                .setMax(Integer.MAX_VALUE);
        configureTransferSizeInput();
        group.addWidget(this.transferSizeInput);

        this.bucketModeInput = new EnumSelectorWidget<>(121, 45, 20, 20, BucketMode.values(),
                bucketMode, this::setBucketMode);
        group.addWidget(this.bucketModeInput);
    }

    private int getCurrentBucketModeTransferSize() {
        return this.getFilterHandler().getFilter().getMaxTransferSize() / this.bucketMode.multiplier;
    }

    private void setCurrentBucketModeTransferSize(long transferSize) {
        this.getFilterHandler().getFilter()
                .setMaxTransferSize((int) Math.min(Math.max(transferSize * this.bucketMode.multiplier, 0),
                        MAX_STACK_SIZE));
    }

    private void configureTransferSizeInput() {
        if (this.transferSizeInput == null || bucketModeInput == null)
            return;

        this.transferSizeInput.setVisible(shouldShowTransferSize());
        this.bucketModeInput.setVisible(shouldShowTransferSize());
    }

    private boolean shouldShowTransferSize() {
        if (this.transferMode == TransferMode.TRANSFER_ANY)
            return false;

        if (!this.filterHandler.isFilterPresent())
            return true;

        return !this.filterHandler.getFilter().supportsAmounts();
    }

    protected int computeContained(@NotNull IFluidHandler handler, @NotNull FluidTestObject testObject) {
        int found = 0;
        for (int i = 0; i < handler.getTanks(); ++i) {
            FluidStack contained = handler.getFluidInTank(i);
            if (testObject.test(contained)) {
                found += contained.getAmount();
            }
        }
        return found;
    }
}
