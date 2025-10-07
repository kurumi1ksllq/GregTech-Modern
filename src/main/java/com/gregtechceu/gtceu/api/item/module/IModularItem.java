package com.gregtechceu.gtceu.api.item.module;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface IModularItem {

    /**
     * @return the default module slot configuration of this item
     */
    List<ItemModuleSlot> getDefaultSlots(ItemStack stack);
}
