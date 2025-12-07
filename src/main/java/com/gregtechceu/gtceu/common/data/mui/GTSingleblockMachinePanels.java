package com.gregtechceu.gtceu.common.data.mui;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.steam.SimpleSteamMachine;
import com.gregtechceu.gtceu.api.mui.drawable.UITexture;
import com.gregtechceu.gtceu.api.mui.factory.PanelFactory;
import com.gregtechceu.gtceu.api.mui.factory.PosGuiData;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.value.sync.PanelSyncManager;
import com.gregtechceu.gtceu.api.mui.widgets.SlotGroupWidget;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Column;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Row;
import com.gregtechceu.gtceu.api.recipe.gui.GTRecipeTypeUIs;
import com.gregtechceu.gtceu.client.mui.screen.ModularPanel;
import com.gregtechceu.gtceu.client.mui.screen.UISettings;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;

public class GTSingleblockMachinePanels {

    private static void logNotSimpleTieredMachine(MetaMachine machine) {
        GTCEu.LOGGER.error("{} is not a SimpleTieredMachine, can not add slots to its content",
                machine.getDefinition().getName());
    }

    private static void logNotSimpleSteamMachine(MetaMachine machine) {
        GTCEu.LOGGER.error("{} is not a SimpleSteamMachine, can not add slots to its content",
                machine.getDefinition().getName());
    }

    public static PanelFactory GENERAL_MACHINE = (PosGuiData data, PanelSyncManager syncManager, UISettings settings,
                                                  MetaMachine machine) -> {
        ModularPanel panel = new ModularPanel(machine.getDefinition().getName());
        if (!(machine instanceof SimpleTieredMachine simpleTieredMachine)) {
            logNotSimpleTieredMachine(machine);
            return panel;
        }

        var inputItemGrid = GTMuiWidgets.createGrid(simpleTieredMachine.importItems.getSize(), 3, false, 'i');
        var inputFluidGrid = GTMuiWidgets.createGrid(simpleTieredMachine.importFluids.getSize(), 3, false, 'f');
        var outputItemGrid = GTMuiWidgets.createGrid(simpleTieredMachine.exportItems.getSize(), 3, true, 'i');
        var outputFluidGrid = GTMuiWidgets.createGrid(simpleTieredMachine.exportFluids.getSize(), 3, true, 'f');

        int inputWidth = 18 *
                Math.min(3, Math.max(simpleTieredMachine.importItems.getSize(),
                        simpleTieredMachine.importFluids.getSize()));
        int outputWidth = 18 *
                Math.min(3, Math.max(simpleTieredMachine.exportItems.getSize(),
                        simpleTieredMachine.exportFluids.getSize()));

        int slotHeight = Math.max(inputItemGrid.length + inputFluidGrid.length,
                outputItemGrid.length + outputFluidGrid.length);

        int topMargin = 0;
        if (slotHeight == 2) {
            topMargin = 9;
        } else if (slotHeight > 2) {
            topMargin = 18;
        }

        // input slots + centering gap + output slots

        /*
         * 1 -> 1.5
         * 2 -> 1
         * 3 -> .5
         * 36 - (inputWidth / 2)
         * 
         * 1:1 -> 18 + 18 + 36
         * 1:2 -> 18 + 36 + 27
         * 1:3 -> 18 + 54 + 3
         * 2 - input + 2 - output
         */
        int fullWidth = (inputWidth + outputWidth) + (90 - ((inputWidth + outputWidth) / 2));

        simpleTieredMachine.hasAutoOutputItem();
        simpleTieredMachine.hasAutoOutputFluid();

        boolean ghostCircuit = simpleTieredMachine.isCircuitSlotEnabled();

        panel.size(176, 124 + Math.max(36, 18 * slotHeight));

        boolean hasXEI = GTRecipeTypeUIs.recipeTypeUIs.containsKey(simpleTieredMachine.getRecipeType());

        panel.child(GTMuiWidgets.createTitleBar(machine.getDefinition(), 176, GTGuiTextures.BACKGROUND))
                .child(new Row()
                        .coverChildrenHeight()
                        .width(fullWidth)
                        .left(7 + (36 - (inputWidth / 2)))
                        .child(new Column()
                                .coverChildrenWidth()
                                .mainAxisAlignment(Alignment.MainAxis.CENTER)
                                .childIf(!(inputItemGrid.length == 0),
                                        GTMuiMachineUtil.createSlotGroupFromInventory(simpleTieredMachine.importItems,
                                                "input_item_inv", simpleTieredMachine.importItems.getSize(), 'i',
                                                inputItemGrid)
                                                .alignX(Alignment.CenterLeft))
                                .childIf(!(inputFluidGrid.length == 0),
                                        GTMuiMachineUtil
                                                .createSlotGroupFromInventory(syncManager,
                                                        simpleTieredMachine.importFluids,
                                                        "input_fluid_inv", simpleTieredMachine.importFluids.getSize(),
                                                        'f',
                                                        inputFluidGrid)
                                                .alignX(Alignment.CenterLeft))
                                .align(Alignment.CenterLeft))
                        .child(new Column()
                                .coverChildrenWidth()
                                .mainAxisAlignment(Alignment.MainAxis.CENTER)
                                .childIf(!(outputItemGrid.length == 0),
                                        GTMuiMachineUtil.createSlotGroupFromInventory(simpleTieredMachine.exportItems,
                                                "output_item_inv", simpleTieredMachine.exportItems.getSize(), 'i',
                                                outputItemGrid)
                                                .alignX(Alignment.CenterRight))
                                .childIf(!(outputFluidGrid.length == 0),
                                        GTMuiMachineUtil
                                                .createSlotGroupFromInventory(syncManager,
                                                        simpleTieredMachine.exportFluids,
                                                        "output_fluid_inv", simpleTieredMachine.exportFluids.getSize(),
                                                        'f',
                                                        outputFluidGrid)
                                                .alignX(Alignment.CenterRight))
                                .align(Alignment.CenterRight))
                        .top(30 - topMargin))
                .child(GTMuiWidgets.createProgressBar(simpleTieredMachine, GTGuiTextures.PROGRESS_BAR_MACERATE, 16)
                        .alignX(Alignment.CENTER)
                        .top(30 + (slotHeight > 3 ? 9 : 0)))
                .child(SlotGroupWidget.playerInventory(false).left(7).bottom(7))
                .child(GTMuiWidgets.createRightSidePanel(simpleTieredMachine, syncManager))
                .child(GTMuiWidgets.createEmptySidePanel(true)
                        .bottom(16)
                        .childIf(ghostCircuit,
                                GTMuiWidgets.createCircuitSlotPanel(simpleTieredMachine, panel, syncManager)))
                .child(GTMuiWidgets.createGTLogo()
                        .right(7).bottom(7 + 78));
        if (hasXEI) {
            panel.child(
                    GTMuiWidgets.createXEIWidget(GTRecipeTypeUIs.recipeTypeUIs.get(simpleTieredMachine.getRecipeType()))
                            .left(190));
        }
        panel.excludeAreaInXei();
        return panel;
    };

