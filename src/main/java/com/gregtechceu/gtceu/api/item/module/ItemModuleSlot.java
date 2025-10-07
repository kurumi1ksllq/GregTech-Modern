package com.gregtechceu.gtceu.api.item.module;

import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ItemModuleSlot {

    public static final String MODULE_SLOTS_KEY = "ModuleSlots";
    private static final Map<ResourceLocation, ItemModuleSlot> SLOTS = new HashMap<>();

    @Getter
    private final ResourceLocation id;

    public static @Nullable ItemModuleSlot getById(ResourceLocation id) {
        return SLOTS.get(id);
    }

    protected ItemModuleSlot(ResourceLocation id) {
        this.id = id;
        SLOTS.put(id, this);
    }

    public static void setSlots(ItemStack stack, List<ItemModuleSlot> slots) {
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < slots.size(); i++) {
            ItemModuleSlot slot = slots.get(i);
            if (slot != null) tag.put(String.valueOf(i), slot.serializeNBT());
        }
        stack.getOrCreateTag().put(MODULE_SLOTS_KEY, tag);
    }

    public static List<ItemModuleSlot> getSlots(ItemStack stack) {
        if (!stack.getOrCreateTag().contains(MODULE_SLOTS_KEY, Tag.TAG_COMPOUND)) {
            if (stack.getItem() instanceof IModularItem modularItem) {
                List<ItemModuleSlot> slots = modularItem.getDefaultSlots(stack);
                setSlots(stack, slots);
                return slots;
            } else if (stack.getItem() instanceof ArmorComponentItem armorItem &&
                    armorItem.getArmorLogic() instanceof IModularItem modularItem) {
                        List<ItemModuleSlot> slots = modularItem.getDefaultSlots(stack);
                        setSlots(stack, slots);
                        return slots;
                    }
            return new ArrayList<>();
        } else {
            List<ItemModuleSlot> slots = new ArrayList<>();
            CompoundTag tag = stack.getOrCreateTagElement(MODULE_SLOTS_KEY);
            for (String key : tag.getAllKeys()) {
                int i = Integer.parseInt(key);
                while (slots.size() <= i) slots.add(null);
                slots.set(i, fromNBT(tag.getCompound(key)));
            }
            return slots;
        }
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id.toString());
        return tag;
    }

    public static ItemModuleSlot fromNBT(CompoundTag tag) {
        return getById(ResourceLocation.tryParse(tag.getString("id")));
    }

    public abstract boolean acceptsModule(ItemModule module);

    public IGuiTexture getSlotTexture() {
        return null;
    }
}
