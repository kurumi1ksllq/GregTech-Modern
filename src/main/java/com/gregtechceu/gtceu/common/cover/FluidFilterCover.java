package com.gregtechceu.gtceu.common.cover;

import com.gregtechceu.gtceu.api.capability.ICoverable;
import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.api.cover.CoverDefinition;
import com.gregtechceu.gtceu.api.cover.IUICover;
import com.gregtechceu.gtceu.api.cover.filter.FluidFilter;
import com.gregtechceu.gtceu.api.transfer.fluid.FluidHandlerDelegate;
import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.api.ui.component.EnumSelectorComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.ParentUIComponent;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;
import com.gregtechceu.gtceu.common.cover.data.FilterMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Direction;
import net.minecraftforge.fluids.FluidStack;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/3/13
 * @implNote ItemFilterCover
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FluidFilterCover extends CoverBehavior implements IUICover {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(FluidFilterCover.class,
            CoverBehavior.MANAGED_FIELD_HOLDER);
    protected FluidFilter fluidFilter;
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
    }

    public void setFilterMode(FilterMode filterMode) {
        this.filterMode = filterMode;
        coverHolder.markDirty();
    }

    @Override
    public boolean canAttach() {
        return coverHolder.getFluidHandlerCap(attachedSide, false) != null;
    }

    public FluidFilter getFluidFilter() {
        if (fluidFilter == null) {
            fluidFilter = FluidFilter.loadFilter(attachItem);
        }
        return fluidFilter;
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
    public ParentUIComponent createUIWidget(UIAdapter<StackLayout> adapter) {
        final var group = UIContainers.stack(Sizing.fixed(178), Sizing.fixed(85));
        group.child(UIComponents.label(attachItem.getHoverName())
                .positioning(Positioning.absolute(60, 5)));
        group.child(new EnumSelectorComponent<>(Sizing.fixed(18), Sizing.fixed(18),
                FilterMode.VALUES, filterMode, this::setFilterMode)
                .positioning(Positioning.absolute(35, 25)));
        group.child(new EnumSelectorComponent<>(Sizing.fixed(18), Sizing.fixed(18),
                ManualIOMode.VALUES, allowFlow, this::setAllowFlow)
                .positioning(Positioning.absolute(35, 45)));
        group.child(getFluidFilter().openConfigurator(62, 25, adapter));
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
            if (filterMode != FilterMode.FILTER_EXTRACT && getFluidFilter().test(resource))
                return super.fill(resource, action);
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if ((filterMode == FilterMode.FILTER_INSERT) && allowFlow == ManualIOMode.UNFILTERED)
                return super.drain(resource, action);
            if (filterMode != FilterMode.FILTER_INSERT && getFluidFilter().test(resource))
                return super.drain(resource, action);
            return FluidStack.EMPTY;
        }
    }
}
