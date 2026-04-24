package com.gregtechceu.gtceu.api.recipe.gui;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableRecipeHandlerTrait;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.DoubleSupplier;

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
    private Map<IO, Map<RecipeCapability<?>, Int2IntArrayMap>> gridLength = new EnumMap<>(IO.class);
    @Getter
    private Map<IO, Map<RecipeCapability<?>, Int2IntArrayMap>> gridWidths = new EnumMap<>(IO.class);

    public GTRecipeTypeUILayout() {}

    @SuppressWarnings("unchecked")
    public ParentWidget<?> getBackedSlotsRow(@NotNull PanelSyncManager syncManager,
                                             @NotNull MetaMachine machine,
                                             DoubleSupplier progressSupplier) {
        Objects.requireNonNull(recipeType);

        Map<IO, Map<RecipeCapability<?>, NotifiableRecipeHandlerTrait<?>>> handlers = new Object2ObjectOpenHashMap<>();
        handlers.put(IO.IN, new Object2ObjectOpenHashMap<>());
        handlers.put(IO.OUT, new Object2ObjectOpenHashMap<>());

        for (var capability: recipeType.maxInputs.keySet()) {
            if (!capability.shouldCreateCapabilityUI()) continue;

            var traitType = capability.getNotifiableHandlerTraitType();
            List<NotifiableRecipeHandlerTrait<?>> capHandlers = (List<NotifiableRecipeHandlerTrait<?>>)machine.getTraitHolder().getTraits(traitType);

            var ioIn = handlers.get(IO.IN);
            for (var capHandler: capHandlers) {
                if (capHandler.getHandlerIO() == IO.IN) ioIn.putIfAbsent(capability, capHandler);
            }
        }
        for (var capability: recipeType.maxOutputs.keySet()) {
            if (!capability.shouldCreateCapabilityUI()) continue;

            var traitType = capability.getNotifiableHandlerTraitType();
            List<NotifiableRecipeHandlerTrait<?>> capHandlers = (List<NotifiableRecipeHandlerTrait<?>>)machine.getTraitHolder().getTraits(traitType);

            var ioOut = handlers.get(IO.OUT);
            for (var capHandler: capHandlers) {
                if (capHandler.getHandlerIO() == IO.OUT) ioOut.putIfAbsent(capability, capHandler);
            }
        }

        DoubleSyncValue progressPercent = syncManager.getOrCreateSyncHandler("progressPercent",
                DoubleSyncValue.class, () -> new DoubleSyncValue(progressSupplier));

        var backedSlotsRow = Flow.row()
                .coverChildren()
                .horizontalCenter()
                .childPadding((progressSize / 2) + 2)
                .childIf(!handlers.get(IO.IN).isEmpty(), () -> getIOColumn(syncManager, machine, IO.IN, handlers.get(IO.IN)))
                .child(new ProgressWidget()
                        .value(progressPercent)
                        .name("progressBar")
                        .texture(progressBar, progressSize)
                        .size(progressSize)
                        .direction(progressDirection))
                .childIf(!handlers.get(IO.OUT).isEmpty(), () -> getIOColumn(syncManager, machine, IO.OUT, handlers.get(IO.OUT)));

        return new ParentWidget<>()
                .widthRel(1f)
                .coverChildrenHeight()
                .child(backedSlotsRow);
    }

    public Flow getIOColumn(@NotNull PanelSyncManager syncManager,
                             MetaMachine machine,
                             IO io,
                             Map<RecipeCapability<?>, NotifiableRecipeHandlerTrait<?>> handlers) {
        boolean in = io == IO.IN;

        var caps = (in ? recipeType.maxInputs : recipeType.maxOutputs);

        Flow ioColumn = Flow.col().coverChildren();


        for (var recipeCap : caps.keySet()) {
            var ui = recipeCap.createCapabilityUI(machine, syncManager, handlers.get(recipeCap), this, caps.getInt(recipeCap), io);
            if (ui != null) ioColumn.child(ui);
        }

        return ioColumn;
    }

    public String[] createGrid(IO io, RecipeCapability<?> cap, char key, int tier, int maxMachineSlots) {
        int maxWidth = 3;
        if (gridWidths.containsKey(io) && gridWidths.get(io).containsKey(cap)) {
            int width = gridWidths.get(io).get(cap).get(tier);
            if (width != 0) maxWidth = width;
        }
        int maxSlots = (io == IO.IN ? recipeType.getMaxInputs(cap) : recipeType.getMaxOutputs(cap));
        if (gridLength.containsKey(io) && gridLength.get(io).containsKey(cap)) {
            int length = gridLength.get(io).get(cap).get(tier);
            if (length != 0) maxSlots = length;
        }
        maxSlots = Math.min(maxMachineSlots, maxSlots);
        maxWidth = Math.min(maxSlots, maxWidth);
        return GTMuiWidgets.createGrid(maxSlots, maxWidth, io.support(IO.OUT), key);
    }

    public static class Builder {

        private UITexture progressBar;
        private int progressSize;
        private ProgressWidget.Direction fillDirection;
        private final Map<IO, Map<RecipeCapability<?>, Int2ObjectOpenHashMap<IDrawable>>> overlays = new EnumMap<>(
                IO.class);
        private final Map<IO, Map<RecipeCapability<?>, Int2IntArrayMap>> gridLength = new EnumMap<>(IO.class);
        private final Map<IO, Map<RecipeCapability<?>, Int2IntArrayMap>> gridWidths = new EnumMap<>(IO.class);

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

        public Builder setIOSlotLength(IO ioMode, RecipeCapability<?> cap, int tier, int value) {
            gridLength.computeIfAbsent(ioMode, it -> new Object2ReferenceOpenHashMap<>())
                    .computeIfAbsent(cap, it -> new Int2IntArrayMap())
                    .put(tier, value);
            return this;
        }

        public Builder setIOSlotLengths(IO ioMode, RecipeCapability<?> cap, int startTier, int endTier, int value) {
            for (int i = startTier; i <= endTier; i++) {
                gridLength.computeIfAbsent(ioMode, it -> new Object2ReferenceOpenHashMap<>())
                        .computeIfAbsent(cap, it -> new Int2IntArrayMap())
                        .put(i, value);
            }
            return this;
        }

        public Builder setIOSlotWidth(IO ioMode, RecipeCapability<?> cap, int tier, int value) {
            gridWidths.computeIfAbsent(ioMode, it -> new Object2ReferenceOpenHashMap<>())
                    .computeIfAbsent(cap, it -> new Int2IntArrayMap())
                    .put(tier, value);
            return this;
        }

        public Builder setIOSlotWidths(IO ioMode, RecipeCapability<?> cap, int startTier, int endTier, int value) {
            for (int i = startTier; i <= endTier; i++) {
                gridWidths.computeIfAbsent(ioMode, it -> new Object2ReferenceOpenHashMap<>())
                        .computeIfAbsent(cap, it -> new Int2IntArrayMap())
                        .put(i, value);
            }
            return this;
        }

        public GTRecipeTypeUILayout build() {
            GTRecipeTypeUILayout layout = new GTRecipeTypeUILayout();
            layout.progressBar = progressBar;
            layout.progressSize = progressSize;
            layout.progressDirection = fillDirection;
            layout.overlays = overlays;
            layout.gridLength = gridLength;
            layout.gridWidths = gridWidths;
            return layout;
        }
    }
}
