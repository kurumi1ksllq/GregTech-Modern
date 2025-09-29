package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.api.item.armor.modifier.ArmorModifier;
import com.gregtechceu.gtceu.core.IFireImmuneEntity;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.input.SyncedKeyMappings;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class GTArmorModifiers {

    private static final double SPEED_ACCEL = 0.085D;

    private static final UUID ADD_ARMOR_UUID = UUID.fromString("95bd81ea-b3af-4cca-8866-f3e62f5f68f1");
    private static final UUID MUL_DAMAGE_UUID = UUID.fromString("a5bd81ea-b3af-4cca-8866-f3e62f5f68f1");
    private static final UUID MUL_ATTACK_SPEED_UUID = UUID.fromString("b5bd81ea-b3af-4cca-8866-f3e62f5f68f1");
    private static final UUID ADD_BLOCK_REACH_UUID = UUID.fromString("c5bd81ea-b3af-4cca-8866-f3e62f5f68f1");

    public static final ArmorModifier ADD_ARMOR_1 = ArmorModifier
            .createItemAttribute(GTCEu.id("add_armor_1"),
                    Attributes.ARMOR,
                    new AttributeModifier(ADD_ARMOR_UUID, "Armor Modifier", 1.0D, AttributeModifier.Operation.ADDITION),
                    null)
            .energyUsageOnHit(1024);

    public static final ArmorModifier ADD_ARMOR_2 = ArmorModifier
            .createItemAttribute(GTCEu.id("add_armor_2"),
                    Attributes.ARMOR,
                    new AttributeModifier(ADD_ARMOR_UUID, "Armor Modifier", 2.0D, AttributeModifier.Operation.ADDITION),
                    null)
            .energyUsageOnHit(2048);

    public static final ArmorModifier ARMOR_PLATE_TUNGSTENSTEEL = ArmorModifier
            .createItemAttribute(GTCEu.id("add_armor_5"),
                    Attributes.ARMOR,
                    new AttributeModifier(ADD_ARMOR_UUID, "Armor Modifier", 10.0D,
                            AttributeModifier.Operation.ADDITION),
                    null)
            .energyUsageOnHit(5120);
    public static final ArmorModifier SPEED = ArmorModifier.createEntityTick(GTCEu.id("speed"),
            (entity, stack, modifier) -> {
                if (entity instanceof Player player) {
                    float mul = (GTUtil.getTier(modifier.getModifierItem().getItem()) - 1) / 4f + 1;
                    boolean sprinting = SyncedKeyMappings.VANILLA_FORWARD.isKeyDown(player) && player.isSprinting();
                    boolean jumping = SyncedKeyMappings.VANILLA_JUMP.isKeyDown(player);
                    boolean sneaking = SyncedKeyMappings.VANILLA_SNEAK.isKeyDown(player);

                    if ((player.onGround() || player.isInWater()) && sprinting) {
                        float speed = 0.25F * mul;
                        if (player.isInWater()) {
                            speed = 0.1F * mul;
                            if (jumping) {
                                player.push(0.0, 0.1, 0.0);
                            }
                        }
                        player.moveRelative(speed, new Vec3(0, 0, 1));
                    } else if (player.isInWater() && (sneaking || jumping)) {
                        if (sneaking)
                            player.push(0.0, -SPEED_ACCEL * mul, 0.0);
                        if (jumping)
                            player.push(0.0, SPEED_ACCEL * mul, 0.0);
                    }
                }
                return true;
            })
            .energyUsagePerTick(819, (entity, itemStack) -> {
                if (entity instanceof Player player) {
                    return SyncedKeyMappings.VANILLA_FORWARD.isKeyDown(player) && player.isSprinting();
                }
                return false;
            })
            .tooltips((stack, tooltips) -> {
                tooltips.add(Component.translatable("metaarmor.tooltip.modifier.speed",
                        GTValues.VN[GTUtil.getTier(stack.getModifierItem().getItem())]));
            });
    public static final ArmorModifier FIRE_PROTECTION = ArmorModifier.createEntity(GTCEu.id("fire_protection"),
            (entity, stack, modifier) -> {
                if (!entity.level().isClientSide && !entity.fireImmune()) {
                    ((IFireImmuneEntity) entity).gtceu$setFireImmune(true);
                    if (entity.isOnFire()) entity.extinguishFire();
                }
                return true;
            },
            ArmorModifier.Modifier.NONE,
            (entity, stack, modifier) -> {
                if (!entity.level().isClientSide) {
                    ((IFireImmuneEntity) entity).gtceu$setFireImmune(false);
                }
                return true;
            });
    public static final ArmorModifier DAMAGE_BLOCK = ArmorModifier.createSpecial(GTCEu.id("damage_block"))
            .onDamage((entity, stack, source, amount, modifier) -> {
                float div = (GTUtil.getTier(modifier.getModifierItem().getItem()) - 1) / 4f + 1;
                long energyPerHP = (long) (8192 / div);
                double percentage = 25;
                if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_FALL) ||
                        source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.STARVE)) {
                    return new ArmorModifier.DamageModifier.Result(amount);
                }

                int damageReduction = Integer.MAX_VALUE;
                IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
                if (electricItem == null) {
                    return new ArmorModifier.DamageModifier.Result(amount);
                }
                damageReduction = (int) Math.min(damageReduction,
                        percentage * electricItem.getCharge() / (energyPerHP * 100.0D));
                damageReduction = Math.toIntExact(electricItem.discharge(
                        damageReduction * energyPerHP,
                        electricItem.getTier(),
                        true, false, false) / energyPerHP);
                return new ArmorModifier.DamageModifier.Result(Math.max(amount - damageReduction, 0));
            }).tooltips((stack, tooltips) -> {
                tooltips.add(Component.translatable("metaarmor.tooltip.modifier.damage_block",
                        GTValues.VN[GTUtil.getTier(stack.getModifierItem().getItem())]));
            });;

    public static final ArmorModifier ATTACK_SPEED = ArmorModifier.createItemAttribute(
            GTCEu.id("attack_speed"),
            Attributes.ATTACK_SPEED,
            (stack, modifier) -> {
                double mul = 1 + GTUtil.getTier(modifier.getModifierItem().getItem()) / 16d;
                return new AttributeModifier(MUL_ATTACK_SPEED_UUID, "Attack Speed Modifier", mul,
                        AttributeModifier.Operation.MULTIPLY_TOTAL);
            }, null).tooltips((stack, tooltips) -> {
                tooltips.add(Component.translatable("metaarmor.tooltip.modifier.attack_speed",
                        GTValues.VN[GTUtil.getTier(stack.getModifierItem().getItem())]));
            });

    public static final ArmorModifier ATTACK_DAMAGE = ArmorModifier.createItemAttribute(
            GTCEu.id("attack_damage"),
            Attributes.ATTACK_DAMAGE,
            (stack, modifier) -> {
                double mul = 1 + GTUtil.getTier(modifier.getModifierItem().getItem()) / 16d;
                return new AttributeModifier(MUL_DAMAGE_UUID, "Attack Damage Modifier", mul,
                        AttributeModifier.Operation.MULTIPLY_TOTAL);
            }, null).tooltips((stack, tooltips) -> {
                tooltips.add(Component.translatable("metaarmor.tooltip.modifier.attack_damage",
                        GTValues.VN[GTUtil.getTier(stack.getModifierItem().getItem())]));
            });

    public static final ArmorModifier BLOCK_REACH = ArmorModifier.createItemAttribute(
            GTCEu.id("block_reach"),
            ForgeMod.BLOCK_REACH,
            (stack, modifier) -> {
                double add = GTUtil.getTier(modifier.getModifierItem().getItem()) / 2d;
                return new AttributeModifier(ADD_BLOCK_REACH_UUID, "Block Reach Modifier", add,
                        AttributeModifier.Operation.ADDITION);
            }, null).tooltips((stack, tooltips) -> {
                tooltips.add(Component.translatable("metaarmor.tooltip.modifier.block_reach",
                        GTValues.VN[GTUtil.getTier(stack.getModifierItem().getItem())]));
            });

    public static final ArmorModifier BATTERY = ArmorModifier.createEntityTick(GTCEu.id("battery"),
            (entity, stack, modifier) -> {
                IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
                IElectricItem battery = GTCapabilityHelper.getElectricItem(modifier.getModifierItem());
                if (electricItem == null || !electricItem.chargeable() || battery == null ||
                        !battery.canProvideChargeExternally())
                    return true;
                long energy = electricItem.getMaxCharge() - electricItem.getCharge();
                long simulated = battery.discharge(energy, battery.getTier(), true, false, true);
                long actualEnergy = electricItem.charge(simulated, electricItem.getTier(), true, true);
                long discharged = battery.discharge(actualEnergy, battery.getTier(), true, false, false);
                electricItem.charge(discharged, electricItem.getTier(), true, false);
                return true;
            }).tooltips((stack, tooltips) -> {
                tooltips.add(Component.translatable("metaarmor.tooltip.modifier.battery",
                        stack.getModifierItem().getDisplayName()));
            });

    public static final ArmorModifier JETPACK = ArmorModifier.createEntityTick(GTCEu.id("jetpack"),
            (entity, stack, modifier) -> {
                if (modifier.getModifierItem().getItem() instanceof ArmorComponentItem armorComponentItem) {
                    armorComponentItem.onArmorTick(stack, entity.level(), (Player) entity);
                }
                return true;
            }).tooltips((modifier, tooltip) -> {
                tooltip.add(Component.translatable("metaarmor.tooltip.modifier.jetpack",
                        modifier.getModifierItem().getDisplayName()));
                modifier.getModifierItem().getItem().appendHoverText(modifier.getModifierItem(), null, tooltip,
                        TooltipFlag.NORMAL);
            });

    public static void init() {}
}
