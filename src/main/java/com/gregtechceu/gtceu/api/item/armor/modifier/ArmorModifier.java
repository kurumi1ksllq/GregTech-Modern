package com.gregtechceu.gtceu.api.item.armor.modifier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ArmorModifier {

    public static final Map<ResourceLocation, ArmorModifier> MODIFIERS = new HashMap<>();
    public static final Codec<ArmorModifier> CODEC = ResourceLocation.CODEC.xmap(ArmorModifier.MODIFIERS::get,
            ArmorModifier::getId);

    @Getter
    public final ResourceLocation id;
    @Getter
    public final ItemModifier onAddToItem;
    @Getter
    public final Modifier onAdd;
    @Getter
    public final Modifier onRemove;

    public ArmorModifier(ResourceLocation id, Modifier onAdd, Modifier onRemove) {
        this.id = id;
        this.onAddToItem = ItemModifier.IDENTITY;
        this.onAdd = onAdd;
        this.onRemove = onRemove;
    }

    public ArmorModifier(ResourceLocation id, Attribute attribute, AttributeModifier modifier, EquipmentSlot slot) {
        this.id = id;
        this.onAddToItem = (stack) -> {
            if (stack.getAttributeModifiers(slot).containsEntry(attribute, modifier)) {
                return;
            }
            stack.addAttributeModifier(attribute, modifier, slot);
        };
        this.onAdd = Modifier.IDENTITY;
        this.onRemove = Modifier.IDENTITY;
    }

    @FunctionalInterface
    public interface Modifier {

        Modifier IDENTITY = (stack, entity) -> {};

        void apply(ItemStack stack, @Nullable LivingEntity entity);
    }

    @FunctionalInterface
    public interface ItemModifier {

        ItemModifier IDENTITY = (stack) -> {};

        void apply(ItemStack stack);
    }
}
