package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.api.item.component.SpoilContext;

import net.minecraft.world.item.ItemStack;

public interface ISpoilableItemStackExtension {

    void gtceu$setStack(ItemStack newStack);

    void gtceu$setSpoilContext(SpoilContext ctx);

    SpoilContext gtceu$getSpoilContext();
}
