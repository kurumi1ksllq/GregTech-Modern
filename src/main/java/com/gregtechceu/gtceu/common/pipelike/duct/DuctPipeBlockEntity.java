package com.gregtechceu.gtceu.common.pipelike.duct;

import com.gregtechceu.gtceu.api.pipenet.PipeBlockEntity;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IHazardParticleContainer;
import com.gregtechceu.gtceu.api.data.medicalcondition.MedicalCondition;
import com.gregtechceu.gtceu.api.machine.feature.IEnvironmentalHazardCleaner;
import com.gregtechceu.gtceu.api.machine.feature.IEnvironmentalHazardEmitter;
import com.gregtechceu.gtceu.common.pipelike.GTPipeNetworks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class DuctPipeBlockEntity extends PipeBlockEntity<DuctPipeType, DuctPipeProperties> {

    // the DuctNetHandler can only be created on the server, so we have an empty placeholder for the client
    public final IHazardParticleContainer clientCapability = new DefaultDuctContainer();

    public DuctPipeBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState blockState) {
        super(type, GTPipeNetworks.DUCT, pos, blockState);
    }

    @Override
    public boolean canHaveBlockedFaces() {
        return false;
    }

    public boolean canAttachTo(Direction side) {
        if (level != null) {
            if (level.getBlockEntity(getBlockPos().relative(side)) instanceof DuctPipeBlockEntity) {
                return false;
            }
            BlockPos relative = getBlockPos().relative(side);
            return GTCapabilityHelper.getHazardContainer(level, relative, side.getOpposite()) !=
                    null ||
                    (level.getBlockEntity(relative) instanceof IEnvironmentalHazardCleaner ||
                            level.getBlockEntity(relative) instanceof IEnvironmentalHazardEmitter);
        }
        return false;
    }

    private static class DefaultDuctContainer implements IHazardParticleContainer {

        @Override
        public boolean inputsHazard(Direction side, MedicalCondition condition) {
            return false;
        }

        @Override
        public float changeHazard(MedicalCondition condition, float differenceAmount) {
            return 0;
        }

        @Override
        public float getHazardStored(MedicalCondition condition) {
            return 0;
        }

        @Override
        public float getHazardCapacity(MedicalCondition condition) {
            return 0;
        }
    }
}
