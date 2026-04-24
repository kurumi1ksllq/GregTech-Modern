package com.gregtechceu.gtceu.common.mui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanel;
import com.gregtechceu.gtceu.api.machine.mui.MachineUIPanelBuilder;
import com.gregtechceu.gtceu.api.machine.steam.SimpleSteamMachine;
import com.gregtechceu.gtceu.api.mui.factory.PanelFactory;
import com.gregtechceu.gtceu.api.recipe.gui.GTRecipeTypeUIs;

import brachy.modularui.factory.PosGuiData;
import brachy.modularui.screen.ModularPanel;
import brachy.modularui.screen.UISettings;
import brachy.modularui.utils.Alignment;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.layout.Flow;

public class GTSingleblockMachinePanels {

    public static PanelFactory GENERAL_MACHINE = (PosGuiData data, PanelSyncManager syncManager, UISettings settings,
                                                  MetaMachine machine) -> {
        if (!(machine instanceof SimpleTieredMachine simpleTieredMachine)) {
            GTCEu.LOGGER.error("{} is not a WorkableTieredMachine, can not add slots to its content",
                    machine.getDefinition().getName());
            return new ModularPanel<>(machine.getDefinition().getName());
        }

        return MachineUIPanelBuilder.defaultSimpleSingleblockPanelBuilder(machine).mainContents((parent) -> {
            boolean hasXEI = GTRecipeTypeUIs.recipeTypeUIs.containsKey(simpleTieredMachine.getRecipeType());

            parent.child(Flow.row()
                    .coverChildren(MachineUIPanel.DEFAULT_CONTENT_WIDTH, MachineUIPanel.DEFAULT_CONTENT_HEIGHT)
                    .childIf(hasXEI, () -> GTRecipeTypeUIs.recipeTypeUIs.get(simpleTieredMachine.getRecipeType())
                            .getBackedSlotsRow(syncManager, simpleTieredMachine,
                                    simpleTieredMachine.recipeLogic::getProgressPercent)));

        }).build(syncManager, settings).excludeAreaInRecipeViewer();
    };

    public static PanelFactory STEAM_MACHINE = (PosGuiData data, PanelSyncManager syncManager, UISettings settings,
                                                MetaMachine machine) -> {
        if (!(machine instanceof SimpleSteamMachine steamMachine)) {
            GTCEu.LOGGER.error("{} is not a SimpleSteamMachine, can not add slots to its content",
                    machine.getDefinition().getName());
            return new ModularPanel<>(machine.getDefinition().getName());
        }

        return MachineUIPanelBuilder.defaultSteamMachineBuilder(machine).mainContents(parent -> {
            boolean hasXEI = GTRecipeTypeUIs.recipeTypeUIs.containsKey(steamMachine.getRecipeType());

            parent.child(Flow.row()
                    .coverChildren(MachineUIPanel.DEFAULT_CONTENT_WIDTH, MachineUIPanel.DEFAULT_CONTENT_HEIGHT)
                    .childIf(hasXEI, () -> GTRecipeTypeUIs.recipeTypeUIs.get(steamMachine.getRecipeType())
                            .getBackedSlotsRow(syncManager, steamMachine,
                                    steamMachine.recipeLogic::getProgressPercent)
                            .posRel(Alignment.Center)));
        }).build(syncManager, settings).excludeAreaInRecipeViewer();
    };
}
