package com.gregtechceu.gtceu.integration.recipeviewer.widgets;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.*;
import com.gregtechceu.gtceu.api.mui.base.drawable.IDrawable;
import com.gregtechceu.gtceu.api.mui.drawable.UITexture;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.widget.ParentWidget;
import com.gregtechceu.gtceu.api.mui.widgets.ProgressWidget;
import com.gregtechceu.gtceu.api.mui.widgets.TextWidget;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Flow;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.recipe.gui.GTRecipeTypeUILayout;
import com.gregtechceu.gtceu.api.recipe.gui.GTRecipeTypeUIs;
import com.gregtechceu.gtceu.api.recipe.ingredient.EnergyStack;
import com.gregtechceu.gtceu.integration.recipeviewer.RecipeSlotRole;
import com.gregtechceu.gtceu.integration.recipeviewer.entry.item.ItemEntryList;
import com.gregtechceu.gtceu.integration.recipeviewer.entry.fluid.FluidEntryList;
import com.gregtechceu.gtceu.utils.FormattingUtil;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.function.Function;

/**
 * MUI2-based recipe widget for displaying recipes in recipe viewers (JEI/EMI/REI).
 * This replaces the LDLib-based {@link GTRecipeWidget} as part of the MUI2 migration.
 */
public class GTMuiRecipeWidget extends ParentWidget<GTMuiRecipeWidget> {

    public static final int LINE_HEIGHT = 10;
    private static final int MAX_SLOT_ROW_WIDTH = 3;
    private static final int SLOT_SIZE = 18;
    private static final int PROGRESS_SIZE = 20;

    private final GTRecipe recipe;
    private final int minTier;

    public GTMuiRecipeWidget(GTRecipe recipe) {
        this.recipe = recipe;
        this.minTier = RecipeHelper.getRecipeEUtTier(recipe);
        initializeWidgets();
    }

