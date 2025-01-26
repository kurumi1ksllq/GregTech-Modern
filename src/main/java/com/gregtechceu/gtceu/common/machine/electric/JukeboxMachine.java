package com.gregtechceu.gtceu.common.machine.electric;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;

// TODO: Copy Lang
// TODO: Better tooltips
// TODO: Actual textures
public class JukeboxMachine extends TieredEnergyMachine {
    public JukeboxMachine(IMachineBlockEntity holder, int tier, Object... args) {
        super(holder, tier, args);
    }



    public long calculateEut() {
        if (this.tier <= GTValues.ULV) {
            return 2;
        } else {
            return GTValues.V[this.tier] / 16;
        }
    }
}
