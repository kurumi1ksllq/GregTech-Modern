package com.gregtechceu.gtceu.api.recipe.gui;

import com.gregtechceu.gtceu.api.capability.recipe.FluidRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.steam.SimpleSteamMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;

import brachy.modularui.api.widget.IWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.slot.FluidSlot;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.SlotGroup;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the UI for a specific capability in a simple singleblock machine ui
 */
@FunctionalInterface
public interface MachineCapabilityLayoutBuilder {

    /**
     * Builds and attaches the UI for a specific capability in a simple singleblock machine ui.
     *
     * @param machine The singleblock machine, will be either a {@link SimpleTieredMachine} or
     *                {@link SimpleSteamMachine}.
     * @param layout  The {@link GTRecipeTypeUILayout} which holds UI layout data.
     * @param io      The IO mode widgets are being created for.
     */
    @Nullable
    IWidget createCapabilityUILayout(MetaMachine machine, GTRecipeTypeUILayout layout, IO io);

    MachineCapabilityLayoutBuilder ITEM = (machine, layout, io) -> {

        NotifiableItemStackHandler itemHandler = ItemRecipeCapability.CAP.getCapabilityHandler(machine, io);
        if (itemHandler == null || layout.getRecipeType().getMaxSlots(ItemRecipeCapability.CAP, io) == 0) return null;

        var slotGroup = new SlotGroup(ItemRecipeCapability.CAP.name + "_" + io.name(), 3);

        return SlotGroupWidget
                .builder()
                .matrix(layout.getMachineGridLayout(ItemRecipeCapability.CAP, io, machine))
                .key('s', i -> new ItemSlot()
                        .slot(new ModularSlot(itemHandler, i)
                                .slotGroup(slotGroup)
                                .accessibility(io == IO.IN, true))
                        .backgroundOverlay(layout.getOverlay(io, ItemRecipeCapability.CAP, i)))
                .build()
                .coverChildren();
    };

    MachineCapabilityLayoutBuilder FLUID = (machine, layout, io) -> {

        NotifiableFluidTank fluidTank = FluidRecipeCapability.CAP.getCapabilityHandler(machine, io);
        if (fluidTank == null || layout.getRecipeType().getMaxSlots(FluidRecipeCapability.CAP, io) == 0) return null;

        return SlotGroupWidget.builder()
                .matrix(layout.getMachineGridLayout(FluidRecipeCapability.CAP, io, machine))
                .key('s', i -> new FluidSlot()
                        .tank(fluidTank.getStorages()[i])
                        .backgroundOverlay(layout.getOverlay(io, FluidRecipeCapability.CAP, i)))
                .build()
                .coverChildren();
    };
}