    private void initializeWidgets() {
        // Collect recipe ingredient data
        var inputItems = collectIngredients(IO.IN, ItemRecipeCapability.CAP);
        var inputFluids = collectFluidIngredients(IO.IN, FluidRecipeCapability.CAP);
        var outputItems = collectIngredients(IO.OUT, ItemRecipeCapability.CAP);
        var outputFluids = collectFluidIngredients(IO.OUT, FluidRecipeCapability.CAP);

        var inputItemContents = collectContents(IO.IN, ItemRecipeCapability.CAP);
        var inputFluidContents = collectContents(IO.IN, FluidRecipeCapability.CAP);
        var outputItemContents = collectContents(IO.OUT, ItemRecipeCapability.CAP);
        var outputFluidContents = collectContents(IO.OUT, FluidRecipeCapability.CAP);

        // Get layout info from recipe type UI if available
        GTRecipeTypeUILayout layout = GTRecipeTypeUIs.recipeTypeUIs.get(recipe.recipeType);

        // Calculate grid dimensions
        int inputItemCount = inputItems.size();
        int inputFluidCount = inputFluids.size();
        int outputItemCount = outputItems.size();
        int outputFluidCount = outputFluids.size();

        int inputItemRows = (int) Math.ceil((float) inputItemCount / MAX_SLOT_ROW_WIDTH);
        int inputFluidRows = (int) Math.ceil((float) inputFluidCount / MAX_SLOT_ROW_WIDTH);
        int outputItemRows = (int) Math.ceil((float) outputItemCount / MAX_SLOT_ROW_WIDTH);
        int outputFluidRows = (int) Math.ceil((float) outputFluidCount / MAX_SLOT_ROW_WIDTH);

        int inputRows = inputItemRows + inputFluidRows;
        int outputRows = outputItemRows + outputFluidRows;
        int maxRows = Math.max(Math.max(inputRows, outputRows), 1);

        int inputItemCols = Math.min(inputItemCount, MAX_SLOT_ROW_WIDTH);
        int inputFluidCols = Math.min(inputFluidCount, MAX_SLOT_ROW_WIDTH);
        int outputItemCols = Math.min(outputItemCount, MAX_SLOT_ROW_WIDTH);
        int outputFluidCols = Math.min(outputFluidCount, MAX_SLOT_ROW_WIDTH);

        int maxInputWidth = Math.max(inputItemCols, inputFluidCols);
        int maxOutputWidth = Math.max(outputItemCols, outputFluidCols);

        // Calculate total size
        int progressBarSize = (layout != null) ? layout.getProgressSize() : PROGRESS_SIZE;
        int slotAreaWidth = (maxInputWidth + maxOutputWidth) * SLOT_SIZE + progressBarSize + 8;
        int slotAreaHeight = maxRows * SLOT_SIZE;

        // Text area for recipe info
        int textHeight = calculateTextHeight();

        int totalWidth = Math.max(slotAreaWidth, 150);
        int totalHeight = slotAreaHeight + textHeight + 5;

        size(totalWidth, totalHeight);

        // Build the slot row
        Flow slotsRow = Flow.row()
                .coverChildrenHeight()
                .childPadding(4)
                .alignX(Alignment.CENTER);

        // Input column
        if (inputItemCount > 0 || inputFluidCount > 0) {
            Flow inputCol = Flow.col().coverChildren();
            if (inputItemCount > 0) {
                inputCol.child(buildSlotGrid(inputItems, inputItemContents, MAX_SLOT_ROW_WIDTH,
                        RecipeSlotRole.INPUT, layout, IO.IN, ItemRecipeCapability.CAP));
            }
            if (inputFluidCount > 0) {
                inputCol.child(buildFluidSlotGrid(inputFluids, inputFluidContents, MAX_SLOT_ROW_WIDTH,
                        RecipeSlotRole.INPUT, layout, IO.IN, FluidRecipeCapability.CAP));
            }
            slotsRow.child(inputCol);
        }

        // Progress bar
        if (layout != null && layout.getProgressBar() != null) {
            slotsRow.child(new ProgressWidget()
                    .texture(layout.getProgressBar(), progressBarSize)
                    .direction(layout.getProgressDirection() != null ?
                            layout.getProgressDirection() : ProgressWidget.Direction.RIGHT)
                    .progress(0.0)
                    .size(progressBarSize)
                    .alignY(Alignment.CENTER));
        } else {
            slotsRow.child(new ProgressWidget()
                    .size(PROGRESS_SIZE)
                    .progress(0.0)
                    .alignY(Alignment.CENTER));
        }

        // Output column
        if (outputItemCount > 0 || outputFluidCount > 0) {
            Flow outputCol = Flow.col().coverChildren();
            if (outputItemCount > 0) {
                outputCol.child(buildSlotGrid(outputItems, outputItemContents, MAX_SLOT_ROW_WIDTH,
                        RecipeSlotRole.OUTPUT, layout, IO.OUT, ItemRecipeCapability.CAP));
            }
            if (outputFluidCount > 0) {
                outputCol.child(buildFluidSlotGrid(outputFluids, outputFluidContents, MAX_SLOT_ROW_WIDTH,
                        RecipeSlotRole.OUTPUT, layout, IO.OUT, FluidRecipeCapability.CAP));
            }
            slotsRow.child(outputCol);
        }

        child(slotsRow);

        // Add recipe info text
        addRecipeText(slotAreaHeight + 5, totalWidth, totalHeight);
    }

