package com.gregtechceu.gtceu.common.item;

import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

public class SpoilableBehaviour implements ISpoilableItem {

    private final long ticks;
    private final ItemLike spoilResult;

    public SpoilableBehaviour(long ticks, ItemLike spoilResult) {
        this.ticks = ticks;
        this.spoilResult = spoilResult;
    }

    @Override
    public long getSpoilTicks(ItemStack stack) {
        return ticks;
    }

    @Override
    public ItemStack spoilResult(ItemStack stack) {
        return new ItemStack(spoilResult, stack.getCount());
    }
}
