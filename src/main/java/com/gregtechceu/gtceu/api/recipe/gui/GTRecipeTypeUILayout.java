package com.gregtechceu.gtceu.api.recipe.gui;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.widgets.ProgressWidget;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Holds UI information for a recipe type UI.
 */
public class GTRecipeTypeUILayout {

    @Setter
    @Getter
    private GTRecipeType recipeType;
    @Getter
    private UITexture progressBar;
    @Getter
    private int progressSize;
    @Getter
    private ProgressWidget.Direction progressDirection;
    @Getter
    private Map<IO, Map<RecipeCapability<?>, Int2ObjectOpenHashMap<IDrawable>>> overlays = new EnumMap<>(IO.class);
    @Getter
    private final SingleblockMachineUILayout singleblockMachineUILayout = new SingleblockMachineUILayout(this);

    public IDrawable getOverlay(IO io, RecipeCapability<?> cap, int index) {
        return getOverlays()
                .computeIfAbsent(io, $ -> new Object2ObjectOpenHashMap<>())
                .computeIfAbsent(cap, $ -> new Int2ObjectOpenHashMap<>())
                .getOrDefault(index, IDrawable.EMPTY);
    }

    public static class Builder {

        private UITexture progressBar;
        private int progressSize;
        private ProgressWidget.Direction fillDirection;
        private final Map<IO, Map<RecipeCapability<?>, Int2ObjectOpenHashMap<IDrawable>>> overlays = new EnumMap<>(
                IO.class);
        private final Map<RecipeCapability<?>, MachineCapabilityUILayoutBuilder> capLayoutBuilders = new Object2ObjectOpenHashMap<>();
        private final Map<RecipeCapability<?>, Map<IO, BiFunction<MetaMachine, GTRecipeTypeUILayout, String[]>>> machineLayoutGridBuilders = new Object2ObjectOpenHashMap<>();

        public Builder() {
            capLayoutBuilders.put(ItemRecipeCapability.CAP, MachineCapabilityUILayoutBuilder.ITEM);
            capLayoutBuilders.put(FluidRecipeCapability.CAP, MachineCapabilityUILayoutBuilder.FLUID);
        }

        public Builder setSlotOverlay(IO ioMode, int slotIndex, RecipeCapability<?> cap, IDrawable overlay) {
            overlays.computeIfAbsent(ioMode, it -> new Object2ReferenceOpenHashMap<>())
                    .computeIfAbsent(cap, it -> new Int2ObjectOpenHashMap<>())
                    .put(slotIndex, overlay);
            return this;
        }

        public Builder setItemSlotOverlay(IO ioMode, int slotIndex, IDrawable overlay) {
            return setSlotOverlay(ioMode, slotIndex, ItemRecipeCapability.CAP, overlay);
        }

        public Builder setFluidSlotOverlay(IO ioMode, int slotIndex, IDrawable overlay) {
            return setSlotOverlay(ioMode, slotIndex, FluidRecipeCapability.CAP, overlay);
        }

        public Builder setItemSlotsOverlay(IO ioMode, int slotIndexStart, int slotIndexEnd, IDrawable overlay) {
            for (int i = slotIndexStart; i <= slotIndexEnd; i++) {
                setSlotOverlay(ioMode, i, ItemRecipeCapability.CAP, overlay);
            }
            return this;
        }

        public Builder setFluidSlotsOverlay(IO ioMode, int slotIndexStart, int slotIndexEnd, IDrawable overlay) {
            for (int i = slotIndexStart; i <= slotIndexEnd; i++) {
                setSlotOverlay(ioMode, i, FluidRecipeCapability.CAP, overlay);
            }
            return this;
        }

        public Builder setProgressBar(UITexture progressBar, int progressSize) {
            return setProgressBar(progressBar, progressSize, ProgressWidget.Direction.RIGHT);
        }

        public Builder setProgressBar(UITexture progressBar, int progressSize, ProgressWidget.Direction fillDirection) {
            this.progressBar = progressBar;
            this.progressSize = progressSize;
            this.fillDirection = fillDirection;
            return this;
        }

        public Builder setMachineCapabilityLayoutBuilder(RecipeCapability<?> cap, MachineCapabilityUILayoutBuilder builder) {
            capLayoutBuilders.put(cap, builder);
            return this;
        }

        public Builder setMachineLayoutGridBuilder(RecipeCapability<?> cap, IO io, BiFunction<MetaMachine, GTRecipeTypeUILayout, String[]> gridBuilder) {
            machineLayoutGridBuilders.computeIfAbsent(cap, $ -> new Object2ObjectOpenHashMap<>()).put(io, gridBuilder);
            return this;
        }

        public GTRecipeTypeUILayout build() {
            GTRecipeTypeUILayout layout = new GTRecipeTypeUILayout();
            layout.progressBar = progressBar;
            layout.progressSize = progressSize;
            layout.progressDirection = fillDirection;
            layout.overlays = overlays;
            layout.singleblockMachineUILayout.machineCapabilityLayoutBuilders = capLayoutBuilders;
            layout.singleblockMachineUILayout.machineLayoutGridBuilders = machineLayoutGridBuilders;
            return layout;
        }
    }
}
