package com.gregtechceu.gtceu.api.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.utils.GTTransferUtils;

import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

@Accessors(chain = true)
public class ItemHandlerProxyTrait extends MachineTrait implements IItemHandlerModifiable, ICapabilityTrait {

    public static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(ItemHandlerProxyTrait.class);
    @Getter
    public final IO capabilityIO;
    @Setter
    @Getter
    @Nullable
    public IItemHandlerModifiable proxy;

    private final Map<Direction, LazyOptional<IItemHandler>> handlers = new Object2ReferenceOpenHashMap<>();

    public ItemHandlerProxyTrait(MetaMachine machine, IO capabilityIO) {
        super(machine);
        this.capabilityIO = capabilityIO;
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    //////////////////////////////////////
    // ******* Capability ********//
    //////////////////////////////////////

    @Override
    public int getSlots() {
        return proxy == null ? 0 : proxy.getSlots();
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        return proxy == null ? ItemStack.EMPTY : proxy.getStackInSlot(slot);
    }

    @Override
    public void setStackInSlot(int index, ItemStack stack) {
        if (proxy != null) {
            proxy.setStackInSlot(index, stack);
        }
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (proxy != null && canCapInput()) {
            return proxy.insertItem(slot, stack, simulate);
        }
        return stack;
    }

    public ItemStack insertItemInternal(int slot, @NotNull ItemStack stack, boolean simulate) {
        return proxy == null ? stack : proxy.insertItem(slot, stack, simulate);
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (proxy != null && canCapOutput()) {
            return proxy.extractItem(slot, amount, simulate);
        }
        return ItemStack.EMPTY;
    }

    public ItemStack extractItemInternal(int slot, int amount, boolean simulate) {
        return proxy == null ? ItemStack.EMPTY : proxy.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return proxy == null ? 0 : proxy.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return proxy != null && proxy.isItemValid(slot, stack);
    }

    public boolean isEmpty() {
        if (proxy instanceof NotifiableItemStackHandler itemStackHandler) return itemStackHandler.isEmpty();
        boolean isEmpty = true;
        if (proxy != null) {
            for (int i = 0; i < proxy.getSlots(); i++) {
                if (!proxy.getStackInSlot(i).isEmpty()) {
                    isEmpty = false;
                    break;
                }
            }
        }
        return isEmpty;
    }

    public void setNeighborHandler(Level level, BlockPos pos, Direction facing) {
        var handler = GTTransferUtils.getAdjacentItemHandler(level, pos, facing);
        handlers.put(facing, handler);
    }

    public boolean hasAdjacentHandler(Direction facing) {
        return handlers.get(facing) != null && !handlers.get(facing).isPresent();
    }

    public void exportToNearby(Direction... facings) {
        if (isEmpty()) return;
        var level = getMachine().getLevel();
        var pos = getMachine().getPos();
        for (Direction facing : facings) {
            var filter = getMachine().getItemCapFilter(facing, IO.OUT);
            var handler = handlers.get(facing);
            if (handler == null || !handler.isPresent()) {
                handler = GTTransferUtils.getAdjacentItemHandler(level, pos, facing);
            }
            handler.ifPresent(adj -> GTTransferUtils.transferItemsFiltered(this, adj, filter));

        }
    }
}
