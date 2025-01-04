package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.ItemFilter;
import com.gregtechceu.gtceu.api.cover.filter.SimpleItemFilter;
import com.gregtechceu.gtceu.api.graphnet.predicate.test.ItemTestObject;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelectorWidget;
import com.gregtechceu.gtceu.api.gui.widget.IntInputWidget;
import com.gregtechceu.gtceu.client.renderer.pipe.cover.CoverRenderer;
import com.gregtechceu.gtceu.client.renderer.pipe.cover.CoverRendererBuilder;
import com.gregtechceu.gtceu.common.cover.data.TransferMode;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntUnaryOperator;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class RobotArmCover extends ConveyorCover {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(RobotArmCover.class,
            ConveyorCover.MANAGED_FIELD_HOLDER);

    @Persisted
    @DescSynced
    @Getter
    protected TransferMode transferMode;
    protected boolean noTransferDueToMinimum = false;

    @Persisted
    @Getter
    protected int globalTransferLimit;
    protected int itemsTransferBuffered;

    private IntInputWidget stackSizeInput;

    public RobotArmCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide, int tier,
                         int maxTransferRate) {
        super(definition, coverHolder, attachedSide, tier, maxTransferRate);
        setTransferMode(TransferMode.TRANSFER_ANY);
    }

    public RobotArmCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide, int tier) {
        this(definition, coverHolder, attachedSide, tier, CONVEYOR_SCALING.applyAsInt(tier));
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    protected CoverRenderer buildRenderer() {
        return new CoverRendererBuilder(GTCEu.id("block/cover/overlay_arm"),
                GTCEu.id("block/cover/overlay_arm_emissive")).build();
    }

    @Override
    protected CoverRenderer buildRendererInverted() {
        return new CoverRendererBuilder(GTCEu.id("block/cover/overlay_arm"),
                GTCEu.id("block/cover/overlay_arm_inverted_emissive")).build();
    }

    @Override
    protected void refreshBuffer(int transferRate) {
        if (this.transferMode == TransferMode.TRANSFER_EXACT && noTransferDueToMinimum) {
            if (getFilterHandler().isFilterPresent()) {
                this.noTransferDueToMinimum = false;
                this.itemsLeftToTransferLastSecond += transferRate;
                int max = getFilterHandler().getFilter().getMaxTransferSize();
                if (this.itemsLeftToTransferLastSecond > max) {
                    this.itemsLeftToTransferLastSecond = max;
                }
                return;
            }
        }
        super.refreshBuffer(transferRate);
    }

    @Override
    protected void performTransferOnUpdate(@NotNull IItemHandler sourceHandler, @NotNull IItemHandler destHandler) {
        if (transferMode != TransferMode.TRANSFER_EXACT) {
            super.performTransferOnUpdate(sourceHandler, destHandler);
            return;
        }
        FilterHandler<ItemStack, ItemFilter> filter = this.getFilterHandler();
        if (!filter.isFilterPresent()) return;
        IntUnaryOperator reqFlow = s -> {
            int limit = filter.getFilter().getTransferLimit(s);
            if (getItemsLeftToTransfer() < limit) {
                noTransferDueToMinimum = true;
                return 0;
            } else return limit;
        };
        performTransfer(sourceHandler, destHandler, true, reqFlow, reqFlow, (a, b) -> reportItemsTransfer(b));
    }

    @Override
    protected int simpleInsert(@NotNull IItemHandler handler, ItemTestObject testObject, int count,
                               boolean simulate) {
        if (transferMode == TransferMode.KEEP_EXACT) {
            assert getFilterHandler().isFilterPresent();
            int kept = getFilterHandler().getFilter().getTransferLimit(testObject.recombine());
            count = Math.min(count, kept - computeContained(handler, testObject));
        }
        return super.simpleInsert(handler, testObject, count, simulate);
    }

    public void setTransferMode(TransferMode transferMode) {
        if (this.transferMode != transferMode) {
            this.transferMode = transferMode;
            this.coverHolder.markDirty();
            configureStackSizeInput();
            this.getFilterHandler().getFilter().setMaxTransferSize(transferMode.maxStackSize);
        }
    }

    public int getBuffer() {
        return itemsTransferBuffered;
    }

    public void buffer(int amount) {
        itemsTransferBuffered += amount;
    }

    public void clearBuffer() {
        itemsTransferBuffered = 0;
    }

    //////////////////////////////////////
    // *********** GUI ***********//
    //////////////////////////////////////

    @Override
    @NotNull
    protected String getUITitle() {
        return "cover.robotic_arm.title";
    }

    @Override
    protected void buildAdditionalUI(WidgetGroup group) {
        group.addWidget(
                new EnumSelectorWidget<>(146, 45, 20, 20, TransferMode.values(), transferMode, this::setTransferMode));

        this.stackSizeInput = new IntInputWidget(64, 45, 80, 20,
                () -> globalTransferLimit, val -> globalTransferLimit = val);
        configureStackSizeInput();

        group.addWidget(this.stackSizeInput);
    }

    @Override
    protected void configureFilter() {
        if (filterHandler.getFilter() instanceof SimpleItemFilter filter) {
            filter.setMaxStackSize(filter.isBlackList() ? 1 : transferMode.maxStackSize);
        }

        configureStackSizeInput();
    }

    private void configureStackSizeInput() {
        if (this.stackSizeInput == null)
            return;

        this.stackSizeInput.setVisible(shouldShowStackSize());
        this.stackSizeInput.setMin(1);
        this.stackSizeInput.setMax(this.transferMode.maxStackSize);
    }

    private boolean shouldShowStackSize() {
        if (this.transferMode == TransferMode.TRANSFER_ANY)
            return false;

        if (!this.filterHandler.isFilterPresent())
            return true;

        return !this.filterHandler.getFilter().supportsAmounts();
    }

    protected int computeContained(@NotNull IItemHandler handler, @NotNull ItemTestObject testObject) {
        int found = 0;
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack contained = handler.getStackInSlot(i);
            if (testObject.test(contained)) {
                found += contained.getCount();
            }
        }
        return found;
    }
}
