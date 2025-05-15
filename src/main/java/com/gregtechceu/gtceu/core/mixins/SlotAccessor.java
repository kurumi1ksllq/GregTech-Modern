package com.gregtechceu.gtceu.core.mixins;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Accessor interface for accessing protected methods from {@link net.minecraft.inventory.Slot}.
 */
@Mixin(Slot.class)
public interface SlotAccessor {

    @Invoker
    void invokeOnQuickCraft(ItemStack stack, int amount);

    @Invoker
    void invokeOnSwapCraft(int numItemsCrafted);

    @Invoker
    void invokeCheckTakeAchievements(ItemStack stack);

    @Accessor
    @Mutable
    void setX(int x);

    @Accessor
    @Mutable
    void setY(int y);
}
