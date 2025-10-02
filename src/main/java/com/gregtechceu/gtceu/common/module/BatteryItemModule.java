package com.gregtechceu.gtceu.common.module;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.module.AppliedItemModule;
import com.gregtechceu.gtceu.api.item.module.ItemModule;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class BatteryItemModule extends ItemModule {

    private static final double PERCENTAGE = 80.0d;

    public BatteryItemModule(ResourceLocation id) {
        super(id);
    }

    @Override
    public void onInventoryTick(Player player, AppliedItemModule module) {
        super.onInventoryTick(player, module);
        if (module.getAppliedTo() == null || module.getModuleItem() == null) return;
        IElectricItem item = GTCapabilityHelper.getElectricItem(module.getAppliedTo());
        IElectricItem battery = GTCapabilityHelper.getElectricItem(module.getModuleItem());
        if (item == null || battery == null) return;
        if (item.getCharge() > item.getMaxCharge() * PERCENTAGE / 100) {
            long amount = (long) (item.getCharge() - item.getMaxCharge() * PERCENTAGE / 100);
            item.charge(battery.discharge(amount, battery.getTier(), true, false, false), item.getTier(), true, false);
        } else if (item.getCharge() < item.getMaxCharge() * PERCENTAGE / 100) {
            long amount = (long) (item.getMaxCharge() * PERCENTAGE / 100 - item.getCharge());
            battery.charge(item.discharge(amount, item.getTier(), true, false, false), battery.getTier(), true, false);
        }
    }
}
