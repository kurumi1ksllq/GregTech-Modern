package com.gregtechceu.gtceu.api.recipe.gui;

import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ProgressWidget;
import brachy.modularui.widgets.layout.Flow;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.common.mui.GTMuiWidgets;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.DoubleSupplier;

/**
 * Holds UI information for recipe type ui layouts that can be drawn as a singleblock machine UI.
 */
public class SingleblockMachineUILayout {

    public final GTRecipeTypeUILayout recipeTypeUILayout;

    @Getter
    public Map<RecipeCapability<?>, Map<IO, BiFunction<MetaMachine, GTRecipeTypeUILayout, String[]>>> machineLayoutGridBuilders = new Object2ObjectOpenHashMap<>();

    @Getter
    public Map<RecipeCapability<?>, MachineCapabilityUILayoutBuilder> machineCapabilityLayoutBuilders = new Object2ObjectOpenHashMap<>();

    public SingleblockMachineUILayout(GTRecipeTypeUILayout layout) {
        recipeTypeUILayout = layout;
    }

    public ParentWidget<?> getBackedSlotsRow(@NotNull PanelSyncManager syncManager,
                                             @NotNull MetaMachine machine,
                                             DoubleSupplier progressSupplier) {
        Objects.requireNonNull(recipeTypeUILayout.getRecipeType());

        var recipeType = recipeTypeUILayout.getRecipeType();

        DoubleSyncValue progressPercent = syncManager.getOrCreateSyncHandler("progressPercent",
                DoubleSyncValue.class, () -> new DoubleSyncValue(progressSupplier));

        Flow inColumn = Flow.col().coverChildren();
        Flow outColumn = Flow.col().coverChildren();

        var backedSlotsRow = Flow.row()
                .coverChildren()
                .center()
                .childPadding((recipeTypeUILayout.getProgressSize() / 2) + 2)
                .child(inColumn)
                .child(new ProgressWidget()
                        .value(progressPercent)
                        .name("progressBar")
                        .texture(recipeTypeUILayout.getProgressBar(), recipeTypeUILayout.getProgressSize())
                        .size(recipeTypeUILayout.getProgressSize())
                        .direction(recipeTypeUILayout.getProgressDirection()))
                .child(outColumn);


        for (var recipeCap : recipeType.maxInputs.keySet()) {
            if (!machineCapabilityLayoutBuilders.containsKey(recipeCap)) continue;
            var ui = machineCapabilityLayoutBuilders.get(recipeCap).createCapabilityUILayout(machine, this, IO.IN);
            if (ui != null) inColumn.child(ui);
        }

        for (var recipeCap : recipeType.maxOutputs.keySet()) {
            if (!machineCapabilityLayoutBuilders.containsKey(recipeCap)) continue;
            var ui = machineCapabilityLayoutBuilders.get(recipeCap).createCapabilityUILayout(machine, this, IO.OUT);
            if (ui != null) outColumn.child(ui);
        }

        return backedSlotsRow;
    }

    public String[] getMachineGridLayout(RecipeCapability<?> cap, IO io, MetaMachine machine) {
        if (machineLayoutGridBuilders.computeIfAbsent(cap, $ -> new Object2ObjectOpenHashMap<>()).containsKey(io)) {
            return machineLayoutGridBuilders.get(cap).get(io).apply(machine, recipeTypeUILayout);
        }
        var slots = recipeTypeUILayout.getRecipeType().getMaxSlots(cap, io);
        return GTMuiWidgets.createGrid(slots, Math.min(3, slots), io.support(IO.OUT), 's');
    }

}
