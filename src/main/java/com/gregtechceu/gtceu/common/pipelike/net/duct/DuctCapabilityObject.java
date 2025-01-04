package com.gregtechceu.gtceu.common.pipelike.net.duct;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.IHazardParticleContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.api.data.chemical.material.properties.HazardProperty;
import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.graphnet.group.NetGroup;
import com.gregtechceu.gtceu.api.graphnet.group.PathCacheGroupData;
import com.gregtechceu.gtceu.api.graphnet.net.NetNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.WorldPipeNode;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.IPipeCapabilityObject;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeCapabilityWrapper;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IEnvironmentalHazardCleaner;
import com.gregtechceu.gtceu.common.capability.EnvironmentalHazardSavedData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DuctCapabilityObject implements IPipeCapabilityObject, IHazardParticleContainer {

    public static final int ACTIVE_KEY = 155;

    private @Nullable PipeBlockEntity blockEntity;

    private final @NotNull WorldPipeNode node;

    private boolean transferring = false;

    public DuctCapabilityObject(@NotNull WorldPipeNode node) {
        this.node = node;
    }

    @Override
    public void init(@NotNull PipeBlockEntity tile, @NotNull PipeCapabilityWrapper wrapper) {
        this.blockEntity = tile;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return GTCapability.CAPABILITY_HAZARD_CONTAINER.orEmpty(cap, LazyOptional.of(() -> this));
    }

    private boolean inputDisallowed(Direction side) {
        if (side == null) return false;
        if (blockEntity == null) return true;
        else return blockEntity.isBlocked(side);
    }

    @Override
    public boolean inputsHazard(Direction side, MedicalCondition condition) {
        return !inputDisallowed(side);
    }

    @Override
    public float changeHazard(MedicalCondition condition, float differenceAmount) {
        if (blockEntity == null || this.transferring) return 0;
        NetGroup group = node.getGroupSafe();
        if (!(group.getData() instanceof DuctGroupData data)) return 0;

        this.transferring = true;

        PathCacheGroupData.SecondaryCache cache = data.getOrCreate(node);
        List<DuctPath> paths = new ObjectArrayList<>(group.getNodesUnderKey(ACTIVE_KEY).size());
        for (NetNode dest : group.getNodesUnderKey(ACTIVE_KEY)) {
            DuctPath path = (DuctPath) cache.getOrCompute(dest);
            if (path == null) continue;
            // construct the path list in order of ascending weight
            int i = 0;
            while (i < paths.size()) {
                if (paths.get(i).getWeight() >= path.getWeight()) break;
                else i++;
            }
            paths.add(i, path);
        }
        float total = differenceAmount;
        for (DuctPath path : paths) {
            NetNode target = path.getTargetNode();
            if (!(target instanceof WorldPipeNode n)) continue;
            for (var capability : n.getBlockEntity().getTargetsWithCapabilities(n).entrySet()) {
                IHazardParticleContainer handler = capability.getValue()
                        .getCapability(GTCapability.CAPABILITY_HAZARD_CONTAINER, capability.getKey().getOpposite())
                        .resolve()
                        .orElse(null);
                if (handler == null) {
                    if (n.getBlockEntity() instanceof IMachineBlockEntity machineBE &&
                            machineBE.getMetaMachine() instanceof IEnvironmentalHazardCleaner cleaner) {
                        cleaner.cleanHazard(condition, differenceAmount);
                        break;
                    }

                    var savedData = EnvironmentalHazardSavedData.getOrCreate(n.getNet().getLevel());
                    savedData.addZone(n.getEquivalencyData().relative(capability.getKey()),
                            differenceAmount, true, HazardProperty.HazardTrigger.INHALATION, condition);
                    total += differenceAmount;
                    emitPollutionParticles(n.getNet().getLevel(), n.getEquivalencyData(), capability.getKey());
                    break;
                }
                float change = handler.changeHazard(condition, differenceAmount);
                differenceAmount -= change;
                total += change;
                if (differenceAmount <= 0) {
                    break;
                }
            }
        }
        return total;
    }

    @Override
    public float getHazardStored(MedicalCondition condition) {
        return 0;
    }

    @Override
    public float getHazardCapacity(MedicalCondition condition) {
        return Float.MAX_VALUE;
    }

    public static void emitPollutionParticles(ServerLevel level, BlockPos pos, Direction frontFacing) {
        float xPos = frontFacing.getStepX() * 0.76F + pos.getX() + 0.25F;
        float yPos = frontFacing.getStepY() * 0.76F + pos.getY() + 0.25F;
        float zPos = frontFacing.getStepZ() * 0.76F + pos.getZ() + 0.25F;

        float ySpd = frontFacing.getStepY() * 0.1F + 0.2F + 0.1F * GTValues.RNG.nextFloat();
        float xSpd;
        float zSpd;

        if (frontFacing.getStepY() == -1) {
            float temp = GTValues.RNG.nextFloat() * 2 * (float) Math.PI;
            xSpd = (float) Math.sin(temp) * 0.1F;
            zSpd = (float) Math.cos(temp) * 0.1F;
        } else {
            xSpd = frontFacing.getStepX() * (0.1F + 0.2F * GTValues.RNG.nextFloat());
            zSpd = frontFacing.getStepZ() * (0.1F + 0.2F * GTValues.RNG.nextFloat());
        }
        level.sendParticles(ParticleTypes.LARGE_SMOKE,
                xPos + GTValues.RNG.nextFloat() * 0.5F,
                yPos + GTValues.RNG.nextFloat() * 0.5F,
                zPos + GTValues.RNG.nextFloat() * 0.5F,
                1,
                xSpd, ySpd, zSpd,
                0.1);
    }
}
