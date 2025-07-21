package com.gregtechceu.gtceu.common.machine.electric;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IHazardParticleContainer;
import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IEnvironmentalHazardCleaner;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.common.capability.EnvironmentalHazardSavedData;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.data.machines.GTMachineUtils;
import com.gregtechceu.gtceu.common.network.GTNetwork;
import com.gregtechceu.gtceu.common.network.packets.hazard.SPacketRemoveHazardZone;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.data.recipe.builder.GTRecipeBuilder;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import it.unimi.dsi.fastutil.objects.Object2FloatLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2FloatSortedMap;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.stream.Collectors;

import static com.gregtechceu.gtceu.api.GTValues.*;

public class AirScrubberMachine extends SimpleTieredMachine
                                implements IEnvironmentalHazardCleaner, IHazardParticleContainer {

    public static final float MIN_CLEANING_PER_OPERATION = 10;

    private float cleaningPerOperation;

    @Getter
    private float removedLastSecond;
    private float maxRemovePerSecond;

    public AirScrubberMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, GTMachineUtils.largeTankSizeFunction, args);
        this.cleaningPerOperation = MIN_CLEANING_PER_OPERATION;
        this.maxRemovePerSecond = MIN_CLEANING_PER_OPERATION;
    }

    @Override
    public boolean regressWhenWaiting() {
        return false;
    }

    @Override
    public void cleanHazard(MedicalCondition condition, float amount) {
        if (this.recipeLogic.isActive()) {
            return;
        }

        GTRecipeBuilder builder = GTRecipeTypes.AIR_SCRUBBER_RECIPES.recipeBuilder(condition.name + "_autogen")
                .duration(200).EUt(VHA[LV]);
        condition.recipeModifier.accept(builder);
        this.recipeLogic.checkMatchedRecipeAvailable(builder.buildRawRecipe());
    }

    @Override
    public boolean isRecipeLogicAvailable() {
        // Don't run recipes if hazards are off
        return ConfigHolder.INSTANCE.gameplay.environmentalHazards;
    }

    @Override
    public boolean beforeWorking(@Nullable GTRecipe recipe) {
        if (super.beforeWorking(recipe) && recipe != null) {
            // Sets the amount of hazard to clean based on the recipe tier, not the machine tier
            this.cleaningPerOperation = MIN_CLEANING_PER_OPERATION * (recipe.ocLevel + 1);
            // value is the result of adding all values in relativePositions (in onWorking) together.
            float value = 0.76f * (float) Math.pow(tier, 2.62);
            this.maxRemovePerSecond = cleaningPerOperation * value;
            return true;
        }
        return false;
    }

    @Override
    public boolean onWorking() {
        if (!super.onWorking() || !ConfigHolder.INSTANCE.gameplay.environmentalHazards) {
            return false;
        }

        if (getOffsetTimer() % 20 == 0) {
            removedLastSecond = 0;

            for (Direction dir : GTUtil.DIRECTIONS) {
                BlockPos offset = getPos().relative(dir);
                if (GTCapabilityHelper.getHazardContainer(getLevel(), offset, dir.getOpposite()) != null) {
                    if (getLevel().getBlockEntity(offset) instanceof PipeBlockEntity duct &&
                            !duct.isConnected(dir.getOpposite())) {
                        continue;
                    }
                    return true;
                }
            }

            final ServerLevel serverLevel = (ServerLevel) getLevel();
            EnvironmentalHazardSavedData savedData = EnvironmentalHazardSavedData.getOrCreate(serverLevel);

            final ChunkPos pos = new ChunkPos(getPos());
            Object2FloatSortedMap<ChunkPos> relativePositions = new Object2FloatLinkedOpenHashMap<>();
            int radius = tier / 2;
            if (radius <= 0) {
                // LV scrubber can only process the chunk it's in
                relativePositions.put(pos, 1);
            } else {
                for (int x = -radius; x <= radius; ++x) {
                    for (int z = -radius; z <= radius; ++z) {
                        relativePositions.put(new ChunkPos(pos.x + x, pos.z + z), Mth.sqrt(Mth.abs(x * z)) + 1);
                    }
                }
                // sort positions to be lowest to highest distance so that the cleaning limit stops at the edges
                // instead of possibly the center
                relativePositions = relativePositions.object2FloatEntrySet().stream()
                        .sorted(Map.Entry.comparingByValue())
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                                Float::sum, Object2FloatLinkedOpenHashMap::new));
            }
            for (var entry : relativePositions.object2FloatEntrySet()) {
                final float distance = entry.getFloatValue();
                savedData.getHazardZones().compute(entry.getKey(), (chunkPos, zone) -> {
                    if (zone == null || zone.strength() <= 0) {
                        return null;
                    }

                    float toClean = cleaningPerOperation / distance;
                    if (removedLastSecond + toClean > maxRemovePerSecond) {
                        return zone;
                    }
                    removedLastSecond += toClean;
                    zone.removeStrength(toClean);
                    if (zone.strength() <= 0) {
                        if (serverLevel.hasChunk(chunkPos.x, chunkPos.z)) {
                            LevelChunk chunk = serverLevel.getChunk(chunkPos.x, chunkPos.z);
                            GTNetwork.sendToAllPlayersTrackingChunk(chunk, new SPacketRemoveHazardZone(chunkPos));
                        }
                        return null;
                    } else return zone;
                });
            }
        }
        return true;
    }

    @Override
    public boolean inputsHazard(Direction side, MedicalCondition condition) {
        return removedLastSecond < maxRemovePerSecond;
    }

    @Override
    public float changeHazard(MedicalCondition condition, float amount, boolean simulate) {
        if (removedLastSecond >= maxRemovePerSecond) {
            return 0;
        }
        float result = Math.min(amount, maxRemovePerSecond - removedLastSecond);
        if (!simulate) {
            cleanHazard(condition, result);
            removedLastSecond += result;
        }
        return result;
    }

    // Disallow emptying hazards from scrubbers
    @Override
    public float removeHazard(MedicalCondition condition, float particlesToRemove, boolean simulate) {
        return 0;
    }

    @Override
    public float getHazardStored(MedicalCondition condition) {
        return removedLastSecond;
    }

    @Override
    public float getHazardCapacity(MedicalCondition condition) {
        return maxRemovePerSecond;
    }
}
