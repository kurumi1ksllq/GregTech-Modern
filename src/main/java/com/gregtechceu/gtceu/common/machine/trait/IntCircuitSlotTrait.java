package com.gregtechceu.gtceu.common.machine.trait;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.ItemRecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;
import com.gregtechceu.gtceu.api.machine.multiblock.part.MultiblockPartMachine;
import com.gregtechceu.gtceu.api.machine.trait.*;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.sync_system.annotations.SaveField;
import com.gregtechceu.gtceu.api.sync_system.annotations.SyncToClient;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class IntCircuitSlotTrait extends NotifiableRecipeHandlerTrait<Ingredient>
                                 implements ICapabilityTrait {

    public static final MachineTraitType<IntCircuitSlotTrait> TYPE = new MachineTraitType<>(IntCircuitSlotTrait.class);

    @Getter
    @SaveField
    @SyncToClient
    private final CustomItemStackHandler storage;
    @Getter
    @Setter
    private boolean circuitSlotEnabled;

    public IntCircuitSlotTrait(MetaMachine machine) {
        super(machine);
        this.storage = new CustomItemStackHandler(1);
        circuitSlotEnabled = true;
        storage.setFilter(IntCircuitBehaviour::isIntegratedCircuit);
    }

    @Override
    public MachineTraitType<IntCircuitSlotTrait> getTraitType() {
        return TYPE;
    }

    public int getCurrentCircuit() {
        return IntCircuitBehaviour.getCircuitConfiguration(storage.getStackInSlot(0));
    }

    public void setCurrentCircuit(int circuit) {
        storage.setStackInSlot(0, IntCircuitBehaviour.stack(circuit));
    }

    public void addedToController(MultiblockControllerMachine controller) {
        if (!controller.allowCircuitSlots()) {
            if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
                storage.dropInventoryInWorld(getLevel(), getBlockPos());
            } else {
                storage.setStackInSlot(0, ItemStack.EMPTY);
            }
            setCircuitSlotEnabled(false);
        }
    }

    public void removedFromController(MultiblockControllerMachine controller) {
        var controllers = ((MultiblockPartMachine) machine).getControllers();
        for (var c : controllers) {
            if (!c.allowCircuitSlots()) {
                return;
            }
        }
        setCircuitSlotEnabled(true);
    }

    @Override
    public void onMachineDestroyed() {
        if (!ConfigHolder.INSTANCE.machines.ghostCircuit) {
            storage.dropInventoryInWorld(getLevel(), getBlockPos());
        }
    }

    @Override
    public IO getHandlerIO() {
        return isCircuitSlotEnabled() ? IO.IN : IO.NONE;
    }

    @Override
    public boolean shouldSearchContent() {
        return false;
    }

    @Override
    public List<Ingredient> handleRecipeInner(IO io, GTRecipe recipe, List<Ingredient> left, boolean simulate) {
        return NotifiableItemStackHandler.handleRecipe(io, recipe, left, simulate, getHandlerIO(), storage);
    }

    @Override
    public List<Object> getContents() {
        return List.of(storage.getStackInSlot(0));
    }

    @Override
    public double getTotalContentAmount() {
        return 1;
    }

    @Override
    public RecipeCapability<Ingredient> getCapability() {
        return ItemRecipeCapability.CAP;
    }

    @Override
    public IO getCapabilityIO() {
        return IO.NONE;
    }
}
