package com.gregtechceu.gtceu.api.recipe.gui;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.mui.GTMuiWidgets;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ProgressWidget;
import brachy.modularui.widgets.layout.Flow;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.DoubleSupplier;

/**
 * Holds UI information for a recipe type UI.
 */
@Getter
public class GTRecipeTypeUILayout {

    @Setter
    private GTRecipeType recipeType;
    private UITexture progressBar;
    private int progressSize;
    private ProgressWidget.Direction progressDirection;
    private Map<IO, Map<RecipeCapability<?>, Int2ObjectOpenHashMap<IDrawable>>> overlays = new EnumMap<>(IO.class);

    private Map<RecipeCapability<?>, Map<IO, BiFunction<MetaMachine, GTRecipeTypeUILayout, String[]>>> machineLayoutGridBuilders = new Object2ObjectOpenHashMap<>();

    private Map<RecipeCapability<?>, MachineCapabilityLayoutBuilder> machineCapabilityLayoutBuilders = new Object2ObjectOpenHashMap<>();

    private Map<RecipeCapability<?>, RecipeViewerCapabilityLayoutBuilder> recipeViewerCapLayoutBuilders = new Object2ObjectOpenHashMap<>();
    private Map<RecipeCapability<?>, Map<IO, BiFunction<GTRecipe, GTRecipeTypeUILayout, String[]>>> recipeViewerLayoutGridBuilders = new Object2ObjectOpenHashMap<>();

    public IDrawable getOverlay(IO io, RecipeCapability<?> cap, int index) {
        return getOverlays()
                .computeIfAbsent(io, $ -> new Object2ObjectOpenHashMap<>())
                .computeIfAbsent(cap, $ -> new Int2ObjectOpenHashMap<>())
                .getOrDefault(index, IDrawable.EMPTY);
    }

