package com.gregtechceu.gtceu.api.fluids;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.data.recipe.CustomTags;
import com.gregtechceu.gtceu.utils.EntityDamageUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fluids.FluidStack;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public enum FluidState implements ContainmentFailureHandler {

    LIQUID("gtceu.fluid.state_liquid", CustomTags.LIQUID_FLUIDS),
    GAS("gtceu.fluid.state_gas", Tags.Fluids.GASEOUS),
    PLASMA("gtceu.fluid.state_plasma", CustomTags.PLASMA_FLUIDS),
    ;

    @Getter
    private final String translationKey;
    @Getter
    private final TagKey<Fluid> tagKey;

    FluidState(@NotNull String translationKey, @NotNull TagKey<Fluid> tagKey) {
        this.translationKey = translationKey;
        this.tagKey = tagKey;
    }

    public static FluidState inferState(FluidStack stack) {
        if (stack.getFluid() instanceof GTFluid fluid) return fluid.getState();
        else return stack.getFluid().getFluidType().isLighterThanAir() ? GAS : LIQUID;
    }

    @Override
    public void handleFailure(Level world, BlockPos failingBlock, net.minecraftforge.fluids.FluidStack failingStack) {
        world.playSound(null, failingBlock, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F, 1.0F);
        switch (this) {
            default -> {
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    int particles = GTValues.RNG.nextInt(5);
                    if (particles != 0) {
                        GTUtil.spawnParticles(world, facing, ParticleTypes.DRIPPING_WATER, failingBlock, particles);
                    }
                }
                float scalar = (float) Math.log(failingStack.getAmount());
                List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                        new AABB(failingBlock).inflate(scalar * 0.5));
                for (LivingEntity entity : entities) {
                    EntityDamageUtil.applyTemperatureDamage(entity,
                            failingStack.getFluid().getFluidType().getTemperature(failingStack),
                            scalar, 20);
                }
                world.removeBlock(failingBlock, false);
            }
            case GAS -> {
                GTUtil.spawnParticles(world, Direction.UP, ParticleTypes.LARGE_SMOKE, failingBlock,
                        9 + GTValues.RNG.nextInt(3));
                float scalar = (float) Math.log(failingStack.getAmount());
                List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                        new AABB(failingBlock).inflate(scalar));
                for (LivingEntity entity : entities) {
                    EntityDamageUtil.applyTemperatureDamage(entity,
                            failingStack.getFluid().getFluidType().getTemperature(failingStack),
                            scalar * 0.75f, 15);
                }
                world.removeBlock(failingBlock, false);
            }
            case PLASMA -> {
                GTUtil.spawnParticles(world, Direction.UP, ParticleTypes.LARGE_SMOKE, failingBlock,
                        3 + GTValues.RNG.nextInt(3));
                float scalar = (float) Math.log(failingStack.getAmount());
                List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                        new AABB(failingBlock).inflate(scalar * 1.5));
                for (LivingEntity entity : entities) {
                    EntityDamageUtil.applyTemperatureDamage(entity,
                            failingStack.getFluid().getFluidType().getTemperature(failingStack),
                            scalar, 30);
                }
                world.removeBlock(failingBlock, false);
                world.explode(null, failingBlock.getX() + 0.5, failingBlock.getY() + 0.5,
                        failingBlock.getZ() + 0.5,
                        1.0f + GTValues.RNG.nextFloat(), Level.ExplosionInteraction.BLOCK);
            }
        }
    }

    @Override
    public void handleFailure(Player failingPlayer, net.minecraftforge.fluids.FluidStack failingStack) {
        Level world = failingPlayer.level();
        Vec3 pos = failingPlayer.getPosition(1.0f);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LAVA_EXTINGUISH, SoundSource.BLOCKS, 1.0F,
                1.0F);
        switch (this) {
            default -> {
                for (Direction facing : Direction.Plane.HORIZONTAL) {
                    int particles = GTValues.RNG.nextInt(5);
                    if (particles != 0) {
                        GTUtil.spawnParticles(world, facing, ParticleTypes.DRIPPING_WATER, failingPlayer, particles);
                    }
                }
                float scalar = (float) Math.log(failingStack.getAmount());
                List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                        new AABB(BlockPos.containing(failingPlayer.getPosition(1.0f))).inflate(scalar * 0.5));
                for (LivingEntity entity : entities) {
                    if (entity == failingPlayer) continue;
                    EntityDamageUtil.applyTemperatureDamage(entity,
                            failingStack.getFluid().getFluidType().getTemperature(failingStack),
                            scalar, 20);
                }
                EntityDamageUtil.applyTemperatureDamage(failingPlayer,
                        failingStack.getFluid().getFluidType().getTemperature(failingStack),
                        scalar * 3, 60);
            }
            case GAS -> {
                GTUtil.spawnParticles(world, Direction.UP, ParticleTypes.LARGE_SMOKE, failingPlayer,
                        9 + GTValues.RNG.nextInt(3));
                float scalar = (float) Math.log(failingStack.getAmount());
                List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                        new AABB(BlockPos.containing(failingPlayer.getPosition(1.0f))).inflate(scalar));
                for (LivingEntity entity : entities) {
                    if (entity == failingPlayer) continue;
                    EntityDamageUtil.applyTemperatureDamage(entity,
                            failingStack.getFluid().getFluidType().getTemperature(failingStack),
                            scalar * 0.75f, 15);
                }
                EntityDamageUtil.applyTemperatureDamage(failingPlayer,
                        failingStack.getFluid().getFluidType().getTemperature(failingStack),
                        scalar * 2.25f, 45);
            }
            case PLASMA -> {
                GTUtil.spawnParticles(world, Direction.UP, ParticleTypes.LARGE_SMOKE, failingPlayer,
                        3 + GTValues.RNG.nextInt(3));
                float scalar = (float) Math.log(failingStack.getAmount());
                List<LivingEntity> entities = world.getEntitiesOfClass(LivingEntity.class,
                        new AABB(BlockPos.containing(failingPlayer.getPosition(1.0f))).inflate(scalar * 1.5));
                for (LivingEntity entity : entities) {
                    if (entity == failingPlayer) continue;
                    EntityDamageUtil.applyTemperatureDamage(entity,
                            failingStack.getFluid().getFluidType().getTemperature(failingStack),
                            scalar, 30);
                }
                EntityDamageUtil.applyTemperatureDamage(failingPlayer,
                        failingStack.getFluid().getFluidType().getTemperature(failingStack),
                        scalar * 3, 90);
                Vec3 vec = failingPlayer.getEyePosition(1);
                world.explode(null, vec.x, vec.y, vec.z,
                        1.0f + GTValues.RNG.nextFloat(), Level.ExplosionInteraction.BLOCK);
            }
        }
    }
}
