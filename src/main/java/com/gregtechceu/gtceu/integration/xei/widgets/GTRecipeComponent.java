package com.gregtechceu.gtceu.integration.xei.widgets;

import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.capability.recipe.CWURecipeCapability;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.capability.recipe.RecipeCapability;
import com.gregtechceu.gtceu.api.recipe.GTRecipe;
import com.gregtechceu.gtceu.api.recipe.OverclockingLogic;
import com.gregtechceu.gtceu.api.recipe.RecipeCondition;
import com.gregtechceu.gtceu.api.recipe.RecipeHelper;
import com.gregtechceu.gtceu.api.recipe.chance.boost.ChanceBoostFunction;
import com.gregtechceu.gtceu.api.recipe.chance.logic.ChanceLogic;
import com.gregtechceu.gtceu.api.recipe.content.Content;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.component.*;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;
import com.gregtechceu.gtceu.api.ui.util.UIComponentUtils;
import com.gregtechceu.gtceu.common.recipe.condition.DimensionCondition;
import com.gregtechceu.gtceu.utils.FormattingUtil;

import com.lowdragmc.lowdraglib.LDLib;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.fml.loading.FMLLoader;

import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.gregtechceu.gtceu.api.GTValues.*;

/**
 * @author KilaBash
 * @date 2023/2/25
 * @implNote GTRecipeComponent
 */
public class GTRecipeComponent extends FlowLayout {

    public static final String RECIPE_CONTENT_GROUP_ID = "recipe_content_group";
    public static final Pattern RECIPE_CONTENT_GROUP_ID_REGEX = Pattern.compile("^recipe_content_group$");

    public static final int LINE_HEIGHT = 10;

    @Getter
    private final GTRecipe recipe;
    private final List<LabelComponent> recipeParaTexts = new ArrayList<>();
    private final int minTier;
    private int tier;
    private ButtonComponent voltageTextComponent;

    public GTRecipeComponent(GTRecipe recipe) {
        super(Sizing.fixed(recipe.recipeType.getRecipeUI().getRecipeViewerSize().width()),
                Sizing.fixed(recipe.recipeType.getRecipeUI().getRecipeViewerSize().height()),
                Algorithm.VERTICAL);
        positioning(Positioning.absolute(0, 0));
        verticalAlignment(VerticalAlignment.TOP);
        horizontalAlignment(HorizontalAlignment.LEFT);
        this.recipe = recipe;
        this.padding(Insets.of(0, 0, /*-getXOffset(recipe) + */3, 0));
        this.minTier = RecipeHelper.getRecipeEUtTier(recipe);
    }

    private static int getXOffset(GTRecipe recipe) {
        if (recipe.recipeType.getRecipeUI().getOriginalWidth() !=
                recipe.recipeType.getRecipeUI().getRecipeViewerSize().width()) {
            return (recipe.recipeType.getRecipeUI().getRecipeViewerSize().width() -
                    recipe.recipeType.getRecipeUI().getOriginalWidth()) / 2;
        }
        return 0;
    }

    @Override
    public void containerAccess(UIComponentMenuAccess access) {
        super.containerAccess(access);
        access.adapter().leftPos(0);
        access.adapter().topPos(0);
        setRecipeWidget();
        setTierToMin();
        initializeRecipeTextWidget();
        addButtons();
    }

