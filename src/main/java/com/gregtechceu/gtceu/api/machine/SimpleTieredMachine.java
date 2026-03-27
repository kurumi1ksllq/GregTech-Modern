package com.gregtechceu.gtceu.api.machine;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.blockentity.BlockEntityCreationInfo;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.trait.AutoOutputTrait;
import com.gregtechceu.gtceu.api.machine.trait.ItemChargerSlotTrait;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;

import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import lombok.Getter;

import java.util.*;
import java.util.function.*;

/**
 * All simple single machines are implemented here.
 */
public class SimpleTieredMachine extends WorkableTieredMachine
                                 implements IHasCircuitSlot {

    @Getter
    @SaveField
    protected final ItemChargerSlotTrait chargerInventory;
    @Getter
    @SaveField
    protected final NotifiableItemStackHandler circuitInventory;
    @SaveField
    @SyncToClient
    public final AutoOutputTrait autoOutput;

    public SimpleTieredMachine(BlockEntityCreationInfo info, int tier, Int2IntFunction tankScalingFunction) {
        super(info, tier, tankScalingFunction);

        this.autoOutput = new AutoOutputTrait(this, List.of(exportItems), List.of(exportFluids));
        this.chargerInventory = new ItemChargerSlotTrait(this, energyContainer);

        this.circuitInventory = new NotifiableItemStackHandler(this, 1, IO.IN, IO.NONE)
                .setFilter(IntCircuitBehaviour::isIntegratedCircuit);
    }

    //////////////////////////////////////
    // ********** MISC ***********//
    //////////////////////////////////////
    @Override
    public void onMachineDestroyed() {
        super.onMachineDestroyed();
        if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
            circuitInventory.dropInventoryInWorld();
        }
    }

    /////////////////////////////////////
    // ****** RECIPE LOGIC *******//
    /////////////////////////////////////

    @Override
    public long getDisplayRecipeVoltage() {
        return GTValues.V[this.tier];
    }
}