    public static PanelFactory MACERATOR = (PosGuiData data, PanelSyncManager syncManager, UISettings settings,
                                            MetaMachine machine) -> {
        ModularPanel panel = new ModularPanel(machine.getDefinition().getName());
        if (!(machine instanceof SimpleTieredMachine simpleTieredMachine)) {
            logNotSimpleTieredMachine(machine);
            return panel;
        }

        int outputSlots = switch (simpleTieredMachine.getTier()) {
            case 0, 1, 2 -> 1;
            case 3 -> 3;
            default -> 4;
        };

        String[] outputGrid;
        if (simpleTieredMachine.getTier() > 3) {
            outputGrid = new String[] { "ii", "ii" };
        } else if (simpleTieredMachine.getTier() == 3) {
            outputGrid = new String[] { "iii" };
        } else {
            outputGrid = new String[] { "i" };
        }
        boolean ghostCircuit = simpleTieredMachine.isCircuitSlotEnabled();
        simpleTieredMachine.hasAutoOutputItem();

        int inputWidth = 18 *
                Math.min(3, Math.max(simpleTieredMachine.importItems.getSize(),
                        simpleTieredMachine.importFluids.getSize()));
        int outputWidth = 18 * outputGrid[0].length();

        int fullWidth = (inputWidth + outputWidth) + (90 - ((inputWidth + outputWidth) / 2));

        panel.child(GTMuiWidgets.createTitleBar(machine.getDefinition(), 176, GTGuiTextures.BACKGROUND))
                .child(new Row()
                        .coverChildrenHeight()
                        .width(fullWidth)
                        .left(7 + (36 - (inputWidth / 2)))
                        .child(GTMuiMachineUtil.createSlotGroupFromInventory(simpleTieredMachine.importItems,
                                "input_inv", 1, 'i', "i")
                                .align(Alignment.CenterLeft))
                        .child(GTMuiMachineUtil.createSlotGroupFromInventory(simpleTieredMachine.exportItems,
                                "output_inv", outputSlots, 'i', outputGrid)
                                .align(Alignment.CenterRight))
                        .top(30))
                .child(GTMuiWidgets.createProgressBar(simpleTieredMachine, GTGuiTextures.PROGRESS_BAR_MACERATE, 16)
                        .alignX(Alignment.CENTER)
                        .top(30))
                .child(SlotGroupWidget.playerInventory(false).left(7).bottom(7))
                .child(GTMuiWidgets.createRightSidePanel(simpleTieredMachine, syncManager))
                .child(GTMuiWidgets.createEmptySidePanel(true)
                        .bottom(16)
                        .childIf(ghostCircuit,
                                GTMuiWidgets.createCircuitSlotPanel(simpleTieredMachine, panel, syncManager)))
                .child(GTMuiWidgets.createGTLogo()
                        .right(7).bottom(7 + 78));

        return panel;
    };

