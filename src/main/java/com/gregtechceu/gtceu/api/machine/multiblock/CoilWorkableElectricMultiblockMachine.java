package com.gregtechceu.gtceu.api.machine.multiblock;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.block.ICoilType;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.multiblock.error.PatternStringError;
import com.gregtechceu.gtceu.common.block.CoilBlock;

import net.minecraft.MethodsReturnNonnullByDefault;

import lombok.Getter;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * @author KilaBash
 * @date 2023/3/4
 * @implNote PyrolyseOvenMachine
 */
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
        var cache = getSubstructure(name).getCache();
        ICoilType coilType = null;
        for(var entry : cache.long2ObjectEntrySet()) {
            var state = entry.getValue().getBlockState();
            if(state.getBlock() instanceof CoilBlock coil) {
                if(GTCEuAPI.HEATING_COILS.containsKey(coil.coilType)) {
                    if(coilType == null) coilType = coil.coilType;
                    else {
                        if(coilType != coil.coilType) {
                            invalidateStructure();
                            patternStates.get(name).setError(new PatternStringError("gtceu.coils.mismatch"));
                            return;
                        }
                    }
                }
            }
        }
        if(coilType != null) {
            this.coilType = coilType;
        }

        /*var type = getMultiblockState().getMatchContext().get("CoilType");
        if (type instanceof ICoilType coil) {
            this.coilType = coil;
        }*/
    }

    public int getCoilTier() {
        return coilType.getTier();
    }
}