    private Flow buildSlotGrid(List<ItemEntryList> ingredients, List<Content> contents,
                               int maxWidth, RecipeSlotRole role,
                               GTRecipeTypeUILayout layout, IO io, RecipeCapability<?> cap) {
        Flow grid = Flow.row().wrap(true).coverChildren().childPadding(0);
        int cols = Math.min(ingredients.size(), maxWidth);
        grid.width(cols * SLOT_SIZE);

        for (int i = 0; i < ingredients.size(); i++) {
            ItemEntryList entryList = ingredients.get(i);
            XEIItemWidget widget = new XEIItemWidget()
                    .ingredients(entryList.getStacks())
                    .role(role);

            // Apply overlay from layout if available
            if (layout != null) {
                var overlays = layout.getOverlays();
                if (overlays.containsKey(io) && overlays.get(io).containsKey(cap)) {
                    IDrawable overlay = overlays.get(io).get(cap).get(i);
                    if (overlay != null) {
                        widget.overlay(overlay);
                    }
                }
            }

            // Apply chance overlay from content
            if (i < contents.size()) {
                Content content = contents.get(i);
                addChanceTooltip(widget, content);
            }

            grid.child(widget);
        }
        return grid;
    }

    private Flow buildFluidSlotGrid(List<FluidEntryList> ingredients, List<Content> contents,
                                    int maxWidth, RecipeSlotRole role,
                                    GTRecipeTypeUILayout layout, IO io, RecipeCapability<?> cap) {
        Flow grid = Flow.row().wrap(true).coverChildren().childPadding(0);
        int cols = Math.min(ingredients.size(), maxWidth);
        grid.width(cols * SLOT_SIZE);

        for (int i = 0; i < ingredients.size(); i++) {
            FluidEntryList entryList = ingredients.get(i);
            XEIFluidWidget widget = new XEIFluidWidget()
                    .ingredients(entryList.getStacks())
                    .role(role);

            // Apply overlay from layout if available
            if (layout != null) {
                var overlays = layout.getOverlays();
                if (overlays.containsKey(io) && overlays.get(io).containsKey(cap)) {
                    IDrawable overlay = overlays.get(io).get(cap).get(i);
                    if (overlay != null) {
                        widget.overlay(overlay);
                    }
                }
            }

            // Apply chance tooltip from content
            if (i < contents.size()) {
                Content content = contents.get(i);
                addChanceTooltip(widget, content);
            }

            grid.child(widget);
        }
        return grid;
    }