    /**
     * Gets the recipe type UI for a singleblock machine.
     */
    public ParentWidget<?> getBackedSlotsRow(@NotNull PanelSyncManager syncManager,
                                             @NotNull MetaMachine machine,
                                             DoubleSupplier progressSupplier) {
        Objects.requireNonNull(getRecipeType());

        DoubleSyncValue progressPercent = syncManager.getOrCreateSyncHandler("progressPercent",
                DoubleSyncValue.class, () -> new DoubleSyncValue(progressSupplier));

        Flow inColumn = Flow.col().coverChildren();
        Flow outColumn = Flow.col().coverChildren();

        var backedSlotsRow = Flow.row()
                .coverChildren()
                .center()
                .childPadding((progressSize / 2) + 2)
                .child(inColumn)
                .child(new ProgressWidget()
                        .value(progressPercent)
                        .name("progressBar")
                        .texture(progressBar, progressSize)
                        .size(progressSize)
                        .direction(progressDirection))
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
            return machineLayoutGridBuilders.get(cap).get(io).apply(machine, this);
        }
        var slots = getRecipeType().getMaxSlots(cap, io);
        return GTMuiWidgets.createGrid(slots, Math.min(3, slots), io.support(IO.OUT), 's');
    }

    public String[] getRecipeViewerGridLayout(RecipeCapability<?> cap, IO io, GTRecipe recipe) {
        if (recipeViewerLayoutGridBuilders.computeIfAbsent(cap, $ -> new Object2ObjectOpenHashMap<>())
                .containsKey(io)) {
            return recipeViewerLayoutGridBuilders.get(cap).get(io).apply(recipe, this);
        }

        return GTMuiWidgets.createGrid(getRecipeType().getMaxSlots(cap, io),
                Math.min(3, getRecipeType().getMaxSlots(cap, io)), io.support(IO.OUT), 's');
    }

    public static class Builder {

        private UITexture progressBar;
        private int progressSize;
        private ProgressWidget.Direction fillDirection;
        private final Map<IO, Map<RecipeCapability<?>, Int2ObjectOpenHashMap<IDrawable>>> overlays = new EnumMap<>(
                IO.class);
        private final Map<RecipeCapability<?>, MachineCapabilityLayoutBuilder> machineCapLayoutBuilders = new Object2ObjectOpenHashMap<>();
        private final Map<RecipeCapability<?>, Map<IO, BiFunction<MetaMachine, GTRecipeTypeUILayout, String[]>>> machineLayoutGridBuilders = new Object2ObjectOpenHashMap<>();

        private final Map<RecipeCapability<?>, RecipeViewerCapabilityLayoutBuilder> recipeViewerCapLayoutBuilders = new Object2ObjectOpenHashMap<>();
        private final Map<RecipeCapability<?>, Map<IO, BiFunction<GTRecipe, GTRecipeTypeUILayout, String[]>>> recipeViewerLayoutGridBuilders = new Object2ObjectOpenHashMap<>();

        public Builder() {
            machineCapLayoutBuilders.put(ItemRecipeCapability.CAP, MachineCapabilityLayoutBuilder.ITEM);
            machineCapLayoutBuilders.put(FluidRecipeCapability.CAP, MachineCapabilityLayoutBuilder.FLUID);
        }

        /**
         * Adds a slot overlay for a specific slot
         * 
         * @param ioMode    The IO of the slot
         * @param slotIndex The index of the slot.
         * @param cap       The slot capability
         * @param overlay   The slot overlay.
         */
        public Builder setSlotOverlay(IO ioMode, int slotIndex, RecipeCapability<?> cap, IDrawable overlay) {
            overlays.computeIfAbsent(ioMode, it -> new Object2ReferenceOpenHashMap<>())
                    .computeIfAbsent(cap, it -> new Int2ObjectOpenHashMap<>())
                    .put(slotIndex, overlay);
            return this;
        }

        /**
         * Adds a slot overlay for an item slot
         * 
         * @param ioMode    The IO of the slot
         * @param slotIndex The index of the slot.
         * @param overlay   The slot overlay.
         */
        public Builder setItemSlotOverlay(IO ioMode, int slotIndex, IDrawable overlay) {
            return setSlotOverlay(ioMode, slotIndex, ItemRecipeCapability.CAP, overlay);
        }

        /**
         * Adds a slot overlay for a fluid slot
         * 
         * @param ioMode    The IO of the slot
         * @param slotIndex The index of the slot.
         * @param overlay   The slot overlay.
         */
        public Builder setFluidSlotOverlay(IO ioMode, int slotIndex, IDrawable overlay) {
            return setSlotOverlay(ioMode, slotIndex, FluidRecipeCapability.CAP, overlay);
        }

        /**
         * Adds a slot overlay for multiple item slots.
         * 
         * @param ioMode         The IO of the slot
         * @param slotIndexStart The first item slot to add the overlay to.
         * @param slotIndexEnd   The last item slot to add the overlay to.
         * @param overlay        The slot overlay.
         */
        public Builder setItemSlotsOverlay(IO ioMode, int slotIndexStart, int slotIndexEnd, IDrawable overlay) {
            for (int i = slotIndexStart; i <= slotIndexEnd; i++) {
                setSlotOverlay(ioMode, i, ItemRecipeCapability.CAP, overlay);
            }
            return this;
        }

        /**
         * Adds a slot overlay for multiple fluid slots.
         * 
         * @param ioMode         The IO of the slot
         * @param slotIndexStart The first fluid slot to add the overlay to.
         * @param slotIndexEnd   The last fluid slot to add the overlay to.
         * @param overlay        The slot overlay.
         */
        public Builder setFluidSlotsOverlay(IO ioMode, int slotIndexStart, int slotIndexEnd, IDrawable overlay) {
            for (int i = slotIndexStart; i <= slotIndexEnd; i++) {
                setSlotOverlay(ioMode, i, FluidRecipeCapability.CAP, overlay);
            }
            return this;
        }

        /**
         * Sets the texture and size of the progress bar
         * 
         * @param progressBar  Progress bar texture
         * @param progressSize Progress bar size
         */
        public Builder setProgressBar(UITexture progressBar, int progressSize) {
            return setProgressBar(progressBar, progressSize, ProgressWidget.Direction.RIGHT);
        }

        /**
         * Sets the texture, size and fill direction of the progress bar
         * 
         * @param progressBar   Progress bar texture
         * @param progressSize  Progress bar size
         * @param fillDirection Progress bar fill direction
         */
        public Builder setProgressBar(UITexture progressBar, int progressSize, ProgressWidget.Direction fillDirection) {
            this.progressBar = progressBar;
            this.progressSize = progressSize;
            this.fillDirection = fillDirection;
            return this;
        }

        /**
         * For singleblock machines using this recipe type, sets a function that builds the ui for a specific capability
         * type.
         * 
         * @param cap     The capability type
         * @param builder UI builder.
         */
        public Builder setMachineCapabilityLayoutBuilder(RecipeCapability<?> cap,
                                                         MachineCapabilityLayoutBuilder builder) {
            machineCapLayoutBuilders.put(cap, builder);
            return this;
        }

        /**
         * For singleblock machines using this recipe type, sets a function that builds the slot grid layout.
         * 
         * @param cap         The capability type
         * @param gridBuilder Function that returns a {@code String[]}, where 's' should be used to denote a slot.
         */
        public Builder setMachineLayoutGridBuilder(RecipeCapability<?> cap, IO io,
                                                   BiFunction<MetaMachine, GTRecipeTypeUILayout, String[]> gridBuilder) {
            machineLayoutGridBuilders.computeIfAbsent(cap, $ -> new Object2ObjectOpenHashMap<>()).put(io, gridBuilder);
            return this;
        }

        /**
         * Loads a recipe type UI from a file.
         * 
         * @param fileName Filename
         */
        public Builder loadRecipeTypeUIFromFile(String fileName) {
            throw new NotImplementedException();
        }

        /**
         * For the recipe viewer UI, sets a function that builds the ui for a specific capability type.
         * 
         * @param cap     The capability type
         * @param builder UI builder.
         */
        public Builder setRecipeViewerLayoutCapabilityLayoutBuilder(RecipeCapability<?> cap,
                                                                    RecipeViewerCapabilityLayoutBuilder builder) {
            recipeViewerCapLayoutBuilders.put(cap, builder);
            return this;
        }

        /**
         * For the recipe viewer UI, sets a function that builds the slot grid layout.
         * 
         * @param cap         The capability type
         * @param gridBuilder Function that returns a {@code String[]}, where 's' should be used to denote a slot.
         */
        public Builder setRecipeViewerLayoutGridBuilder(RecipeCapability<?> cap, IO io,
                                                        BiFunction<GTRecipe, GTRecipeTypeUILayout, String[]> gridBuilder) {
            recipeViewerLayoutGridBuilders.computeIfAbsent(cap, $ -> new Object2ObjectOpenHashMap<>()).put(io,
                    gridBuilder);
            return this;
        }

        public GTRecipeTypeUILayout build() {
            GTRecipeTypeUILayout layout = new GTRecipeTypeUILayout();
            layout.progressBar = progressBar;
            layout.progressSize = progressSize;
            layout.progressDirection = fillDirection;
            layout.overlays = overlays;
            layout.recipeViewerCapLayoutBuilders = recipeViewerCapLayoutBuilders;
            layout.recipeViewerLayoutGridBuilders = recipeViewerLayoutGridBuilders;
            layout.machineCapabilityLayoutBuilders = machineCapLayoutBuilders;
            layout.machineLayoutGridBuilders = machineLayoutGridBuilders;
            return layout;
        }
    }
}
