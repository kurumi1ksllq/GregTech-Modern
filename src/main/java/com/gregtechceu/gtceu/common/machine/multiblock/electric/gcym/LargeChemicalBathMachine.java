package com.gregtechceu.gtceu.common.machine.multiblock.electric.gcym;

import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.feature.multiblock.IFluidRenderMulti;
import com.gregtechceu.gtceu.api.machine.multiblock.WorkableElectricMultiblockMachine;
import com.gregtechceu.gtceu.api.multiblock.util.RelativeDirection;
import com.gregtechceu.gtceu.syncsystem.annotations.RerenderOnChanged;
import com.gregtechceu.gtceu.syncsystem.annotations.SyncToClient;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class LargeChemicalBathMachine extends WorkableElectricMultiblockMachine implements IFluidRenderMulti {

    @Getter
    @SyncToClient
    @RerenderOnChanged
    private @NotNull Set<BlockPos> fluidBlockOffsets = new HashSet<>();

    public LargeChemicalBathMachine(BlockEntityCreationInfo info) {
        super(info);
    }

    public void setFluidBlockOffsets(Set<BlockPos> offsets) {
        fluidBlockOffsets = offsets;
        syncDataHolder.markClientSyncFieldDirty("fluidBlockOffsets");
    }

    @Override
    public void formStructure(String name) {
        super.formStructure(name);
        IFluidRenderMulti.super.formStructure(name);
    }

    @Override
    public void invalidateStructure(String name) {
        super.invalidateStructure(name);
        IFluidRenderMulti.super.invalidateStructure(name);
    }

    @NotNull
    @Override
    public Set<BlockPos> saveOffsets() {
        Direction up = RelativeDirection.UP.getRelativeFacing(getFrontFacing(), getUpwardsFacing(), isFlipped());
        Direction back = getFrontFacing().getOpposite();
        Direction clockWise = RelativeDirection.RIGHT.getRelativeFacing(getFrontFacing(), getUpwardsFacing(),
                isFlipped());
        Direction counterClockWise = RelativeDirection.LEFT.getRelativeFacing(getFrontFacing(), getUpwardsFacing(),
                isFlipped());

        BlockPos pos = getBlockPos();
        BlockPos center = pos.relative(up);

        Set<BlockPos> offsets = new HashSet<>();

        for (int i = 0; i < 5; i++) {
            center = center.relative(back);
            offsets.add(center.subtract(pos));
            offsets.add(center.relative(clockWise).subtract(pos));
            offsets.add(center.relative(counterClockWise).subtract(pos));
        }
        return offsets;
    }
}
