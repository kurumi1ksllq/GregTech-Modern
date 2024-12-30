package com.gregtechceu.gtceu.api.item.armor.modifier;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
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
    private Modifier onEquip;
    @Getter
    private Modifier onTick;
    @Getter
    private Modifier onUnequip;

    @Getter
    @Setter
    private DamageModifier onDamage = DamageModifier.NONE;
    @Getter
    @Setter
    private BiConsumer<ItemStack, List<Component>> tooltips = (stack, tooltips) -> {};

    protected ArmorModifier(ResourceLocation id, ItemModifier onAddToItem,
                            Modifier onEquip, Modifier onTick, Modifier onUnequip) {
        this.id = id;
        this.onAddToItem = onAddToItem;
        this.onEquip = onEquip;
        this.onTick = onTick;
        this.onUnequip = onUnequip;
        MODIFIERS.put(id, this);
    }

    public ArmorModifier energyUsagePerTick(long energyUsage) {
        return this.energyUsagePerTick(energyUsage, (entity, itemStack) -> true);
    }

    public ArmorModifier energyUsagePerTick(long energyUsage, BiPredicate<LivingEntity, ItemStack> doDrain) {
        this.onTick = this.onTick.compose((entity, stack) -> {
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
        this.onDamage = this.onDamage.compose((entity, stack, source, amount) -> {
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

    public static ArmorModifier createItem(ResourceLocation id, ItemModifier modifier) {
        return new ArmorModifier(id, modifier, Modifier.NONE, Modifier.NONE, Modifier.NONE);
    }

    public static ArmorModifier createItemAttribute(ResourceLocation id,
                                                    Attribute attribute, AttributeModifier modifier,
                                                    @Nullable EquipmentSlot slot) {
        return createItem(id, (stack) -> {
            EquipmentSlot slot1 = slot != null ? slot : LivingEntity.getEquipmentSlotForItem(stack);
            stack.addAttributeModifier(attribute, modifier, slot1);
        });
    }

    public static ArmorModifier createEntity(ResourceLocation id,
                                             Modifier onEquip, Modifier onTick, Modifier onUnequip) {
        return new ArmorModifier(id, ItemModifier.NONE, onEquip, onTick, onUnequip);
    }

    public static ArmorModifier createEntityTick(ResourceLocation id, Modifier onTick) {
        return new ArmorModifier(id, ItemModifier.NONE, Modifier.NONE, onTick, Modifier.NONE);
    }

    public static ArmorModifier createEntityAttribute(ResourceLocation id,
                                                      Attribute attribute, AttributeModifier modifier) {
        return createEntity(id, (entity, stack) -> {
            if (entity.getAttribute(attribute) == null) return true;
            entity.getAttribute(attribute).addPermanentModifier(modifier);
            return true;
        }, Modifier.NONE, (entity, stack) -> {
            if (entity.getAttribute(attribute) == null) return true;
            entity.getAttribute(attribute).removeModifier(modifier);
            return true;
        });
    }

    public static ArmorModifier createAll(ResourceLocation id, ItemModifier onAddToItem,
                                          Modifier onEquip, Modifier onTick, Modifier onUnequip) {
        return new ArmorModifier(id, onAddToItem, onEquip, onTick, onUnequip);
    }

    public static ArmorModifier createSpecial(ResourceLocation id) {
        return new ArmorModifier(id, ItemModifier.NONE, Modifier.NONE, Modifier.NONE, Modifier.NONE);
    }

    @FunctionalInterface
    public interface Modifier {

        Modifier NONE = (entity, stack) -> true;

        boolean apply(@NotNull LivingEntity entity, @NotNull ItemStack stack);

        default Modifier andThen(Modifier after) {
            return (entity, stack) -> {
                if (this.apply(entity, stack)) {
                    return after.apply(entity, stack);
                }
                return false;
            };
        }

        default Modifier compose(Modifier before) {
            return (entity, stack) -> {
                if (before.apply(entity, stack)) {
                    return this.apply(entity, stack);
                }
                return false;
            };
        }
    }

    @FunctionalInterface
    public interface ItemModifier {

        ItemModifier NONE = (stack) -> {};

        void apply(ItemStack stack);

        default ItemModifier andThen(ItemModifier after) {
            return (stack) -> {
                this.apply(stack);
                after.apply(stack);
            };
        }
    }

    @FunctionalInterface
    public interface DamageModifier {

        DamageModifier NONE = (entity, stack, source, amount) -> new Result(amount);

        Result apply(@NotNull LivingEntity entity, @NotNull ItemStack stack,
                     @NotNull DamageSource source, float amount);

        default DamageModifier compose(DamageModifier before) {
            return (entity, stack, source, amount) -> {
                var result = before.apply(entity, stack, source, amount);
                if (!result.doApplyNext) {
                    return result;
                }
                return this.apply(entity, stack, source, result.newAmount);
            };
        }

        default DamageModifier andThen(DamageModifier after) {
            return (entity, stack, source, amount) -> {
                var result = this.apply(entity, stack, source, amount);
                if (!result.doApplyNext) {
                    return result;
                }
                return after.apply(entity, stack, source, result.newAmount);
            };
        }

        record Result(float newAmount, boolean doApplyNext) {

            public Result(float newAmount) {
                this(newAmount, true);
            }
        }
    }
}
