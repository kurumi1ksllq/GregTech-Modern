package com.gregtechceu.gtceu.api.item.module;

import net.minecraft.resources.ResourceLocation;

public class UniversalItemModuleSlot extends ItemModuleSlot {

    public UniversalItemModuleSlot(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean acceptsModule(ItemModule module) {
        return true;
    }
}
