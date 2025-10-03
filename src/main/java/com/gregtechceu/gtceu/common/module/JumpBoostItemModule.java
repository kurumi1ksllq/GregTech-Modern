package com.gregtechceu.gtceu.common.module;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.module.AppliedItemModule;
import com.gregtechceu.gtceu.api.item.module.IJumpBoostItemModule;
import com.gregtechceu.gtceu.api.item.module.TieredItemModule;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class JumpBoostItemModule extends TieredItemModule implements IJumpBoostItemModule {

    public JumpBoostItemModule(ResourceLocation id, int tier) {
        super(id, tier);
    }

    @Override
    public float getJumpBoost(AppliedItemModule module) {
        return getTier() / 4f;
    }

    @Override
    public void appendHoverText(Level level, TooltipFlag isAdvanced, List<Component> tooltips,
                                AppliedItemModule module) {
        super.appendHoverText(level, isAdvanced, tooltips, module);
        tooltips.add(Component.translatable("metaarmor.tooltip.modifier.jump", GTValues.VNF[getTier()]));
    }
}