    public static PanelFactory ARC_FURNACE = (PosGuiData data, PanelSyncManager syncManager, UISettings settings,
                                              MetaMachine machine) -> {
        ModularPanel panel = new ModularPanel(machine.getDefinition().getName());
        if (!(machine instanceof SimpleTieredMachine simpleTieredMachine)) {
            logNotSimpleTieredMachine(machine);
            return panel;
        }

        int outputSlots = 4;

        String[] outputGrid = new String[] { "ii", "ii" };
        boolean ghostCircuit = simpleTieredMachine.isCircuitSlotEnabled();

        int inputWidth = 18;
        int outputWidth = 18 * 2;

        int fullWidth = (inputWidth + outputWidth) + (90 - ((inputWidth + outputWidth) / 2));

        panel.size(176, 169);

        panel.child(GTMuiWidgets.createTitleBar(machine.getDefinition(), 186, GTGuiTextures.BACKGROUND))
                .child(new Row()
                        .coverChildrenHeight()
                        .width(fullWidth)
                        .left(7 + (36 - (inputWidth / 2)))
                        .child(new Column()
                                .coverChildrenWidth()
                                .mainAxisAlignment(Alignment.MainAxis.CENTER)
                                .child(GTMuiMachineUtil.createSlotGroupFromInventory(simpleTieredMachine.importItems,
                                        "input_inv", 1, 'i', "i")
                                        .alignX(Alignment.CenterLeft))
                                .child(GTMuiMachineUtil
                                        .createSlotGroupFromInventory(syncManager, simpleTieredMachine.importFluids,
                                                "input_inv", 1, 'f', "f")
                                        .alignX(Alignment.CenterLeft))
                                .align(Alignment.CenterLeft))
                        .child(GTMuiMachineUtil.createSlotGroupFromInventory(simpleTieredMachine.exportItems,
                                "output_inv", outputSlots, 'i', outputGrid)
                                .align(Alignment.CenterRight))
                        .top(21))
                .child(GTMuiWidgets.createProgressBar(simpleTieredMachine, GTGuiTextures.PROGRESS_BAR_ARC_FURNACE, 16)
                        .alignX(Alignment.CENTER)
                        .top(30))
                .child(SlotGroupWidget.playerInventory(false).left(7).bottom(7))
                .child(GTMuiWidgets.createRightSidePanel(simpleTieredMachine, syncManager))
                .child(GTMuiWidgets.createEmptySidePanel(true)
                        .bottom(16)
                        .childIf(ghostCircuit,
                                GTMuiWidgets.createCircuitSlotPanel(simpleTieredMachine, panel, syncManager)))
                .child(GTMuiWidgets.createGTLogo()
                        .right(7).bottom(7 + 78));

        return panel;
    };