    @SuppressWarnings("unchecked")
    private List<ItemEntryList> collectIngredients(IO io, RecipeCapability<?> cap) {
        var contentMap = io == IO.IN ? recipe.inputs : recipe.outputs;
        var tickContentMap = io == IO.IN ? recipe.tickInputs : recipe.tickOutputs;

        List<Content> contents = new ArrayList<>();
        if (contentMap.containsKey(cap)) {
            contents.addAll(contentMap.get(cap));
        }
        if (tickContentMap.containsKey(cap)) {
            contents.addAll(tickContentMap.get(cap));
        }

        List<Object> entryLists = cap.createXEIContainerContents(contents, recipe, io);
        List<ItemEntryList> result = new ArrayList<>();
        for (Object obj : entryLists) {
            if (obj instanceof ItemEntryList itemList) {
                result.add(itemList);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<FluidEntryList> collectFluidIngredients(IO io, RecipeCapability<?> cap) {
        var contentMap = io == IO.IN ? recipe.inputs : recipe.outputs;
        var tickContentMap = io == IO.IN ? recipe.tickInputs : recipe.tickOutputs;

        List<Content> contents = new ArrayList<>();
        if (contentMap.containsKey(cap)) {
            contents.addAll(contentMap.get(cap));
        }
        if (tickContentMap.containsKey(cap)) {
            contents.addAll(tickContentMap.get(cap));
        }

        List<Object> entryLists = cap.createXEIContainerContents(contents, recipe, io);
        List<FluidEntryList> result = new ArrayList<>();
        for (Object obj : entryLists) {
            if (obj instanceof FluidEntryList fluidList) {
                result.add(fluidList);
            }
        }
        return result;
    }

    private List<Content> collectContents(IO io, RecipeCapability<?> cap) {
        var contentMap = io == IO.IN ? recipe.inputs : recipe.outputs;
        var tickContentMap = io == IO.IN ? recipe.tickInputs : recipe.tickOutputs;

        List<Content> contents = new ArrayList<>();
        if (contentMap.containsKey(cap)) {
            contents.addAll(contentMap.get(cap));
        }
        if (tickContentMap.containsKey(cap)) {
            contents.addAll(tickContentMap.get(cap));
        }
        return contents;
    }

    private void addChanceTooltip(XEIItemWidget widget, Content content) {
        if (content.chance < content.maxChance) {
            float chancePercent = 100f * content.chance / content.maxChance;
            widget.addTooltipLine(Component.translatable("gtceu.gui.content.chance_no_boost",
                    FormattingUtil.formatNumber2Places(chancePercent) + "%")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private void addChanceTooltip(XEIFluidWidget widget, Content content) {
        if (content.chance < content.maxChance) {
            float chancePercent = 100f * content.chance / content.maxChance;
            widget.addTooltipLine(Component.translatable("gtceu.gui.content.chance_no_boost",
                    FormattingUtil.formatNumber2Places(chancePercent) + "%")
                    .withStyle(ChatFormatting.YELLOW));
        }
    }

    private int calculateTextHeight() {
        int lines = 0;
        EnergyStack eut = RecipeHelper.getRealEUt(recipe);
        if (!eut.isEmpty()) {
            lines += 3; // Duration, Total EU, EU/t line
        } else {
            lines += 1; // Just duration
        }
        // Add condition lines
        lines += recipe.conditions.size();
        // Add data info lines
        lines += recipe.recipeType.getDataInfos().size();
        return lines * LINE_HEIGHT;
    }

    private void addRecipeText(int yStart, int totalWidth, int totalHeight) {
        int y = yStart;
        int duration = recipe.duration;
        EnergyStack.WithIO eut = RecipeHelper.getRealEUtWithIO(recipe);

        if (!recipe.data.getBoolean("hide_duration")) {
            child(new TextWidget<>(Component.translatable("gtceu.recipe.duration",
                    FormattingUtil.formatNumbers(duration / 20f)).getString())
                    .color(0xFFFFFF)
                    .shadow(true)
                    .pos(3, y));
            y += LINE_HEIGHT;
        }

        if (eut.voltage() > 0) {
            long euTotal = eut.getTotalEU() * duration;
            child(new TextWidget<>(Component.translatable("gtceu.recipe.total",
                    FormattingUtil.formatNumbers(euTotal)).getString())
                    .color(0xFFFFFF)
                    .shadow(true)
                    .pos(3, y));
            y += LINE_HEIGHT;

            int voltageTier = GTUtil.getTierByVoltage(eut.voltage());
            float amperage = (float) eut.getTotalEU() / GTValues.V[voltageTier];
            child(new TextWidget<>(Component.translatable(
                    eut.isInput() ? "gtceu.recipe.eu" : "gtceu.recipe.eu_inverted",
                    FormattingUtil.formatNumber2Places(amperage),
                    GTValues.VN[voltageTier]).getString())
                    .color(0xFFFFFF)
                    .shadow(true)
                    .pos(3, y));
            y += LINE_HEIGHT;

            // Voltage tier indicator
            child(new TextWidget<>(GTValues.VNF[minTier])
                    .color(0xFFFFFF)
                    .pos(totalWidth - 20, totalHeight - 10));
        }

        // Conditions
        for (RecipeCondition<?> condition : recipe.conditions) {
            if (condition.getTooltips() == null) continue;
            child(new TextWidget<>(condition.getTooltips().getString())
                    .color(0xFFFFFF)
                    .shadow(true)
                    .pos(3, y));
            y += LINE_HEIGHT;
        }

        // Data info
        for (Function<CompoundTag, String> dataInfo : recipe.recipeType.getDataInfos()) {
            child(new TextWidget<>(dataInfo.apply(recipe.data))
                    .color(0xFFFFFF)
                    .shadow(true)
                    .pos(3, y));
            y += LINE_HEIGHT;
        }
    }
}
