package com.gregtechceu.gtceu.api.item.module;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ItemModule {

    private static final Map<ResourceLocation, ItemModule> MODULES = new HashMap<>();

    @Getter
    private final ResourceLocation id;

    public static @Nullable ItemModule getModuleById(ResourceLocation id) {
        return MODULES.get(id);
    }

    public ItemModule(ResourceLocation id) {
        this.id = id;
        if (MODULES.containsKey(id)) {
            GTCEu.LOGGER.warn("Attempted to create 2 modules with the same id: {}", id);
        } else MODULES.put(id, this);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        return tag;
    }

    public static ItemModule fromNBT(CompoundTag tag) {
        return getModuleById(ResourceLocation.tryParse(tag.getString("id")));
    }

    public void onAttach(AppliedItemModule modifier) {}

    public void onRemove(AppliedItemModule modifier) {}

    public void onEquip(LivingEntity entity, AppliedItemModule modifier) {}

    public void onArmorTick(LivingEntity entity, AppliedItemModule module) {
        IElectricItem electricItem = GTCapabilityHelper.getElectricItem(module.getModuleItem());
        long energy = energyUsagePerTick(entity, module);
        if (electricItem != null) {
            electricItem.discharge(energy, electricItem.getTier(), true, false, false);
        }
    }

    public void onUnequip(LivingEntity entity, AppliedItemModule modifier) {}

    public void onInventoryTick(Player player, AppliedItemModule module) {
        if (module.getModuleItem() == null) return;
        IElectricItem electricItem = GTCapabilityHelper.getElectricItem(module.getModuleItem());
        long energy = energyUsagePerTick(player, module);
        if (electricItem != null && useEnergyInInventory(player, module)) {
            electricItem.discharge(energy, electricItem.getTier(), true, false, false);
        }
    }

    public void appendHoverText(Level level, TooltipFlag isAdvanced, List<Component> tooltips,
                                AppliedItemModule module) {}

    public boolean useEnergyInInventory(LivingEntity entity, AppliedItemModule module) {
        return true;
    }

    public long energyUsagePerTick(LivingEntity entity, AppliedItemModule module) {
        return 0;
    }

    public float changeDamage(LivingEntity entity, AppliedItemModule modifier, float damage, DamageSource source) {
        return damage;
    }

    public boolean canRemove(AppliedItemModule modifier) {
        return true;
    }

    public boolean canApplyTo(ItemStack stack) {
        return true;
    }
}
