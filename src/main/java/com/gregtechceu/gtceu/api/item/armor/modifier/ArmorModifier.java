package com.gregtechceu.gtceu.api.item.armor.modifier;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

@SuppressWarnings("FieldMayBeFinal")
@Accessors(chain = true, fluent = true)
public class ArmorModifier {

    public static final Map<ResourceLocation, ArmorModifier> MODIFIERS = new HashMap<>();
    public static final Codec<ArmorModifier> CODEC = ResourceLocation.CODEC.xmap(ArmorModifier.MODIFIERS::get,
            ArmorModifier::id);

    @Getter
    private final ResourceLocation id;
    @Getter
    private ItemModifier onAddToItem;
    @Getter
    private ItemModifier onRemove;
    @Getter
    private Modifier onEquip;
    @Getter
    private Modifier onTick;
    @Getter
    private Modifier onUnequip;
    @Getter
    private boolean canRemove;

    @Getter
    @Setter
    private DamageModifier onDamage = DamageModifier.NONE;
    @Getter
    @Setter
    private BiConsumer<AppliedArmorModifier, List<Component>> tooltips = (modifier, tooltips) -> {};

    protected ArmorModifier(ResourceLocation id, ItemModifier onAddToItem, ItemModifier onRemove,
                            Modifier onEquip, Modifier onTick, Modifier onUnequip, boolean canRemove) {
        this.id = id;
        this.onAddToItem = onAddToItem;
        this.onRemove = onRemove;
        this.onEquip = onEquip;
        this.onTick = onTick;
        this.onUnequip = onUnequip;
        this.canRemove = canRemove;
        MODIFIERS.put(id, this);
    }

    public ArmorModifier energyUsagePerTick(long energyUsage) {
        return this.energyUsagePerTick(energyUsage, (entity, itemStack) -> true);
    }

    public ArmorModifier energyUsagePerTick(long energyUsage, BiPredicate<LivingEntity, ItemStack> doDrain) {
        this.onTick = this.onTick.compose((entity, stack, appliedModifier) -> {
            if (!doDrain.test(entity, stack)) {
                return true;
            }
            IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
            if (electricItem != null) {
                if (!electricItem.canUse(energyUsage)) {
                    return false;
                }
                electricItem.discharge(energyUsage, electricItem.getTier(), true, false, false);
            }
            return true;
        });
        return this;
    }

    public ArmorModifier energyUsageOnHit(long energyUsage) {
        this.onDamage = this.onDamage.compose((entity, stack, source, amount, modifier) -> {
            IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
            if (electricItem != null) {
                if (!electricItem.canUse(energyUsage)) {
                    return new DamageModifier.Result(amount, false);
                }
                electricItem.discharge(energyUsage, electricItem.getTier(), true, false, false);
            }
            return new DamageModifier.Result(amount);
        });
        return this;
    }

    public static ArmorModifier createItem(ResourceLocation id, ItemModifier onAdd, ItemModifier onRemove) {
        return new ArmorModifier(id, onAdd, onRemove, Modifier.NONE, Modifier.NONE, Modifier.NONE, true);
    }

    public static ArmorModifier createItemAttribute(ResourceLocation id,
                                                    Attribute attribute,
                                                    BiFunction<ItemStack, AppliedArmorModifier, AttributeModifier> modifier,
                                                    @Nullable EquipmentSlot slot) {
        return createItem(id, (stack, appliedArmorModifier) -> {
            if (appliedArmorModifier.getTag().contains("modifierIndex")) return;
            EquipmentSlot slot1 = slot != null ? slot : LivingEntity.getEquipmentSlotForItem(stack);
            AttributeModifier attributeModifier = modifier.apply(stack, appliedArmorModifier);
            stack.addAttributeModifier(attribute, attributeModifier, slot1);
            appliedArmorModifier.getTag().putUUID("modifierUUID", attributeModifier.getId());
        }, (stack, appliedArmorModifier) -> {
            UUID uuid = appliedArmorModifier.getTag().getUUID("modifierUUID");
            ListTag listTag = stack.getOrCreateTag().getList("AttributeModifiers", Tag.TAG_COMPOUND);
            Iterator<Tag> it = listTag.iterator();
            while (it.hasNext()) {
                Tag tag = it.next();
                if (tag instanceof CompoundTag compoundTag) {
                    AttributeModifier attributeModifier = AttributeModifier.load(compoundTag);
                    if (attributeModifier != null && attributeModifier.getId().equals(uuid)) it.remove();
                }
            }
        });
    }

