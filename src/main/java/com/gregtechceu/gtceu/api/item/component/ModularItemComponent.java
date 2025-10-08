package com.gregtechceu.gtceu.api.item.component;

import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.item.capability.ModularItem;
import com.gregtechceu.gtceu.api.item.component.forge.IComponentCapability;
import com.gregtechceu.gtceu.api.item.module.ItemModuleSlot;
import com.gregtechceu.gtceu.common.data.GTArmorModifiers;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class ModularItemComponent implements IItemComponent, IComponentCapability {

    private final Function<ItemStack, List<ItemModuleSlot>> defaultSlotGetter;

    public ModularItemComponent(Function<ItemStack, List<ItemModuleSlot>> defaultSlotGetter) {
        this.defaultSlotGetter = defaultSlotGetter;
    }

    public ModularItemComponent(int slots, int maxTier) {
        List<ItemModuleSlot> defaultSlots = new ArrayList<>();
        for (int i = 0; i < slots; i++) defaultSlots.add(GTArmorModifiers.TIERED_SLOTS[maxTier]);
        this.defaultSlotGetter = stack -> defaultSlots;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(ItemStack stack, @NotNull Capability<T> cap) {
        return GTCapability.CAPABILITY_MODULAR_ITEM.orEmpty(cap,
                LazyOptional.of(() -> new ModularItem(stack, defaultSlotGetter)));
    }
}
