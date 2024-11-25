package com.gregtechceu.gtceu.api.misc;

import com.gregtechceu.gtceu.api.capability.recipe.IO;

import com.lowdragmc.lowdraglib.misc.FluidTransferList;
import com.lowdragmc.lowdraglib.side.fluid.IFluidHandlerModifiable;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author KilaBash
 * @date 2023/3/14
 * @implNote IOFluidTransferList
 */
public class IOFluidTransferList extends FluidTransferList implements IFluidHandlerModifiable {

    @Getter
    private final IO io;
    private final Predicate<FluidStack> inFilter;
    private final Predicate<FluidStack> outFilter;

    public IOFluidTransferList(List<IFluidHandler> transfers, IO io,
                               Predicate<FluidStack> inFilter, Predicate<FluidStack> outFilter) {
        super(transfers);
        this.io = io;
        this.inFilter = inFilter;
        this.outFilter = outFilter;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!io.support(IO.IN) || !inFilter.test(resource)) return 0;
        return super.fill(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (!io.support(IO.OUT) || !outFilter.test(resource)) return FluidStack.EMPTY;
        return super.drain(resource, action);
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        if (!io.support(IO.OUT)) return FluidStack.EMPTY;
        var fluidStack = super.drain(maxDrain, FluidAction.SIMULATE);
        if (fluidStack.isEmpty() || !outFilter.test(fluidStack)) return FluidStack.EMPTY;
        return action.simulate() ? fluidStack : super.drain(maxDrain, action);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        Predicate<FluidStack> filter = s -> true;
        if (io.support(IO.IN)) filter = inFilter.and(filter);
        if (io.support(IO.OUT)) filter = outFilter.and(filter);
        return filter.test(stack) && super.isFluidValid(tank, stack);
    }

    @Override
    public void setFluidInTank(int tank, @NotNull FluidStack fluidStack) {
        int index = 0;
        for (IFluidHandler transfer : transfers) {
            if (transfer instanceof IFluidHandlerModifiable modifiable) {
                if (tank - index < transfer.getTanks()) {
                    modifiable.setFluidInTank(tank - index, fluidStack);
                }
                return;
            }
            index += transfer.getTanks();
        }
    }
}
