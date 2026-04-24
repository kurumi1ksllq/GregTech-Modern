package com.gregtechceu.gtceu.api.recipe.gui;

import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableFluidTank;
import com.gregtechceu.gtceu.api.machine.trait.NotifiableItemStackHandler;
import com.gregtechceu.gtceu.api.recipe.GTRecipeType;
import com.gregtechceu.gtceu.common.mui.GTMuiWidgets;

import brachy.modularui.api.drawable.IDrawable;
import brachy.modularui.drawable.UITexture;
import brachy.modularui.theme.ThemeAPI;
import brachy.modularui.value.sync.DoubleSyncValue;
import brachy.modularui.value.sync.PanelSyncManager;
import brachy.modularui.value.sync.SyncHandlers;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.ProgressWidget;
import brachy.modularui.widgets.SlotGroupWidget;
import brachy.modularui.widgets.layout.Flow;
import brachy.modularui.widgets.slot.FluidSlot;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;
import brachy.modularui.widgets.slot.SlotGroup;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2ReferenceOpenHashMap;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    private Map<IO, Map<RecipeCapability<?>, Int2IntArrayMap>> gridLength = new EnumMap<>(IO.class);
    private Map<IO, Map<RecipeCapability<?>, Int2IntArrayMap>> gridWidths = new EnumMap<>(IO.class);

    public GTRecipeTypeUILayout() {}

    public ParentWidget<?> getBackedSlotsRow(@NotNull PanelSyncManager syncManager,
                                             @NotNull String themeId,
                                             @Nullable NotifiableItemStackHandler inputItems,
                                             @Nullable NotifiableItemStackHandler outputItems,
                                             @Nullable NotifiableFluidTank inputFluids,
                                             @Nullable NotifiableFluidTank outputFluids,
                                             DoubleSupplier progressSupplier, int tier) {
        Objects.requireNonNull(recipeType);

        DoubleSyncValue progressPercent = syncManager.getOrCreateSyncHandler("progressPercent",
                DoubleSyncValue.class, () -> new DoubleSyncValue(progressSupplier));

        var backedSlotsRow = Flow.row()
                .coverChildren()
                .horizontalCenter()
                .childPadding((progressSize / 2) + 2)
                .childIf(inputItems != null || inputFluids != null, () -> getIOColumn(syncManager, IO.IN,
                        inputItems, inputFluids, themeId, tier))
                .child(new ProgressWidget()
                        .value(progressPercent)
                        .name("progressBar")
                        .texture(progressBar, progressSize)
                        .size(progressSize)
                        .direction(progressDirection))
                .childIf(outputFluids != null || outputItems != null, () -> getIOColumn(syncManager, IO.OUT,
                        outputItems, outputFluids, themeId, tier));

        return new ParentWidget<>()
                .widthRel(1f)
                .coverChildrenHeight()
                .child(backedSlotsRow);
    }

    @Contract("_, _, null, null, _, _ -> fail")
    public Flow getIOColumn(@NotNull PanelSyncManager syncManager,
                             IO io,
                             @Nullable NotifiableItemStackHandler items,
                             @Nullable NotifiableFluidTank fluids,
                             String themeId,
                             int tier) {
        boolean in = io == IO.IN;

        if (items == null && fluids == null)
            throw new IllegalArgumentException("Item and fluid handler cannot both be null");

        var caps = (in ? recipeType.maxInputs : recipeType.maxOutputs);

        Flow ioColumn = Flow.col().coverChildren();

        var widgetGroups = new ArrayList<ParentWidget<?>>();

        for (var recipeCap : caps.keySet()) {
            int maxRecipeTypeSlots = caps.getInt(recipeCap);
            int maxMachineSlots = 0;
            if (maxRecipeTypeSlots == 0 || recipeCap == EURecipeCapability.CAP) continue;
            if (recipeCap == ItemRecipeCapability.CAP) {
                if (items == null) continue;
                maxMachineSlots = items.getSlots();
            } else if (recipeCap == FluidRecipeCapability.CAP) {
                if (fluids == null) continue;
                maxMachineSlots = fluids.getTanks();
            }

            var grid = createGrid(io, recipeCap, 's', tier, maxMachineSlots);

            IDrawable defaultSlotBackground = (recipeCap == ItemRecipeCapability.CAP ?
                    ThemeAPI.INSTANCE.getTheme(themeId).getItemSlotTheme().theme().getBackground() :
                    ThemeAPI.INSTANCE.getTheme(themeId).getFluidSlotTheme().theme().getBackground());

            SlotGroupWidget.Builder slotWidgetBuilder = SlotGroupWidget.builder()
                    .matrix(grid);

            if (recipeCap == ItemRecipeCapability.CAP) {
                SlotGroup group = new SlotGroup("item_" + io.name(), grid[0].length());
                slotWidgetBuilder.key('s', i -> {
                    var overlay = IDrawable.EMPTY;

                    if (overlays.containsKey(io) && overlays.get(io).containsKey(recipeCap)) {
                        overlay = overlays.get(io).get(recipeCap).get(i) != null ?
                                overlays.get(io).get(recipeCap).get(i) : IDrawable.EMPTY;
                    }

                    return new ItemSlot().slot(new ModularSlot(items, i)
                            .slotGroup(group))
                            .background(defaultSlotBackground, overlay);
                });
            } else if (recipeCap == FluidRecipeCapability.CAP) {
                String syncHandlerName = "fluid_" + io.name();
                for (int i = 0; i < maxMachineSlots; i++) {
                    syncManager.syncValue(syncHandlerName, i, SyncHandlers.fluidSlot(fluids.getStorages()[i]));
                }
                slotWidgetBuilder.key('s', i -> {
                    var overlay = IDrawable.EMPTY;

                    if (overlays.containsKey(io) && overlays.get(io).containsKey(recipeCap)) {
                        overlay = overlays.get(io).get(recipeCap).get(i) != null ?
                                overlays.get(io).get(recipeCap).get(i) : IDrawable.EMPTY;
                    }

                    return new FluidSlot()
                            .syncHandler(syncHandlerName, i)
                            .background(defaultSlotBackground, overlay);
                });
            }

            widgetGroups.add(slotWidgetBuilder.build()
                    .coverChildren()
                    .name(recipeCap.name + "_" + io.name()));
        }

        widgetGroups.forEach(ioColumn::child);
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
