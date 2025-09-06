package com.gregtechceu.gtceu.api.item.component;

import com.gregtechceu.gtceu.api.item.ISpoilableItemStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public interface ISpoilableItem extends IItemComponent {

    static void update(ItemStack stack, Level level) {
        ((ISpoilableItemStack) (Object) stack).gtceu$updateFreshness(level);
    }

    /**
     * Should return the amount of ticks that this item can stay fresh
     * The result of this method shouldn't be based on the freshness of the provided stack
     */
    long getSpoilTicks(ItemStack stack);

    /**
     * Should return the stack to replace the provided stack with when it spoils
     */
    ItemStack spoilResult(ItemStack stack);
}
