package com.gregtechceu.gtceu.api.item.module;

import com.gregtechceu.gtceu.api.item.armor.IArmorLogic;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

public abstract class ArmorLogicItemModule extends ItemModule {

    public ArmorLogicItemModule(ResourceLocation id) {
        super(id);
    }

    protected abstract @Nullable IArmorLogic getArmorLogic(AppliedItemModule module);

    @Override
    public void onArmorTick(LivingEntity entity, AppliedItemModule module) {
        super.onArmorTick(entity, module);
        IArmorLogic armorLogic = getArmorLogic(module);
        if (armorLogic == null) return;
        if (entity instanceof Player player) armorLogic.onArmorTick(player.level(), player, module.getAppliedTo());
    }

    @Override
    public void onEquip(LivingEntity entity, AppliedItemModule module) {
        super.onEquip(entity, module);
        IArmorLogic armorLogic = getArmorLogic(module);
        if (armorLogic == null) return;
        if (entity instanceof Player player) armorLogic.onEquip(player);
    }

    @Override
    public void onUnequip(LivingEntity entity, AppliedItemModule module) {
        super.onUnequip(entity, module);
        IArmorLogic armorLogic = getArmorLogic(module);
        if (armorLogic == null) return;
        if (entity instanceof Player player) armorLogic.onUnequip(player);
    }

    @Override
    public boolean isPPE(AppliedItemModule module) {
        IArmorLogic armorLogic = getArmorLogic(module);
        if (armorLogic == null) return super.isPPE(module);
        return armorLogic.isPPE(module.getAppliedTo());
    }
}