    @SuppressWarnings("UnstableApiUsage")
    private void setRecipeWidget() {
        var storages = Tables.newCustomTable(new EnumMap<>(IO.class), LinkedHashMap<RecipeCapability<?>, Object>::new);
        var contents = Tables.newCustomTable(new EnumMap<>(IO.class),
                LinkedHashMap<RecipeCapability<?>, List<Content>>::new);
        collectStorage(storages, contents, recipe);

        // noinspection unchecked
        FlowLayout group = recipe.recipeType.getRecipeUI().createUITemplate(ProgressComponent.JEIProgress,
                (UIAdapter<StackLayout>) this.containerAccess().adapter(),
                storages,
                recipe.data.copy(), recipe.conditions);
        addSlots(contents, group, (UIAdapter<StackLayout>) this.containerAccess().adapter(), recipe);

        // Ensure any previous instances of the widget are removed first. This applies when changing the recipe
        // preview's voltage tier, as this recipe widget stays the same while its contents are updated.
        group.id(RECIPE_CONTENT_GROUP_ID);
        childrenByPattern(RECIPE_CONTENT_GROUP_ID_REGEX).forEach(this::removeChild);

        child(group.positioning(Positioning.relative(50, 0)));

        /*
         * var EUt = RecipeHelper.getInputEUt(recipe);
         * if (EUt == 0) {
         * EUt = RecipeHelper.getOutputEUt(recipe);
         * }
         * int yOffset = 5 + size.height();
         * yOffset += EUt > 0 ? 20 : 0;
         * if (recipe.data.getBoolean("duration_is_total_cwu")) {
         * yOffset -= LINE_HEIGHT;
         * }
         */

        /// add text based on i/o's
        for (var capability : recipe.inputs.entrySet()) {
            capability.getKey().addXEIInfo(this, recipe, capability.getValue(), false, true);
        }
        for (var capability : recipe.tickInputs.entrySet()) {
            capability.getKey().addXEIInfo(this, recipe, capability.getValue(), true, true);
        }
        for (var capability : recipe.outputs.entrySet()) {
            capability.getKey().addXEIInfo(this, recipe, capability.getValue(), false, false);
        }
        for (var capability : recipe.tickOutputs.entrySet()) {
            capability.getKey().addXEIInfo(this, recipe, capability.getValue(), true, false);
        }

        for (RecipeCondition condition : recipe.conditions) {
            if (condition.getTooltips() == null) continue;
            if (condition instanceof DimensionCondition dimCondition) {
                child(dimCondition.setupDimensionMarkers());
            } else child(UIComponents.label(condition.getTooltips()));
        }
        for (Function<CompoundTag, Component> dataInfo : recipe.recipeType.getDataInfos()) {
            child(UIComponents.label(dataInfo.apply(recipe.data)));
        }
        recipe.recipeType.getRecipeUI().appendJEIUI(recipe, this);
    }

    private void initializeRecipeTextWidget() {
        String tierText = GTValues.VNF[tier];
        int duration = recipe.duration;
        long inputEUt = RecipeHelper.getInputEUt(recipe);
        long outputEUt = RecipeHelper.getOutputEUt(recipe);
        List<Component> texts = getRecipeParaText(recipe, duration, inputEUt, outputEUt);
        for (Component text : texts) {
            LabelComponent labelWidget = UIComponents.label(text).color(Color.BLACK);
            labelWidget.sizing(Sizing.content(), Sizing.fixed(LINE_HEIGHT));
            child(labelWidget);
            recipeParaTexts.add(labelWidget);
        }
        if (inputEUt > 0) {
            ButtonComponent voltageTextComponent = UIComponents.button(Component.literal(tierText),
                    cd -> setRecipeOC(cd.button, cd.isShiftClick))
                    .configure(c -> c
                            .sizing(Sizing.fixed(16), Sizing.fixed(10))
                            .positioning(Positioning.relative(100, 100))
                            .margins(Insets.of(0, LINE_HEIGHT, 0, getVoltageXOffset()))
                            .tooltip(List.of(
                                    Component.translatable("gtceu.oc.tooltip.0", GTValues.VNF[minTier]),
                                    Component.translatable("gtceu.oc.tooltip.1"),
                                    Component.translatable("gtceu.oc.tooltip.2"),
                                    Component.translatable("gtceu.oc.tooltip.3"),
                                    Component.translatable("gtceu.oc.tooltip.4"))));
            if (recipe.recipeType.isOffsetVoltageText()) {
                voltageTextComponent
                        .margins(Insets.of(0, recipe.recipeType.getVoltageTextOffset(), 0, getVoltageXOffset()));
            }
            // make it clickable
            child(this.voltageTextComponent = voltageTextComponent);
        }
        updateLayout();
    }

    @NotNull
    private static List<Component> getRecipeParaText(GTRecipe recipe, int duration, long inputEUt, long outputEUt) {
        List<Component> texts = new ArrayList<>();
        if (!recipe.data.getBoolean("hide_duration")) {
            texts.add(Component.translatable("gtceu.recipe.duration", FormattingUtil.formatNumbers(duration / 20f)));
        }
        var EUt = inputEUt;
        boolean isOutput = false;
        if (EUt == 0) {
            EUt = outputEUt;
            isOutput = true;
        }
        if (EUt > 0) {
            long euTotal = EUt * duration;
            // sadly we still need a custom override here, since computation uses duration and EU/t very differently
            if (recipe.data.getBoolean("duration_is_total_cwu") &&
                    recipe.tickInputs.containsKey(CWURecipeCapability.CAP)) {
                int minimumCWUt = Math.max(recipe.tickInputs.get(CWURecipeCapability.CAP).stream()
                        .map(Content::getContent).mapToInt(CWURecipeCapability.CAP::of).sum(), 1);
                texts.add(Component.translatable("gtceu.recipe.max_eu",
                        FormattingUtil.formatNumbers(euTotal / minimumCWUt)));
            } else {
                texts.add(Component.translatable("gtceu.recipe.total", FormattingUtil.formatNumbers(euTotal)));
            }
            texts.add(Component.translatable(!isOutput ? "gtceu.recipe.eu" : "gtceu.recipe.eu_inverted",
                    FormattingUtil.formatNumbers(EUt)));
        }

        return texts;
    }

