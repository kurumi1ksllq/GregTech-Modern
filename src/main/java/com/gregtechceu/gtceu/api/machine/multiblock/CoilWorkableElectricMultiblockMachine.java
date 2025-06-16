package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.multiblock.error.CoilMatchingError;
import com.gregtechceu.gtceu.common.block.CoilBlock;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;

import lombok.Getter;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CoilWorkableElectricMultiblockMachine extends WorkableElectricMultiblockMachine {

    @Getter
    private ICoilType coilType = CoilBlock.CoilType.CUPRONICKEL;

    public CoilWorkableElectricMultiblockMachine(IMachineBlockEntity holder) {
        super(holder);
    }

    //////////////////////////////////////
    // *** Multiblock LifeCycle ***//
    //////////////////////////////////////
    @Override
    public void formStructure(String name) {
        super.formStructure(name);
        var cache = patternStates.get(name).getCache();
        ICoilType coilType = null;
        for (var entry : cache.long2ObjectEntrySet()) {
            var state = entry.getValue().getBlockState();
            if (state.getBlock() instanceof CoilBlock coil) {
                if (GTCEuAPI.HEATING_COILS.containsKey(coil.coilType)) {
                    if (coilType == null) coilType = coil.coilType;
                    else {
                        if (coilType != coil.coilType) {
                            patternStates.get(name).setError(
                                    new CoilMatchingError(BlockPos.of(entry.getLongKey()), coilType, coil.coilType));
                            invalidateStructure(name);
                            return;
                        }
                    }
                }
            }
        }
        if (coilType != null) {
            this.coilType = coilType;
        }
    }

    public int getCoilTier() {
        return coilType.getTier();
    }
}
