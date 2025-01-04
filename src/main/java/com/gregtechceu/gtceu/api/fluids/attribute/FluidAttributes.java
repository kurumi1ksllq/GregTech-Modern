package com.gregtechceu.gtceu.api.fluids.attribute;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.utils.EntityDamageUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class FluidAttributes {

    /**
     * Attribute for acidic fluids.
     */
    public static final FluidAttribute ACID = new FluidAttribute(GTCEu.id("acid"),
            list -> list.accept(Component.translatable("gtceu.fluid.type_acid.tooltip")),
            list -> list.accept(Component.translatable("gtceu.fluid_pipe.acid_proof")),
            (w, b, f) -> {
                w.playSound(null, b, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
                boolean gaseous = f.getFluid().getFluidType().getDensity(f) <= 0;
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    GTUtil.spawnParticles(w, facing, ParticleTypes.CRIT, b, 3 + GTValues.RNG.nextInt(2));
                }
                GTUtil.spawnParticles(w, gaseous ? Direction.UP : Direction.DOWN, ParticleTypes.CRIT,
                        b, 6 + GTValues.RNG.nextInt(4));
                float scalar = (float) Math.log(f.getAmount());
                List<LivingEntity> entities = w.getEntitiesOfClass(LivingEntity.class,
                        new AABB(b).inflate(scalar * (gaseous ? 0.4 : 0.2)));
                for (LivingEntity entity : entities) {
                    EntityDamageUtil.applyChemicalDamage(entity, scalar * (gaseous ? 0.6f : 0.8f));
                }
                w.removeBlock(b, false);
            },
            (p, f) -> {
                Vec3 pos = p.getPosition(1.0f);
                p.level().playSound(null, pos.x, pos.y, pos.z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F,
                        1.0F);
                boolean gaseous = f.getFluid().getFluidType().getDensity(f) <= 0;
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    GTUtil.spawnParticles(p.level(), facing, ParticleTypes.CRIT, p,
                            3 + GTValues.RNG.nextInt(2));
                }
                GTUtil.spawnParticles(p.level(), gaseous ? Direction.UP : Direction.DOWN,
                        ParticleTypes.CRIT,
                        p, 6 + GTValues.RNG.nextInt(4));
                float scalar = (float) Math.log(f.getAmount());
                List<LivingEntity> entities = p.level().getEntitiesOfClass(LivingEntity.class,
                        new AABB(BlockPos.containing(p.getPosition(1.0f))).inflate(scalar * (gaseous ? 0.4 : 0.2)));
                for (LivingEntity entity : entities) {
                    if (entity == p) continue;
                    EntityDamageUtil.applyChemicalDamage(entity, scalar * (gaseous ? 0.6f : 0.8f));
                }
                EntityDamageUtil.applyChemicalDamage(p, scalar * (gaseous ? 3f : 4f));
            });

    private FluidAttributes() {}
}
