package com.gregtechceu.gtceu.api.item.modifier;

import com.gregtechceu.gtceu.api.item.armor.modifier.AppliedArmorModifier;
import lombok.Getter;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
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
        if (MODULES.contains(id)) {
            GTCEu.LOGGER.warn("Attempted to create 2 modules with the same id: {}", id);
        } else MODULES.put(id, this);
    }

    public void onAttach(ItemStack stack, AppliedArmorModifier modifier);

    public void onRemove(ItemStack stack, AppliedArmorModifier modifier);

    public void onEquip(LivingEntity entity, ItemStack stack, AppliedArmorModifier modifier);

    public void onTick(LivingEntity entity, ItemStack stack, AppliedArmorModifier modifier);

    public void onUnequip(LivingEntity entity, ItemStack stack, AppliedArmorModifier modifier);

    public boolean canRemove(ItemStack stack, AppliedArmorModifier modifier) {
        return true;
    }
}
