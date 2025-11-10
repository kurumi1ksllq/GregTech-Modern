package com.gregtechceu.gtceu.api.mui.value.sync;

import com.gregtechceu.gtceu.api.mui.base.drawable.IKey;
import com.gregtechceu.gtceu.api.mui.utils.MouseData;
import com.gregtechceu.gtceu.client.mui.screen.RichTooltip;
import com.gregtechceu.gtceu.utils.NetworkUtils;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class FluidTankSyncHandler extends SyncHandler{

    public static  final int TRY_CLICK_CONTAINER =1;
    public static final int UPDATE_TANK = 2;
    public static final int UPDATE_AMOUNT = 3;
    public static final int LOCK_FLUID = 5;

    private final IFluidTank tank;
    private BooleanConsumer lockHandler;
    private BooleanSupplier isLocked;
    @Getter
    @Setter
    private Supplier<FluidStack> lockedFluid;
    private FluidStack lastFluid;
    private FluidStack phantomFluid;
    @Getter
    @Setter
    private boolean canDrainSlot = true;
    @Getter
    @Setter
    private boolean canFillSlot = true;
    @Getter
    @Setter
    private boolean phantom;
    @Getter
    @Setter
    private BooleanSupplier showAmountinToolip = () -> true;
    @Getter
    @Setter
    private BooleanSupplier showAmoutnOnSlot = () -> true;
    @Getter
    @Setter
    private BooleanSupplier drawAlwaysFull = () -> true;
    @Nullable
    private Consumer< FluidStack> changeConsumer;

    public FluidTankSyncHandler(IFluidTank tank) {

        this.tank = tank;

    }

    public FluidStack getFluid() {
        return this.tank.getFluid();
    }

    public boolean canLockFluid() {
        return lockHandler != null && lockedFluid != null && isLocked != null;
    }

    @Override
    public void detectAndSendChanges(boolean init) {

        FluidStack current = getFluid();
        if(init || current.isFluidEqual(lastFluid)) {
            lastFluid = current == null ? null : current.copy();
            syncToClient(UPDATE_TANK, buffer -> NetworkUtils.writeFluidStack(buffer, current));
        } else if (lastFluid != null && current.getAmount() != lastFluid.getAmount()) {
            lastFluid.setAmount(current.getAmount());
            syncToClient(UPDATE_AMOUNT, buffer -> buffer.writeInt(current.getAmount()));
        } else if (!isPhantom() && canLockFluid() &&
            ! this.phantomFluid.isFluidEqual(this.lockedFluid.get())){
            this.phantomFluid = this.lockedFluid.get();
            sync(LOCK_FLUID, buffer -> {
                buffer.writeBoolean(this.isLocked.getAsBoolean());
                NetworkUtils.writeFluidStack(buffer,this.phantomFluid);
            });
            
        }
    }

    public FluidTankSyncHandler handleLocking(Supplier<FluidStack> lockedFluid, BooleanSupplier isLocked, BooleanConsumer lockHandler) {
        this.lockedFluid =lockedFluid;
        this.lockHandler = lockHandler;
        this.isLocked = isLocked;
        return this;
    }

    public void setFluid(FluidStack fluid) {

        if(tank instanceof FluidTank fluidTank) {
            fluidTank.setFluid(fluid);
        }else{
            tank.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.EXECUTE);
            if(fluid.isEmpty()) tank.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        }
        if(!isPhantom() || fluid.isEmpty()) {return;}
        if(this.phantomFluid.isEmpty() || this.phantomFluid.getFluid() != fluid.getFluid()) {
            this.phantomFluid = fluid;
        }
    }

    public void setAmount(int amount) {

        FluidStack stack = getFluid();
        if(stack == null) return;
        stack.setAmount(amount);
    }
    public int getCapacity() {
        return this.tank.getCapacity();
    }

    public FluidTankSyncHandler accessibility(boolean canDrain, boolean canFill) {
        this.canDrainSlot = canDrain;
        this.canFillSlot = canFill;
        return this;
    }

    public FluidTankSyncHandler showAmount(boolean inSlot, boolean inTooltip){
        return showAmount(() -> inSlot, () -> inTooltip);
    }

    public FluidTankSyncHandler showAmount(BooleanSupplier inSlot, BooleanSupplier inTooltip) {
        return showAmountOnSlot(inSlot).showAmountInTooltip(inTooltip);
    }

    public FluidTankSyncHandler showAmountInTooltip(boolean showAmount) {
        return showAmountInTooltip(() -> showAmount);
    }

    public FluidTankSyncHandler showAmountInTooltip(BooleanSupplier showAmount) {
        this.showAmountinToolip = showAmount;
        return this;
    }

    public boolean showAmountInTooltip() {
        if (!isPhantom() && phantomFluid != null)
            return false;
        return this.showAmountinToolip.getAsBoolean();
    }

    public FluidTankSyncHandler showAmountOnSlot(boolean showAmount) {
        return showAmountOnSlot(() -> showAmount);
    }

    public FluidTankSyncHandler showAmountOnSlot(BooleanSupplier showAmount) {
        this.showAmoutnOnSlot = showAmount;
        return this;
    }

    public boolean showAmountOnSlot() {
        if (!isPhantom() && phantomFluid != null)
            return false;
        return this.showAmoutnOnSlot.getAsBoolean();
    }

    public FluidTankSyncHandler drawAlwaysFull(boolean drawAsFull) {
        this.drawAlwaysFull = () -> drawAsFull;
        return this;
    }

    public FluidTankSyncHandler drawAlwaysFull(BooleanSupplier drawAsFull) {
        this.drawAlwaysFull = drawAsFull;
        return this;
    }

    public boolean drawAlwaysFull() {
        return this.drawAlwaysFull.getAsBoolean();
    }

    public void setChangeConsumer(@Nullable Consumer<FluidStack> changeConsumer) {
        this.changeConsumer = changeConsumer;
    }

    protected void onChange(@Nullable FluidStack fluidStack) {
        if (changeConsumer != null) {
            changeConsumer.accept(fluidStack);
        }
    }

    public @NotNull String getFormattedFluidAmount() {
        var tankFluid = this.tank.getFluid();
        return String.format("%,d", tankFluid == null ? 0 : tankFluid.getAmount());
    }

    public int getFluidAmount() {
        FluidStack tankFluid = tank.getFluid();
        return tankFluid == null ? 0 : tankFluid.getAmount();
    }

    public @Nullable String getFluidLocalizedName() {
        var tankFluid = this.tank.getFluid();
        if (tankFluid == null)
            tankFluid = lockedFluid.get();

        return tankFluid == null ? null : tankFluid.getDisplayName().getString();
    }

    public @NotNull IKey getFluidNameKey() {
        FluidStack tankFluid = tank.getFluid();
        if (tankFluid == null) {
            tankFluid = getLockedFluid().get();
        }
        return tankFluid == null ? IKey.EMPTY : IKey.lang(tankFluid.getDisplayName().getString());
    }

    public void lockFluid(boolean isLocked) {
        if(!canLockFluid()) return;
        this.lockHandler.accept(isLocked);
    }

    @Override
    public void readOnClient(int id, FriendlyByteBuf buf) {
        switch (id) {
            case TRY_CLICK_CONTAINER -> replaceCursorItemStack(NetworkUtils.readItemStack(buf));
            case UPDATE_TANK -> {
                FluidStack stack = NetworkUtils.readFluidStack(buf);
                setFluid(stack);
                onChange(stack);
            }
            case UPDATE_AMOUNT -> {
                setAmount(buf.readInt());
                onChange(getFluid());
            }
            case LOCK_FLUID -> {
                lockHandler.accept(buf.readBoolean());
                lockFluid(NetworkUtils.readFluidStack(buf));
                FluidStack stack = getFluid();
                onChange(stack == null ? getLockedFluid() : stack);
            }
        }

    }
    private void replaceCursorItemStack(ItemStack resultStack) {
        int resultStackSize = resultStack.getMaxStackSize();
        ItemStack playerStack = getSyncManager().getCursorItem();

        if (!getSyncManager().isClient())
            syncToClient(TRY_CLICK_CONTAINER, buffer -> NetworkUtils.writeItemStack(buffer, resultStack));

        while (resultStack.getCount() > resultStackSize) {
            playerStack.shrink(resultStackSize);
            addItemToPlayerInventory(resultStack.split(resultStackSize));
        }
        if (playerStack.getCount() == resultStack.getCount()) {
            // every item on the cursor is mutated, so leave it there
            getSyncManager().setCursorItem(resultStack);
        } else {
            // some items not mutated. Mutated items go into the inventory/world.
            playerStack.shrink(resultStack.getCount());
            getSyncManager().setCursorItem(playerStack);
            addItemToPlayerInventory(resultStack);
        }
    }
    private void addItemToPlayerInventory(ItemStack stack) {
        if (stack == null) return;
        var player = getSyncManager().getPlayer();

        if (!player.getInventory().add(stack) && !player.level().isClientSide) {
            ItemEntity dropItem = player.drop(stack, false);
            if (dropItem != null) dropItem.setPickUpDelay(0);
        }
    }
    public void tryClickPhantom(MouseData data) {
        Player player = getSyncManager().getPlayer();
        ItemStack currentStack = player.getItemInHand(data.);
        FluidStack currentFluid = this.tank.getFluid();
        IFluidHandlerItem fluidHandlerItem = FluidUtil.getFluidHandler(currentStack);

        switch (data.mouseButton()) {
            case 0 -> {
                if (currentStack.isEmpty() || fluidHandlerItem == null) {
                    if (this.canDrainSlot()) {
                        this.tank.drain(data.shift ? Integer.MAX_VALUE : 1000, true);
                    }
                } else {
                    FluidStack cellFluid = fluidHandlerItem.drain(Integer.MAX_VALUE, false);
                    if ((this.showAmountOnSlot.getAsBoolean() || currentFluid == null) && cellFluid != null) {
                        if (this.canFillSlot()) {
                            if (!this.showAmountOnSlot.getAsBoolean()) {
                                cellFluid.amount = 1;
                            }
                            if (this.tank.fill(cellFluid, true) > 0) {
                                this.phantomFluid = cellFluid.copy();
                            }
                        }
                    } else {
                        if (this.canDrainSlot()) {
                            this.tank.drain(data.shift ? Integer.MAX_VALUE : 1000, true);
                        }
                    }
                }
            }
            case 1 -> {
                if (this.canFillSlot()) {
                    if (currentFluid != null) {
                        if (this.showAmountOnSlot.getAsBoolean()) {
                            FluidStack toFill = currentFluid.copy();
                            toFill.amount = 1000;
                            this.tank.fill(toFill, true);
                        }
                    } else if (this.phantomFluid != null) {
                        FluidStack toFill = this.phantomFluid.copy();
                        toFill.amount = this.showAmountOnSlot.getAsBoolean() ? 1 : toFill.amount;
                        this.tank.fill(toFill, true);
                    }
                }
            }
            case 2 -> {
                if (currentFluid != null && canDrainSlot())
                    this.tank.drain(data.shift ? Integer.MAX_VALUE : 1000, true);
            }
        }
    }

    public void tryScrollPhantom(MouseData mouseData) {
        FluidStack currentFluid = this.tank.getFluid();
        int amount = mouseData.mouseButton;
        if (!this.showAmountOnSlot()) {
            int newAmt = amount == 1 ? 1 : 0;
            if (newAmt == 0) {
                setFluid(null);
            } else if (currentFluid != null && currentFluid.amount != newAmt) {
                setAmount(newAmt);
            }
            return;
        }
        if (mouseData.shift) {
            amount *= 10;
        }
        if (mouseData.ctrl) {
            amount *= 100;
        }
        if (mouseData.alt) {
            amount *= 1000;
        }
        if (currentFluid == null) {
            if (amount > 0 && this.phantomFluid != null) {
                FluidStack toFill = this.phantomFluid.copy();
                toFill.amount = this.showAmountOnSlot() ? amount : 1;
                this.tank.fill(toFill, true);
            }
            return;
        }
        if (amount > 0) {
            FluidStack toFill = currentFluid.copy();
            toFill.amount = amount;
            this.tank.fill(toFill, true);
        } else if (amount < 0) {
            this.tank.drain(-amount, true);
        }
    }

    public ItemStack tryClickContainer(boolean tryFillAll) {
        ItemStack playerHeldStack = getSyncManager().getCursorItem();
        if (playerHeldStack.isEmpty())
            return ItemStack.EMPTY;

        ItemStack useStack = GTUtility.copy(1, playerHeldStack);
        var fluidHandlerItem = FluidUtil.getFluidHandler(useStack);
        if (fluidHandlerItem == null) return ItemStack.EMPTY;

        FluidStack tankFluid = tank.getFluid();
        FluidStack heldFluid = fluidHandlerItem.drain(Integer.MAX_VALUE, false);

        // nothing to do, return
        if (tankFluid == null && heldFluid == null)
            return ItemStack.EMPTY;

        ItemStack returnable = ItemStack.EMPTY;

        // tank is empty, try to fill tank
        if (canFillSlot && tankFluid == null) {
            returnable = fillTankFromStack(fluidHandlerItem, heldFluid, tryFillAll);

            // hand is empty, try to drain tank
        } else if (canDrainSlot && heldFluid == null) {
            returnable = drainTankIntoStack(fluidHandlerItem, tankFluid, tryFillAll);

            // neither is empty but tank is not full, try to fill tank
        } else if (canFillSlot && tank.getFluidAmount() < tank.getCapacity() && heldFluid != null) {
            returnable = fillTankFromStack(fluidHandlerItem, heldFluid, tryFillAll);
        }

        syncToClient(UPDATE_TANK, buffer -> NetworkUtils.writeFluidStack(buffer, tank.getFluid()));

        return returnable;
    }

    private ItemStack fillTankFromStack(IFluidHandlerItem fluidHandler, @NotNull FluidStack heldFluid,
                                        boolean tryFillAll) {
        ItemStack heldItem = getSyncManager().getCursorItem();
        if (heldItem.isEmpty()) return ItemStack.EMPTY;

        FluidStack currentFluid = tank.getFluid();
        // Fluid type does not match
        if (currentFluid != null && !currentFluid.isFluidEqual(heldFluid)) return ItemStack.EMPTY;

        int freeSpace = tank.getCapacity() - tank.getFluidAmount();
        if (freeSpace <= 0) return ItemStack.EMPTY;

        ItemStack itemStackEmptied = ItemStack.EMPTY;
        int fluidAmountTaken = 0;

        FluidStack drained = fluidHandler.drain(freeSpace, true);
        if (drained != null && drained.amount > 0) {
            itemStackEmptied = fluidHandler.getContainer();
            fluidAmountTaken = drained.amount;
        }
        if (itemStackEmptied == ItemStack.EMPTY) {
            return ItemStack.EMPTY;
        }

        // find out how many fills we can do
        // same round down behavior as drain
        int additional = tryFillAll ? Math.min(freeSpace / fluidAmountTaken, heldItem.getCount()) : 1;
        FluidStack copiedFluidStack = heldFluid.copy();
        copiedFluidStack.amount = fluidAmountTaken * additional;
        tank.fill(copiedFluidStack, true);

        itemStackEmptied.setCount(additional);
        replaceCursorItemStack(itemStackEmptied);
        playSound(heldFluid, true);
        return itemStackEmptied;
    }

    private ItemStack drainTankIntoStack(IFluidHandlerItem fluidHandler, FluidStack tankFluid, boolean tryFillAll) {
        ItemStack heldItem = getSyncManager().getCursorItem();
        if (heldItem.isEmpty()) return ItemStack.EMPTY;

        ItemStack fluidContainer = ItemStack.EMPTY;
        int filled = fluidHandler.fill(tankFluid, false);
        int stored = tankFluid.amount;
        if (filled > 0) {
            fluidHandler.fill(tankFluid, true);
            tank.drain(filled, true);
            fluidContainer = fluidHandler.getContainer();
            if (tryFillAll) {
                // Determine how many more items we can fill. One item is already filled.
                // Integer division means it will round down, so it will only fill equivalent fluid amounts.
                // For example:
                // Click with 3 cells, with 2500L of fluid in the tank.
                // 2 cells will be filled, and 500L will be left behind in the tank.
                int additional = Math.min(heldItem.getCount(), stored / filled) - 1;
                tank.drain(filled * additional, true);
                fluidContainer.grow(additional);
            }
            replaceCursorItemStack(fluidContainer);
            playSound(tankFluid, false);
        }
        return fluidContainer;
    }


    @Override
    public void readOnServer(int id, FriendlyByteBuf buf) {

        if (id == TRY_CLICK_CONTAINER) {
            var data = MouseData.readPacket(buf);
            if (isPhantom()) {
                tryClickPhantom(data);
            } else {
                var stack = tryClickContainer(data.mouseButton == 0);
                if (!stack.isEmpty())
                    syncToClient(TRY_CLICK_CONTAINER, buffer -> NetworkUtils.writeItemStack(buffer, stack));
            }
        } else if (id == UPDATE_TANK) {
            var fluid = NetworkUtils.readFluidStack(buf);
            setFluid(fluid);
        } else if (id == PHANTOM_SCROLL) {
            tryScrollPhantom(MouseData.readPacket(buf));
        } else if (id == LOCK_FLUID) {
            boolean locked = buf.readBoolean();
            var fluidStack = NetworkUtils.readFluidStack(buf);
            if (fluidStack == null) {
                this.lockHandler.accept(locked);
            } else {
                this.jeiHandler.accept(fluidStack);
            }
        }
    }

}
