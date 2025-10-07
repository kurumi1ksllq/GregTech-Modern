package com.gregtechceu.gtceu.api.item.module;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;

import net.minecraft.resources.ResourceLocation;

public class UniversalItemModuleSlot extends ItemModuleSlot {

    public UniversalItemModuleSlot(ResourceLocation id) {
        super(id);
    }

    @Override
    public boolean acceptsModule(ItemModule module) {
        return true;
    }

    @Override
    public IGuiTexture getSlotTexture() {
        return new ColorBorderTexture(1, 0xFFFFFFFF);
    }
}
