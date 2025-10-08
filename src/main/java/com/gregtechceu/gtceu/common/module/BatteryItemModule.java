package com.gregtechceu.gtceu.common.module;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.item.module.AppliedItemModule;
import com.gregtechceu.gtceu.api.item.module.ICapabilityModule;
import com.gregtechceu.gtceu.api.item.module.ItemModule;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class BatteryItemModule extends ItemModule implements ICapabilityModule {

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
        if (item == null || battery == null || item == battery) return;
        if (item.getCharge() > item.getMaxCharge() * PERCENTAGE / 100) {
            long amount = (long) (item.getCharge() - item.getMaxCharge() * PERCENTAGE / 100);
            item.charge(battery.discharge(amount, battery.getTier(), true, false, false), item.getTier(), true, false);
        } else if (item.getCharge() < item.getMaxCharge() * PERCENTAGE / 100) {
            long amount = (long) (item.getMaxCharge() * PERCENTAGE / 100 - item.getCharge());
            battery.charge(item.discharge(amount, item.getTier(), true, false, false), battery.getTier(), true, false);
        }
    }

    @Override
    public void appendHoverText(Level level, TooltipFlag isAdvanced, List<Component> tooltips,
                                AppliedItemModule module) {
        super.appendHoverText(level, isAdvanced, tooltips, module);
        tooltips.add(
                Component.translatable("metaarmor.tooltip.modifier.battery", module.getModuleItem().getDisplayName()));
        if (module.getModuleItem() != null) {
            module.getModuleItem().getItem().appendHoverText(module.getModuleItem(), level, tooltips, isAdvanced);
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(AppliedItemModule module, @NotNull Capability<T> cap) {
        if (cap == GTCapability.CAPABILITY_ELECTRIC_ITEM && module.getModuleItem() != null) {
            return module.getModuleItem().getCapability(cap);
        } else return LazyOptional.empty();
    }
}
