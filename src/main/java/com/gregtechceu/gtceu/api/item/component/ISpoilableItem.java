package com.gregtechceu.gtceu.api.item.component;

import com.gregtechceu.gtceu.api.item.ISpoilableItemStack;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public interface ISpoilableItem extends IItemComponent {

    /**
     * Initializes this ItemStack's spoilage timer if it wasn't initialized before.
     * Should be called when it finishes crafting, for example.
     * 
     * @param level may be {@code null}, maybe even should be lol
     */
    static void update(ItemStack stack, @Nullable Level level) {
        ((ISpoilableItemStack) (Object) stack).gtceu$updateFreshness(level, true);
    }

    /**
     * Should return the amount of ticks that this item can stay fresh.
     * The result of this method shouldn't be based on the freshness of the provided stack
     */
    long getSpoilTicks(ItemStack stack);

    default long getTicksUntilSpoiled(ItemStack stack) {
        return ((ISpoilableItemStack) (Object) stack).gtceu$getRemainingTicks(null);
    }

    default void setTicksUntilSpoiled(ItemStack stack, long value) {
        ((ISpoilableItemStack) (Object) stack).gtceu$setRemainingTicks(null, value);
    }

    default void freezeSpoiling(ItemStack stack) {
        ((ISpoilableItemStack) (Object) stack).gtceu$setFreezeSpoiling(true);
    }

    default void unfreezeSpoiling(ItemStack stack) {
        ((ISpoilableItemStack) (Object) stack).gtceu$setFreezeSpoiling(false);
    }

    /**
     * Should return the stack to replace the provided stack with when it spoils
     */
    ItemStack spoilResult(ItemStack stack);

    /**
     * Note: returning {@code false} in this method won't stop the item from spoiling if the spoiling NBT has already
     * been initialized
     */
    boolean shouldSpoil(ItemStack stack);
}
