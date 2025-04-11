package com.gregtechceu.gtceu.common.data;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IElectricItem;
import com.gregtechceu.gtceu.api.item.armor.modifier.ArmorModifier;
import com.gregtechceu.gtceu.core.IFireImmuneEntity;
import com.gregtechceu.gtceu.utils.input.KeyBind;

import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

@Slf4j
public class GTArmorModifiers {

    private static final double SPEED_ACCEL = 0.085D;

    private static final UUID ADD_ARMOR_UUID = UUID.fromString("95bd81ea-b3af-4cca-8866-f3e62f5f68f1");

    public static final ArmorModifier ADD_ARMOR_1 = ArmorModifier.createItemAttribute(GTCEu.id("add_armor_1"),
            Attributes.ARMOR,
            new AttributeModifier(ADD_ARMOR_UUID, "Armor Modifier", 1.0D, AttributeModifier.Operation.ADDITION),
            null)
            .energyUsageOnHit(1024);
    public static final ArmorModifier ADD_ARMOR_2 = ArmorModifier.createItemAttribute(GTCEu.id("add_armor_2"),
            Attributes.ARMOR,
            new AttributeModifier(ADD_ARMOR_UUID, "Armor Modifier", 2.0D, AttributeModifier.Operation.ADDITION),
            null)
            .energyUsageOnHit(2048);
    public static final ArmorModifier ARMOR_PLATE_TUNGSTENSTEEL = ArmorModifier.createItemAttribute(GTCEu.id("add_armor_5"),
            Attributes.ARMOR,
            new AttributeModifier(ADD_ARMOR_UUID, "Armor Modifier", 10.0D, AttributeModifier.Operation.ADDITION),
            null)
            .energyUsageOnHit(5120);
    public static final ArmorModifier SPEED = ArmorModifier.createEntityTick(GTCEu.id("speed"),
            (entity, stack) -> {
                if (entity instanceof Player player) {
                    boolean sprinting = KeyBind.VANILLA_FORWARD.isKeyDown(player) && player.isSprinting();
                    boolean jumping = KeyBind.VANILLA_JUMP.isKeyDown(player);
                    boolean sneaking = KeyBind.VANILLA_SNEAK.isKeyDown(player);

                    if ((player.onGround() || player.isInWater()) && sprinting) {
                        float speed = 0.25F;
                        if (player.isInWater()) {
                            speed = 0.1F;
                            if (jumping) {
                                player.push(0.0, 0.1, 0.0);
                            }
                        }
                        player.moveRelative(speed, new Vec3(0, 0, 1));
                    } else if (player.isInWater() && (sneaking || jumping)) {
                        if (sneaking)
                            player.push(0.0, -SPEED_ACCEL, 0.0);
                        if (jumping)
                            player.push(0.0, SPEED_ACCEL, 0.0);
                    }
                }
                return true;
            })
            .energyUsagePerTick(819, (entity, itemStack) -> {
                if (entity instanceof Player player) {
                    return KeyBind.VANILLA_FORWARD.isKeyDown(player) && player.isSprinting();
                }
                return false;
            })
            .tooltips((stack, tooltips) -> {
                tooltips.add(Component.translatable("metaarmor.tooltip.speed"));
            });
    public static final ArmorModifier FIRE_PROTECTION = ArmorModifier.createEntity(GTCEu.id("fire_protection"),
            (entity, stack) -> {
                if (!entity.level().isClientSide && !entity.fireImmune()) {
                    ((IFireImmuneEntity) entity).gtceu$setFireImmune(true);
                    if (entity.isOnFire()) entity.extinguishFire();
                }
                return true;
            },
            ArmorModifier.Modifier.NONE,
            (entity, stack) -> {
                if (!entity.level().isClientSide) {
                    ((IFireImmuneEntity) entity).gtceu$setFireImmune(false);
                }
                return true;
            });
    public static final ArmorModifier DAMAGE_BLOCK = ArmorModifier.createSpecial(GTCEu.id("damage_block"))
            .onDamage((entity, stack, source, amount) -> {
                if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || source.is(DamageTypeTags.IS_FALL) ||
                        source.is(DamageTypeTags.IS_DROWNING) || source.is(DamageTypes.STARVE)) {
                    return new ArmorModifier.DamageModifier.Result(amount);
                }

                int damageLimit = Integer.MAX_VALUE;
                IElectricItem electricItem = GTCapabilityHelper.getElectricItem(stack);
                if (electricItem == null) {
                    return new ArmorModifier.DamageModifier.Result(amount);
                }
                damageLimit = (int) Math.min(damageLimit, 25.0D * electricItem.getCharge() / (8192 * 100.0D));
                return new ArmorModifier.DamageModifier.Result(Math.min(amount, damageLimit));
            })
            .energyUsageOnHit(8192);

    public static void init() {}
}
