package com.gregtechceu.gtceu.api.recipe.gui;

import brachy.modularui.api.GuiAxis;
import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widgets.ProgressWidget;
import brachy.modularui.widgets.layout.Flow;
import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import java.util.function.DoubleSupplier;

/**
 * The UI for singleblock recipe machines.
 */
public class GTRecipeTypeMachineWidget extends Flow {

    public final Flow inputColumn = Flow.col().coverChildren();
    public final Flow outputColumn = Flow.col().coverChildren();

    public GTRecipeTypeMachineWidget(GTRecipeType recipeType, PanelSyncManager syncManager,
                                     MetaMachine machine,
                                     DoubleSupplier progressSupplier) {
        super(GuiAxis.X);

        if (recipeType.getUiLayout() == null) {
            GTCEu.LOGGER.error(
                    "Tried to draw a singleblock recipe type UI for {}, but it does not have a recipe type UI",
                    machine.getDefinition().getName());
            return;
        }

        var layout = recipeType.getUiLayout();

        DoubleSyncValue progressPercent = syncManager.getOrCreateSyncHandler("progressPercent",
                DoubleSyncValue.class, () -> new DoubleSyncValue(progressSupplier));

        coverChildren();
        center();
        childPadding((layout.progressSize / 2) + 2);
        child(inputColumn);
        child(new ProgressWidget()
                        .value(progressPercent)
                        .name("progressBar")
                        .texture(layout.progressBar, layout.progressSize)
                        .size(layout.progressSize)
                        .direction(layout.progressDirection));

        child(outputColumn);

        for (var cap: recipeType.capabilities) {
            var layoutFunc = layout.capabilityInfo(cap).machineLayoutBuilder;
            if (layoutFunc == null) continue;
            if (recipeType.getMaxInputs(cap) != 0) layoutFunc.createCapabilityUILayout(machine, layout, this, IO.IN);
            if (recipeType.getMaxOutputs(cap) != 0) layoutFunc.createCapabilityUILayout(machine, layout, this, IO.OUT);
        }
    }
}
