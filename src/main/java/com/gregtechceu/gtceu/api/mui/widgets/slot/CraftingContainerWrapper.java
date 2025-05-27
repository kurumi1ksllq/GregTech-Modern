package com.gregtechceu.gtceu.api.mui.widgets.slot;

import com.gregtechceu.gtceu.core.mixins.TransientCraftingContainerAccessor;

import net.minecraft.core.NonNullList;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.ItemHandlerHelper;

import lombok.Getter;

public class CraftingContainerWrapper extends TransientCraftingContainer {

    @Getter
    private final IItemHandler delegate;
    @Getter
    private final int startIndex;
    private final ItemStack[] snapshot;

    public CraftingContainerWrapper(AbstractContainerMenu menu, int width, int height, IItemHandlerModifiable delegate,
                                    int startIndex) {
        super(menu, width, height);
        this.delegate = delegate;
        this.startIndex = startIndex;
        this.snapshot = new ItemStack[width * height];
        // save inventory snapshot
        for (int i = 0; i < snapshot.length; i++) {
            ItemStack stack = this.delegate.getStackInSlot(i + this.startIndex);
            updateSnapshot(i, stack);
            getBackingList().set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
    }

    private NonNullList<ItemStack> getBackingList() {
        return ((TransientCraftingContainerAccessor) this).gtceu$getActualItems();
    }

    public AbstractContainerMenu getMenu() {
        return ((TransientCraftingContainerAccessor) this).getMenu();
    }

    private void updateSnapshot(int index, ItemStack stack) {
        this.snapshot[index] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
    }

    public void detectChanges() {
        // detect changes from snapshot and notify container
        for (int slot = 0; slot < snapshot.length; slot++) {
            ItemStack stack = snapshot[slot];
            ItemStack current = this.delegate.getStackInSlot(slot + this.startIndex);
            if (stack.isEmpty() != current.isEmpty() ||
                    (!stack.isEmpty() && !ItemHandlerHelper.canItemStacksStack(stack, current))) {
                setItem(slot, current);
                updateSnapshot(slot, current);
            }
        }
    }
}