    public static PanelFactory STEAM_MACHINE = (PosGuiData data, PanelSyncManager syncManager, UISettings settings,
                                                MetaMachine machine) -> {
        ModularPanel panel = new ModularPanel(machine.getDefinition().getName());
        if (!(machine instanceof SimpleSteamMachine steamMachine)) {
            logNotSimpleSteamMachine(machine);
            return panel;
        }

        UITexture background = steamMachine.isHighPressure() ? GTGuiTextures.BACKGROUND_STEEL :
                GTGuiTextures.BACKGROUND_BRONZE;

        panel.background(background);
        // panel.widgetTheme(GTGuiTheme.BRONZE.getId());

        var inputItemGrid = GTMuiWidgets.createGrid(steamMachine.importItems.getSize(), 3, false, 'i');
        var outputItemGrid = GTMuiWidgets.createGrid(steamMachine.exportItems.getSize(), 3, true, 'i');

        int inputWidth = 18 * Math.min(3, steamMachine.importItems.getSize());
        int outputWidth = 18 * Math.min(3, steamMachine.exportItems.getSize());

        int slotHeight = Math.max(inputItemGrid.length, outputItemGrid.length);

        int topMargin = 0;
        if (slotHeight == 2) {
            topMargin = 9;
        } else if (slotHeight > 2) {
            topMargin = 18;
        }

        // input slots + centering gap + output slots

        /*
         * 1 -> 1.5
         * 2 -> 1
         * 3 -> .5
         * 36 - (inputWidth / 2)
         * 
         * 1:1 -> 18 + 18 + 36
         * 1:2 -> 18 + 36 + 27
         * 1:3 -> 18 + 54 + 3
         * 2 - input + 2 - output
         */
        int fullWidth = (inputWidth + outputWidth) + (90 - ((inputWidth + outputWidth) / 2));

        panel.size(176, 124 + Math.max(36, 18 * slotHeight));

        panel.child(GTMuiWidgets.createTitleBar(machine.getDefinition(), 176, background))
                .child(new Row()
                        .coverChildrenHeight()
                        .width(fullWidth)
                        .left(7 + (36 - (inputWidth / 2)))
                        .child(new Column()
                                .coverChildrenWidth()
                                .mainAxisAlignment(Alignment.MainAxis.CENTER)
                                .childIf(!(inputItemGrid.length == 0),
                                        GTMuiMachineUtil.createSlotGroupFromInventory(steamMachine.importItems,
                                                "input_item_inv", steamMachine.importItems.getSize(), 'i',
                                                inputItemGrid)
                                                .alignX(Alignment.CenterLeft))
                                .align(Alignment.CenterLeft))
                        .child(new Column()
                                .coverChildrenWidth()
                                .mainAxisAlignment(Alignment.MainAxis.CENTER)
                                .childIf(!(outputItemGrid.length == 0),
                                        GTMuiMachineUtil.createSlotGroupFromInventory(steamMachine.exportItems,
                                                "output_item_inv", steamMachine.exportItems.getSize(), 'i',
                                                outputItemGrid)
                                                .alignX(Alignment.CenterRight))
                                .align(Alignment.CenterRight))
                        .top(30 - topMargin))
                .child(GTMuiWidgets.createProgressBar(steamMachine, GTGuiTextures.PROGRESS_BAR_MACERATE, 16)
                        .alignX(Alignment.CENTER)
                        .top(30 + (slotHeight > 3 ? 9 : 0)))
                .child(SlotGroupWidget.playerInventory(false).left(7).bottom(7))
                .child(new Column()
                        .coverChildren()
                        .leftRel(1.0f)
                        .reverseLayout(true)
                        .bottom(16)
                        .padding(0, 8, 4, 4)
                        .childPadding(2)
                        .background(background.getSubArea(0.25f, 0f, 1.0f, 1.0f))
                        .child(GTMuiWidgets.createPowerButton(steamMachine, syncManager)))
                .child(new Column()
                        .coverChildren()
                        .rightRel(1.0f)
                        .reverseLayout(true)
                        .padding(0, 8, 4, 4)
                        .bottom(16)
        // .background(GTGuiTextures.BACKGROUND.getSubArea(0f, 0f, 0.75f, 1.0f)))
        // todo steam overlay
        )
                .child(GTMuiWidgets.createGTLogo()
                        .right(7).bottom(7 + 78));

        return panel;
    };

