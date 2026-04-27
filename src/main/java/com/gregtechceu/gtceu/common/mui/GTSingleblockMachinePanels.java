package com.gregtechceu.gtceu.common.mui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder;
import com.gregtechceu.gtceu.api.machine.steam.SimpleSteamMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gregtechceu.gtceu.api.mui.factory.PanelFactory;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.api.recipe.gui.GTRecipeTypeUIs;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.value.sync.PanelSyncManager;

public class GTSingleblockMachinePanels {

    public static PanelFactory GENERAL_MACHINE = (PosGuiData data, PanelSyncManager syncManager, UISettings settings,
                                                  MetaMachine machine) -> {

        GTRecipeType type;
        RecipeLogic recipeLogic;
        if (machine instanceof SimpleTieredMachine simpleTieredMachine) {
            type = simpleTieredMachine.getRecipeType();
            recipeLogic = simpleTieredMachine.getRecipeLogic();
        } else if (machine instanceof SimpleSteamMachine simpleSteamMachine) {
            type = simpleSteamMachine.getRecipeType();
            recipeLogic = simpleSteamMachine.recipeLogic;
        } else {
            GTCEu.LOGGER.error("{} is not a SimpleTieredMachine or SimpleSteamMachine, cannot add slots to its content",
                    machine.getDefinition().getName());
            return new ModularPanel<>(machine.getDefinition().getName());
        }

        return MachineUIPanelBuilder.defaultSimpleSingleblockPanelBuilder(machine).mainContents((parent) -> {
            if (!GTRecipeTypeUIs.recipeTypeUIs.containsKey(type))return;

            parent.child(GTRecipeTypeUIs.recipeTypeUIs.get(type)
                    .getSingleblockMachineUILayout().getBackedSlotsRow(syncManager, machine,
                                    recipeLogic::getProgressPercent));

        }).build(syncManager, settings).excludeAreaInRecipeViewer();
    };
}
