package com.gregtechceu.gtceu.api.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.machine.trait.AutoOutputTrait;
import com.gregtechceu.gtceu.api.machine.trait.ItemChargerSlotTrait;
import com.gregtechceu.gtceu.api.machine.trait.multiblock.IntCircuitSlotTrait;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import lombok.Getter;

import java.util.*;

/**
 * All simple single machines are implemented here.
 */
public class SimpleTieredMachine extends WorkableTieredMachine {

    public SimpleTieredMachine(BlockEntityCreationInfo info, int tier, Int2IntFunction tankScalingFunction) {
        super(info, tier, tankScalingFunction);

        registerPersistentTrait("autoOutput", new AutoOutputTrait(this, List.of(exportItems), List.of(exportFluids)));
        registerPersistentTrait("chargerInventory", new ItemChargerSlotTrait(this, energyContainer));
        registerPersistentTrait("circuitInventory", new IntCircuitSlotTrait(this));
    }

    /////////////////////////////////////
    // ****** RECIPE LOGIC *******//
    /////////////////////////////////////

    @Override
    public long getDisplayRecipeVoltage() {
        return GTValues.V[this.tier];
    }
}
