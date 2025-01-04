package com.gregtechceu.gtceu.api.transfer.item;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;
import java.util.function.Predicate;

public class ItemHandlerList implements IItemHandlerModifiable, INBTSerializable<CompoundTag> {

    private final Int2ObjectMap<IItemHandler> handlerBySlotIndex = new Int2ObjectLinkedOpenHashMap<>();
    private final Reference2IntOpenHashMap<IItemHandler> baseIndexOffset = new Reference2IntOpenHashMap<>();

    @Setter
    protected Predicate<ItemStack> filter = fluid -> true;

    public ItemHandlerList(Collection<? extends IItemHandler> handlers) {
        int currentSlotIndex = 0;
        for (IItemHandler itemHandler : handlers) {
            if (baseIndexOffset.containsKey(itemHandler)) {
                throw new IllegalArgumentException("Attempted to add item handler " + itemHandler + " twice");
            }
            baseIndexOffset.put(itemHandler, currentSlotIndex);
            int slotsCount = itemHandler.getSlots();
            for (int slotIndex = 0; slotIndex < slotsCount; slotIndex++) {
                handlerBySlotIndex.put(currentSlotIndex + slotIndex, itemHandler);
            }
            currentSlotIndex += slotsCount;
        }
    }

    @Override
    public int getSlots() {
        return handlerBySlotIndex.size();
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        IItemHandler itemHandler = getHandlerBySlot(slot);
        if (!(itemHandler instanceof IItemHandlerModifiable))
            throw new UnsupportedOperationException("Handler " + itemHandler + " does not support this method");
        ((IItemHandlerModifiable) itemHandler).setStackInSlot(slot - getOffsetByHandler(itemHandler), stack);
    }

    @NotNull
    @Override
    public ItemStack getStackInSlot(int slot) {
        IItemHandler itemHandler = getHandlerBySlot(slot);
        return itemHandler.getStackInSlot(slot - getOffsetByHandler(itemHandler));
    }

    @Override
    public int getSlotLimit(int slot) {
        IItemHandler itemHandler = getHandlerBySlot(slot);
        return itemHandler.getSlotLimit(slot - getOffsetByHandler(itemHandler));
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        IItemHandler itemHandler = getHandlerBySlot(slot);
        return itemHandler.isItemValid(slot - getOffsetByHandler(itemHandler), stack);
    }

    @NotNull
    @Override
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        IItemHandler itemHandler = getHandlerBySlot(slot);
        return itemHandler.insertItem(slot - getOffsetByHandler(itemHandler), stack, simulate);
    }

    @NotNull
    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        IItemHandler itemHandler = getHandlerBySlot(slot);
        return itemHandler.extractItem(slot - getOffsetByHandler(itemHandler), amount, simulate);
    }

    @Override
    public CompoundTag serializeNBT() {
        var tag = new CompoundTag();
        var list = new ListTag();
        for (IItemHandler handler : handlerBySlotIndex.values()) {
            if (handler instanceof INBTSerializable<?> serializable) {
                list.add(serializable.serializeNBT());
            } else {
                GTCEu.LOGGER.warn("[ItemHandlerList] internal tank doesn't support serialization");
            }
        }
        tag.put("slots", list);
        tag.putByte("type", list.getElementType());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        var list = nbt.getList("slots", nbt.getByte("type"));
        for (int i = 0; i < list.size(); i++) {
            if (getHandlerBySlot(i) instanceof INBTSerializable serializable) {
                serializable.deserializeNBT(list.get(i));
            } else {
                GTCEu.LOGGER.warn("[ItemHandlerList] internal tank doesn't support serialization");
            }
        }
    }

    @NotNull
    @UnmodifiableView
    public Collection<IItemHandler> getBackingHandlers() {
        return handlerBySlotIndex.values();
    }

    public IItemHandler getHandlerBySlot(int slot) {
        return handlerBySlotIndex.get(slot);
    }

    public int getOffsetByHandler(IItemHandler handler) {
        return baseIndexOffset.getInt(handler);
    }
}
