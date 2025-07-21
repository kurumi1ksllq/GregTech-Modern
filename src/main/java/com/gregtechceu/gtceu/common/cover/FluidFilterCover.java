package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.cover.filter.CoverWithFluidFilter;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandler;
import com.gregtechceu.gtceu.api.cover.filter.FilterHandlers;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.gui.widget.EnumSelectorWidget;
import com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerDelegate;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.client.renderer.pipe.cover.CoverRenderer;
import com.gregtechceu.gtceu.client.renderer.pipe.cover.CoverRendererBuilder;
import com.gregtechceu.gtceu.common.cover.data.FilterMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;

import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidFilterCover extends CoverBehavior implements IUICover, CoverWithFluidFilter {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidFilterCover.class,
            CoverBehavior.MANAGED_FIELD_HOLDER);
    protected FilterHandler<FluidStack, FluidFilter> filterHandler;
    @Persisted
    @DescSynced
    @Getter
    protected FilterMode filterMode = FilterMode.FILTER_INSERT;
    private FilteredFluidHandlerWrapper fluidFilterWrapper;
    @Setter
    @Getter
    protected ManualIOMode allowFlow = ManualIOMode.DISABLED;

    public FluidFilterCover(CoverDefinition definition, ICoverable coverHolder, Direction attachedSide) {
        super(definition, coverHolder, attachedSide);
        filterHandler = FilterHandlers.fluid(this);
    }

    public void setFilterMode(FilterMode filterMode) {
        this.filterMode = filterMode;
        coverHolder.markDirty();
    }

    @Override
    protected CoverRenderer buildRenderer() {
        return new CoverRendererBuilder(GTCEu.id("block/cover/overlay_fluid_filter"), null).build();
    }

    @Override
    public boolean canAttach(@NotNull ICoverable coverable, @NotNull Direction side) {
        return coverable.getCapability(ForgeCapabilities.FLUID_HANDLER).isPresent();
    }

    public @NotNull FilterHandler<FluidStack, FluidFilter> getFilterHandler() {
        if (!filterHandler.isFilterPresent()) {
            filterHandler.loadFilter(attachItem);
        }
        return filterHandler;
    }

    @Override
    public ManualIOMode getManualIOMode() {
        return ManualIOMode.FILTERED;
    }

    @Override
    public @Nullable IFluidHandlerModifiable getFluidHandlerCap(@Nullable IFluidHandlerModifiable defaultValue) {
        if (defaultValue == null) {
            return null;
        }

        if (fluidFilterWrapper == null || fluidFilterWrapper.delegate != defaultValue) {
            this.fluidFilterWrapper = new FilteredFluidHandlerWrapper(defaultValue);
        }

        return fluidFilterWrapper;
    }

    @Override
    public Widget createUIWidget() {
        final var group = new WidgetGroup(0, 0, 178, 85);
        group.addWidget(new LabelWidget(60, 5, attachItem.getDescriptionId()));
        group.addWidget(new EnumSelectorWidget<>(35, 25, 18, 18,
                FilterMode.VALUES, filterMode, this::setFilterMode));
        group.addWidget(new EnumSelectorWidget<>(35, 45, 18, 18, ManualIOMode.VALUES, allowFlow, this::setAllowFlow));
        group.addWidget(getFilterHandler().getFilter().openConfigurator(62, 25));
        return group;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    private class FilteredFluidHandlerWrapper extends FluidHandlerDelegate {

        public FilteredFluidHandlerWrapper(IFluidHandlerModifiable delegate) {
            super(delegate);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if ((filterMode == FilterMode.FILTER_EXTRACT) && allowFlow == ManualIOMode.UNFILTERED)
                return super.fill(resource, action);
            if (filterMode != FilterMode.FILTER_EXTRACT && getFilterHandler().test(resource))
                return super.fill(resource, action);
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if ((filterMode == FilterMode.FILTER_INSERT) && allowFlow == ManualIOMode.UNFILTERED)
                return super.drain(resource, action);
            if (filterMode != FilterMode.FILTER_INSERT && getFilterHandler().test(resource))
                return super.drain(resource, action);
            return FluidStack.EMPTY;
        }
    }
}
