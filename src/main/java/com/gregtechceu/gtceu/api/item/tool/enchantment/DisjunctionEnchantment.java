package com.gregtechceu.gtceu.api.item.tool.enchantment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class DisjunctionEnchantment extends Enchantment {

    public static final DisjunctionEnchantment INSTANCE = new DisjunctionEnchantment();

    private DisjunctionEnchantment() {
        super(Rarity.UNCOMMON, EnchantmentCategory.WEAPON, new EquipmentSlot[] {EquipmentSlot.MAINHAND});

    }

    @Override
    public int getMinCost(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 5;
    }

    @Override
    public void doPostAttack(LivingEntity attacker, Entity target, int level) {
        ResourceLocation entityName = EntityType.getKey(target.getType());
        if(target instanceof LivingEntity livingEntity &&
                (target instanceof EnderMan || target instanceof EnderDragon || target instanceof Endermite ||
                        entityName.getPath().contains("ender"))) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, level * 200, Math.max(1, (5 * level) / 7)));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, level * 200, Math.max(1, (5 * level) / 7)));
        }
    }
}
