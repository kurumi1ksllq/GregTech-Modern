package com.gregtechceu.gtceu.common.module;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.module.AppliedItemModule;
import com.gregtechceu.gtceu.api.item.module.TieredItemModule;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class EnergyShieldItemModule extends TieredItemModule {

    public EnergyShieldItemModule(ResourceLocation id, int tier) {
        super(id, tier);
    }

    @Override
    public float changeDamage(LivingEntity entity, AppliedItemModule module, float amount, DamageSource source) {
        float div = (getTier() - 1) / 4f + 1;
        long energyPerHP = (long) (8192 / div);
        double percentage = 25;
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_FALL) ||
                source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.STARVE)) {
            return amount;
        }

        int damageReduction = Integer.MAX_VALUE;
        IElectricItem electricItem = GTCapabilityHelper.getElectricItem(module.getAppliedTo());
        if (electricItem == null) {
            return amount;
        }
        damageReduction = (int) Math.min(damageReduction,
                percentage * electricItem.getCharge() / (energyPerHP * 100.0D));
        damageReduction = Math.toIntExact(electricItem.discharge(
                damageReduction * energyPerHP,
                electricItem.getTier(),
                true, false, false) / energyPerHP);
        return Math.max(amount - damageReduction, 0);
    }

    @Override
    public void appendHoverText(Level level, TooltipFlag isAdvanced, List<Component> tooltips,
                                AppliedItemModule module) {
        super.appendHoverText(level, isAdvanced, tooltips, module);
        tooltips.add(Component.translatable("metaarmor.tooltip.modifier.damage_block",
                GTValues.VNF[getTier()]));
    }
}