    private void addButtons() {
        // add a recipe id getter, btw all the things can only click within the WidgetGroup while using EMI
        int x = width() - 15;
        int y = height() - 30;
        child(new PredicatedButtonComponent(
                UITextures.group(GuiTextures.BUTTON, UITextures.text(Component.literal("ID"))),
                cd -> Minecraft.getInstance().keyboardHandler.setClipboard(recipe.id.toString()),
                () -> !FMLLoader.isProduction(), !FMLLoader.isProduction())
                .positioning(Positioning.absolute(x, y))
                // TODO make translatable
                .tooltip(Component.literal("click to copy: " + recipe.id)));
    }

    private int getVoltageXOffset() {
        int x = switch (tier) {
            case ULV, LuV, ZPM, UHV, UEV, UXV -> 20;
            case OpV, MAX -> 22;
            case UIV -> 18;
            case IV -> 12;
            default -> 14;
        };
        if (!LDLib.isEmiLoaded()) {
            x -= 3;
        }
        return x;
    }

    public void setRecipeOC(int button, boolean isShiftClick) {
        OverclockingLogic oc = OverclockingLogic.NON_PERFECT_OVERCLOCK;
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            setTier(tier + 1);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            setTier(tier - 1);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            setTierToMin();
        }
        if (isShiftClick) {
            oc = OverclockingLogic.PERFECT_OVERCLOCK;
        }
        setRecipeTextWidget(oc);
        setRecipeWidget();
    }

    private void setRecipeTextWidget(OverclockingLogic logic) {
        long inputEUt = RecipeHelper.getInputEUt(recipe);
        int duration = recipe.duration;
        String tierText = GTValues.VNF[tier];
        if (tier > minTier && inputEUt != 0) {
            int ocs = tier - minTier;
            if (minTier == ULV) ocs--;
            var params = new OverclockingLogic.OCParams(inputEUt, recipe.duration, ocs, 1);
            var result = logic.runOverclockingLogic(params, V[tier]);
            duration = (int) (duration * result.durationMultiplier());
            inputEUt = (long) (inputEUt * result.eutMultiplier());
            tierText = tierText.formatted(ChatFormatting.ITALIC);
        }
        List<Component> texts = getRecipeParaText(recipe, duration, inputEUt, 0);
        for (int i = 0; i < texts.size(); i++) {
            recipeParaTexts.get(i).text(texts.get(i));
        }
        voltageTextComponent.setMessage(Component.literal(tierText));
        // TODO implement
        // detectAndSendChanges();
    }

    public static void setConsumedChance(Content content, ChanceLogic logic, List<Component> tooltips, int recipeTier,
                                         int chanceTier, ChanceBoostFunction function) {
        if (content.chance < ChanceLogic.getMaxChancedValue()) {
            int boostedChance = function.getBoostedChance(content, recipeTier, chanceTier);
            if (boostedChance == 0) {
                tooltips.add(Component.translatable("gtceu.gui.content.chance_nc"));
            } else {
                float baseChanceFloat = 100f * content.chance / content.maxChance;
                float boostedChanceFloat = 100f * boostedChance / content.maxChance;
                if (logic != ChanceLogic.NONE && logic != ChanceLogic.OR) {
                    tooltips.add(Component.translatable("gtceu.gui.content.chance_base_logic",
                            FormattingUtil.formatNumber2Places(baseChanceFloat), logic.getTranslation())
                            .withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltips.add(
                            FormattingUtil.formatPercentage2Places("gtceu.gui.content.chance_base", baseChanceFloat));
                }
                if (content.tierChanceBoost != 0) {
                    String key = "gtceu.gui.content.chance_tier_boost_" +
                            ((content.tierChanceBoost > 0) ? "plus" : "minus");
                    tooltips.add(FormattingUtil.formatPercentage2Places(key,
                            Math.abs(100f * content.tierChanceBoost / content.maxChance)));
                }
                if (logic != ChanceLogic.NONE && logic != ChanceLogic.OR) {
                    tooltips.add(Component.translatable("gtceu.gui.content.chance_boosted_logic",
                            FormattingUtil.formatNumber2Places(boostedChanceFloat), logic.getTranslation())
                            .withStyle(ChatFormatting.YELLOW));
                } else {
                    tooltips.add(
                            FormattingUtil.formatPercentage2Places("gtceu.gui.content.chance_boosted",
                                    boostedChanceFloat));
                }
            }
        }
    }

    private void setTier(int tier) {
        this.tier = Mth.clamp(tier, minTier, GTValues.MAX);
    }

    private void setTierToMin() {
        setTier(minTier);
    }

    public void collectStorage(Table<IO, RecipeCapability<?>, Object> extraTable,
                               Table<IO, RecipeCapability<?>, List<Content>> extraContents, GTRecipe recipe) {
        Map<RecipeCapability<?>, List<Object>> inputCapabilities = new Object2ObjectLinkedOpenHashMap<>();
        for (var entry : recipe.inputs.entrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            List<Content> contents = entry.getValue();

            extraContents.put(IO.IN, cap, contents);
            inputCapabilities.put(cap, cap.createXEIContainerContents(contents, recipe, IO.IN));
        }
        for (var entry : recipe.tickInputs.entrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            List<Content> contents = entry.getValue();

            extraContents.put(IO.IN, cap, contents);
            inputCapabilities.put(cap, cap.createXEIContainerContents(contents, recipe, IO.IN));
        }
        for (var entry : inputCapabilities.entrySet()) {
            while (entry.getValue().size() < recipe.recipeType.getMaxInputs(entry.getKey())) entry.getValue().add(null);
            var container = entry.getKey().createXEIContainer(entry.getValue());
            if (container != null) {
                extraTable.put(IO.IN, entry.getKey(), container);
            }
        }

        Map<RecipeCapability<?>, List<Object>> outputCapabilities = new Object2ObjectLinkedOpenHashMap<>();
        for (var entry : recipe.outputs.entrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            List<Content> contents = entry.getValue();

            extraContents.put(IO.OUT, cap, contents);
            outputCapabilities.put(cap, cap.createXEIContainerContents(contents, recipe, IO.OUT));
        }
        for (var entry : recipe.tickOutputs.entrySet()) {
            RecipeCapability<?> cap = entry.getKey();
            List<Content> contents = entry.getValue();

            extraContents.put(IO.OUT, cap, contents);
            outputCapabilities.put(cap, cap.createXEIContainerContents(contents, recipe, IO.OUT));
        }
        for (var entry : outputCapabilities.entrySet()) {
            while (entry.getValue().size() < recipe.recipeType.getMaxOutputs(entry.getKey()))
                entry.getValue().add(null);
            var container = entry.getKey().createXEIContainer(entry.getValue());
            if (container != null) {
                extraTable.put(IO.OUT, entry.getKey(), container);
            }
        }
    }

    public void addSlots(Table<IO, RecipeCapability<?>, List<Content>> contentTable,
                         FlowLayout group,
                         UIAdapter<StackLayout> adapter,
                         GTRecipe recipe) {
        for (var capabilityEntry : contentTable.rowMap().entrySet()) {
            IO io = capabilityEntry.getKey();
            for (var contentsEntry : capabilityEntry.getValue().entrySet()) {
                RecipeCapability<?> cap = contentsEntry.getKey();
                int nonTickCount = (io == IO.IN ? recipe.getInputContents(cap) : recipe.getOutputContents(cap)).size();
                List<Content> contents = contentsEntry.getValue();
                // bind fluid out overlay
                UIComponentUtils.componentByIdForEach(group, "^%s.[0-9]+$".formatted(cap.slotName(io)),
                        cap.getWidgetClass(),
                        component -> {
                            var index = UIComponentUtils.componentIdIndex(component);
                            if (index >= 0 && index < contents.size()) {
                                var content = contents.get(index);
                                cap.applyUIComponentInfo(component, adapter, index, true, io, null, recipe.getType(),
                                        recipe, content,
                                        null, minTier, tier);
                                group.child(UIComponents
                                        .texture(content.createOverlay(index >= nonTickCount, minTier, tier,
                                                recipe.getType().getChanceFunction()))
                                        .sizing(component.horizontalSizing().get(), component.verticalSizing().get())
                                        .positioning(component.positioning().get()));
                            }
                        });
            }
        }
    }
}
