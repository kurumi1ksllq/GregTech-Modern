package com.gregtechceu.gtceu.common.module;

import com.gregtechceu.gtceu.api.item.module.AppliedItemModule;
import com.gregtechceu.gtceu.api.item.module.IJumpBoostItemModule;
import com.gregtechceu.gtceu.api.item.module.TieredItemModule;

import net.minecraft.resources.ResourceLocation;

public class JumpBoostItemModule extends TieredItemModule implements IJumpBoostItemModule {

    public JumpBoostItemModule(ResourceLocation id, int tier) {
        super(id, tier);
    }

    @Override
    public float getJumpBoost(AppliedItemModule module) {
        return getTier() / 4f;
    }
}