    public static PanelFactory STEAM_MACERATOR = (PosGuiData data, PanelSyncManager syncManager, UISettings settings,
                                                  MetaMachine machine) -> {
        ModularPanel panel = new ModularPanel(machine.getDefinition().getName());
        if (!(machine instanceof SimpleSteamMachine steamMachine)) {
            logNotSimpleSteamMachine(machine);
            return panel;
        }

        UITexture background = steamMachine.isHighPressure() ? GTGuiTextures.BACKGROUND_STEEL :
                GTGuiTextures.BACKGROUND_BRONZE;

        panel.background(background);
        // panel.widgetTheme(GTGuiTheme.BRONZE.getId());

        var inputItemGrid = GTMuiWidgets.createGrid(steamMachine.importItems.getSize(), 3, false, 'i');
        var outputItemGrid = GTMuiWidgets.createGrid(1, 3, true, 'i');

        int inputWidth = 18 * Math.min(3, steamMachine.importItems.getSize());
        int outputWidth = 18 * Math.min(3, steamMachine.exportItems.getSize());

        int slotHeight = Math.max(inputItemGrid.length, outputItemGrid.length);

        int topMargin = 0;
        if (slotHeight == 2) {
            topMargin = 9;
        } else if (slotHeight > 2) {
            topMargin = 18;
        }

        // input slots + centering gap + output slots

        /*
         * 1 -> 1.5
         * 2 -> 1
         * 3 -> .5
         * 36 - (inputWidth / 2)
         * 
         * 1:1 -> 18 + 18 + 36
         * 1:2 -> 18 + 36 + 27
         * 1:3 -> 18 + 54 + 3
         * 2 - input + 2 - output
         */
        int fullWidth = (inputWidth + outputWidth) + (90 - ((inputWidth + outputWidth) / 2));

        panel.size(176, 124 + Math.max(36, 18 * slotHeight));

        panel.child(GTMuiWidgets.createTitleBar(machine.getDefinition(), 176, background))
                .child(new Row()
                        .coverChildrenHeight()
                        .width(fullWidth)
                        .left(7 + (36 - (inputWidth / 2)))
                        .child(new Column()
                                .coverChildrenWidth()
                                .mainAxisAlignment(Alignment.MainAxis.CENTER)
                                .childIf(!(inputItemGrid.length == 0),
                                        GTMuiMachineUtil.createSlotGroupFromInventory(steamMachine.importItems,
                                                "input_item_inv", steamMachine.importItems.getSize(), 'i',
                                                inputItemGrid)
                                                .alignX(Alignment.CenterLeft))
                                .align(Alignment.CenterLeft))
                        .child(new Column()
                                .coverChildrenWidth()
                                .mainAxisAlignment(Alignment.MainAxis.CENTER)
                                .childIf(!(outputItemGrid.length == 0),
                                        GTMuiMachineUtil.createSlotGroupFromInventory(steamMachine.exportItems,
                                                "output_item_inv", steamMachine.exportItems.getSize(), 'i',
                                                outputItemGrid)
                                                .alignX(Alignment.CenterRight))
                                .align(Alignment.CenterRight))
                        .top(30 - topMargin))
                .child(GTMuiWidgets.createProgressBar(steamMachine, GTGuiTextures.PROGRESS_BAR_MACERATE, 16)
                        .alignX(Alignment.CENTER)
                        .top(30 + (slotHeight > 3 ? 9 : 0)))
                .child(SlotGroupWidget.playerInventory(false).left(7).bottom(7))
                .child(new Column()
                        .coverChildren()
                        .leftRel(1.0f)
                        .reverseLayout(true)
                        .bottom(16)
                        .padding(0, 8, 4, 4)
                        .childPadding(2)
                        .background(background.getSubArea(0.25f, 0f, 1.0f, 1.0f))
                        .child(GTMuiWidgets.createPowerButton(steamMachine, syncManager)))
                .child(new Column()
                        .coverChildren()
                        .rightRel(1.0f)
                        .reverseLayout(true)
                        .padding(0, 8, 4, 4)
                        .bottom(16)
        // .background(GTGuiTextures.BACKGROUND.getSubArea(0f, 0f, 0.75f, 1.0f)))
        // todo steam overlay
        )
                .child(GTMuiWidgets.createGTLogo()
                        .right(7).bottom(7 + 78));

        return panel;
    };
}
