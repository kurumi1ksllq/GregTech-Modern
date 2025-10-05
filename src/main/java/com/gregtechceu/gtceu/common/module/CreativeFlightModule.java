package com.gregtechceu.gtceu.common.module;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.module.AppliedItemModule;
import com.gregtechceu.gtceu.api.item.module.ITieredItemModule;
import com.gregtechceu.gtceu.api.item.module.ItemModule;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class CreativeFlightModule extends ItemModule implements ITieredItemModule {

    public CreativeFlightModule(ResourceLocation id) {
        super(id);
    }

    private void setMayFly(LivingEntity entity, boolean mayFly) {
        if (entity instanceof Player player) {
            player.getAbilities().mayfly = mayFly;
            if (!mayFly) player.getAbilities().flying = false;
        }
    }

    private boolean isFlying(LivingEntity entity) {
        if (entity instanceof Player player) {
            return player.getAbilities().flying;
        } else return false;
    }

    @Override
    public void onEquip(LivingEntity entity, AppliedItemModule module) {
        super.onEquip(entity, module);
        setMayFly(entity, true);
    }

    @Override
    public void onUnequip(LivingEntity entity, AppliedItemModule module) {
        super.onUnequip(entity, module);
        setMayFly(entity, false);
    }

    @Override
    public void onArmorTick(LivingEntity entity, AppliedItemModule module) {
        super.onArmorTick(entity, module);
        IElectricItem electricItem = GTCapabilityHelper.getElectricItem(module.getAppliedTo());
        if (electricItem == null || !isFlying(entity)) return;
        if (!electricItem.canUse(2048)) setMayFly(entity, false);
        else {
            electricItem.discharge(2048, electricItem.getTier(), true, false, false);
            setMayFly(entity, true);
        }
    }

    @Override
    public int getTier() {
        return GTValues.IV;
    }
}
