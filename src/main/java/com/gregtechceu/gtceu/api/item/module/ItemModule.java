package com.gregtechceu.gtceu.api.item.module;

import com.gregtechceu.gtceu.GTCEu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

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

    public void onArmorTick(LivingEntity entity, AppliedItemModule modifier) {}

    public void onUnequip(LivingEntity entity, AppliedItemModule modifier) {}

    public void onInventoryTick(Player player, AppliedItemModule modifier) {}

    public boolean canRemove(AppliedItemModule modifier) {
        return true;
    }
}