    public static ArmorModifier createItemAttribute(ResourceLocation id, Attribute attribute,
                                                    AttributeModifier modifier, @Nullable EquipmentSlot slot) {
        return createItemAttribute(id, attribute, (stack, appliedArmorModifier) -> modifier, slot);
    }

    public static ArmorModifier createEntity(ResourceLocation id,
                                             Modifier onEquip, Modifier onTick, Modifier onUnequip) {
        return new ArmorModifier(id, ItemModifier.NONE, ItemModifier.NONE, onEquip, onTick, onUnequip, true);
    }

    public static ArmorModifier createEntityTick(ResourceLocation id, Modifier onTick) {
        return new ArmorModifier(id, ItemModifier.NONE, ItemModifier.NONE, Modifier.NONE, onTick, Modifier.NONE, true);
    }

    public static ArmorModifier createEntityAttribute(ResourceLocation id,
                                                      Attribute attribute,
                                                      ModifierInformedSupplier<AttributeModifier> modifierSupplier) {
        return createEntity(id, (entity, stack, appliedModifier) -> {
            AttributeInstance attributeInstance = entity.getAttribute(attribute);
            if (attributeInstance == null) return true;
            AttributeModifier modifier = modifierSupplier.apply(entity, stack, appliedModifier);
            appliedModifier.getTag().putUUID("attributeModifierId", modifier.getId());
            attributeInstance.addPermanentModifier(modifier);
            return true;
        }, Modifier.NONE, (entity, stack, appliedModifier) -> {
            AttributeInstance attributeInstance = entity.getAttribute(attribute);
            if (attributeInstance == null) return true;
            attributeInstance.removePermanentModifier(appliedModifier.getTag().getUUID("attributeModifierId"));
            return true;
        });
    }

    public static ArmorModifier createAll(ResourceLocation id, ItemModifier onAddToItem, ItemModifier onRemove,
                                          Modifier onEquip, Modifier onTick, Modifier onUnequip, boolean canRemove) {
        return new ArmorModifier(id, onAddToItem, onRemove, onEquip, onTick, onUnequip, canRemove);
    }

    public static ArmorModifier createSpecial(ResourceLocation id) {
        return new ArmorModifier(id, ItemModifier.NONE, ItemModifier.NONE, Modifier.NONE, Modifier.NONE, Modifier.NONE,
                true);
    }

    @FunctionalInterface
    public interface ModifierInformedSupplier<T> {

        T apply(@NotNull LivingEntity entity, @NotNull ItemStack stack, AppliedArmorModifier modifier);
    }

    @FunctionalInterface
    public interface Modifier extends ModifierInformedSupplier<Boolean> {

        Modifier NONE = (entity, stack, modifier) -> true;

        Boolean apply(@NotNull LivingEntity entity, @NotNull ItemStack stack, AppliedArmorModifier modifier);

        default Modifier andThen(Modifier after) {
            return (entity, stack, modifier) -> {
                if (this.apply(entity, stack, modifier)) {
                    return after.apply(entity, stack, modifier);
                }
                return false;
            };
        }

        default Modifier compose(Modifier before) {
            return (entity, stack, modifier) -> {
                if (before.apply(entity, stack, modifier)) {
                    return this.apply(entity, stack, modifier);
                }
                return false;
            };
        }
    }

    @FunctionalInterface
    public interface ItemModifier {

        ItemModifier NONE = (stack, modifier) -> {};

        void apply(ItemStack stack, AppliedArmorModifier modifier);

        default ItemModifier andThen(ItemModifier after) {
            return (stack, modifier) -> {
                this.apply(stack, modifier);
                after.apply(stack, modifier);
            };
        }
    }

    @FunctionalInterface
    public interface DamageModifier {

        DamageModifier NONE = (entity, stack, source, amount, modifier) -> new Result(amount);

        Result apply(@NotNull LivingEntity entity, @NotNull ItemStack stack,
                     @NotNull DamageSource source, float amount, AppliedArmorModifier modifier);

        default DamageModifier compose(DamageModifier before) {
            return (entity, stack, source, amount, modifier) -> {
                var result = before.apply(entity, stack, source, amount, modifier);
                if (!result.doApplyNext) {
                    return result;
                }
                return this.apply(entity, stack, source, result.newAmount, modifier);
            };
        }

        default DamageModifier andThen(DamageModifier after) {
            return (entity, stack, source, amount, modifier) -> {
                var result = this.apply(entity, stack, source, amount, modifier);
                if (!result.doApplyNext) {
                    return result;
                }
                return after.apply(entity, stack, source, result.newAmount, modifier);
            };
        }

        record Result(float newAmount, boolean doApplyNext) {

            public Result(float newAmount) {
                this(newAmount, true);
            }
        }
    }
}
