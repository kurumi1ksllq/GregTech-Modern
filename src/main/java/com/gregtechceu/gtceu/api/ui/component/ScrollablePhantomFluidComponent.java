package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.transfer.fluid.IFluidHandlerModifiable;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ScrollablePhantomFluidComponent extends PhantomFluidComponent {

    private static final int SCROLL_ACTION_ID = 0x0001_0001;

    public ScrollablePhantomFluidComponent(@Nullable IFluidHandlerModifiable fluidTank, int tank,
                                           Supplier<FluidStack> phantomFluidGetter,
                                           Consumer<FluidStack> phantomFluidSetter) {
        super(fluidTank, tank, phantomFluidGetter, phantomFluidSetter);
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        if (!isMouseOverElement(mouseX, mouseY))
            return false;

        var delta = getModifiedChangeAmount((amount > 0) ? 1 : -1);
        // sendMessage(SCROLL_ACTION_ID, buf -> buf.writeInt(delta));

        return true;
    }

    private int getModifiedChangeAmount(int amount) {
        if (GTUtil.isShiftDown())
            amount *= 10;

        if (GTUtil.isCtrlDown())
            amount *= 100;

        if (!GTUtil.isAltDown())
            amount *= 1000;

        return amount;
    }

    @Override
    public void receiveMessage(int id, FriendlyByteBuf buffer) {
        if (id == SCROLL_ACTION_ID) {
            handleScrollAction(buffer.readInt());
        } else {
            super.receiveMessage(id, buffer);
        }

        // detectAndSendChanges();
    }

    private void handleScrollAction(int delta) {
        IFluidHandlerModifiable fluidTank = (IFluidHandlerModifiable) handler();
        if (fluidTank == null)
            return;

        FluidStack fluid = fluidTank.getFluidInTank(tank);
        if (fluid.isEmpty())
            return;

        if (fluid.isEmpty())
            return;

        fluid.setAmount(Math.min(Math.max(fluid.getAmount() + delta, 0), fluidTank.getTankCapacity(tank)));
        if (fluid.getAmount() <= 0L) {
            fluidTank.setFluidInTank(tank, FluidStack.EMPTY);
        }
    }
}
