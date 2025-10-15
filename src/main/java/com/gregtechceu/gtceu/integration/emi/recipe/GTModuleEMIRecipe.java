package com.gregtechceu.gtceu.integration.emi.recipe;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.api.item.module.TieredItemModule;
import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.gregtechceu.gtceu.common.data.GTRecipeTypes;
import com.gregtechceu.gtceu.common.recipe.type.EquipmentFoundryRecipe;
import com.gregtechceu.gtceu.utils.GTStringUtils;

import com.lowdragmc.lowdraglib.emi.ModularEmiRecipe;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.crafting.Ingredient;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GTModuleEMIRecipe extends ModularEmiRecipe<WidgetGroup> implements EmiRecipe {

    public static final EmiRecipeCategory CATEGORY = new EmiRecipeCategory(
            GTCEu.id("equipment_foundry"),
            EmiIngredient.of(Ingredient.of(GTBlocks.EQUIPMENT_FOUNDRY)));

    private final EquipmentFoundryRecipe recipe;

    public GTModuleEMIRecipe(EquipmentFoundryRecipe recipe) {
        super(() -> createUIWidget(recipe));
        this.recipe = recipe;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CATEGORY;
    }

    @Override
    public @Nullable ResourceLocation getId() {
        return recipe.getId();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return List.of(EmiIngredient.of(recipe.getEquipment()), EmiIngredient.of(recipe.getIngredient()));
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of();
    }

    @Override
    public List<EmiIngredient> getCatalysts() {
        return List.of(EmiIngredient.of(Ingredient.of(GTBlocks.EQUIPMENT_FOUNDRY)));
    }

    @Override
    public int getDisplayWidth() {
        return 200;
    }

    @Override
    public int getDisplayHeight() {
        int height = 57;
        height += Minecraft.getInstance().font.wordWrapHeight(recipe.getModules()[0].getInfo(), getDisplayWidth() - 8);
        return height;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        super.addWidgets(widgets);
        Font font = Minecraft.getInstance().font;
        Component appliedTo = Component.translatable("gtceu.equipment_foundry.gui.applied_to");
        Component moduleItem = Component.translatable("gtceu.equipment_foundry.gui.module_item");
        widgets.addText(appliedTo, 4, 8, 0xFFFFFFFF, true);
        widgets.addText(moduleItem, font.width(appliedTo) + 36, 8, 0xFFFFFFFF, true);
        widgets.addSlot(EmiIngredient.of(recipe.getEquipment()), 8 + font.width(appliedTo), 4);
        widgets.addSlot(EmiIngredient.of(recipe.getIngredient()), 40 + font.width(appliedTo) + font.width(moduleItem),
                4);
    }

    public static WidgetGroup createUIWidget(EquipmentFoundryRecipe recipe) {
        Font font = Minecraft.getInstance().font;
        WidgetGroup widgets = new WidgetGroup();
        int y = 30;
        List<LabelWidget> desc = new ArrayList<>();
        if (recipe.getModules()[0] instanceof TieredItemModule) {
            TieredItemModule[] tieredModules = Arrays.stream(recipe.getModules())
                    .map(module -> (TieredItemModule) module).toArray(TieredItemModule[]::new);
            int minTier = tieredModules[0].getTier();
            int maxTier = tieredModules[tieredModules.length - 1].getTier();
            if (minTier != maxTier) {
                widgets.addWidget(new LabelWidget(2, y, Component.translatable(
                        "gtceu.equipment_foundry.gui.supports_tiers",
                        GTValues.VNF[minTier],
                        GTValues.VNF[maxTier])));
                y += font.lineHeight;
            }
            int[] selectedTier = new int[] { tieredModules[0].getTier() };
            LabelWidget tierLabel = new LabelWidget(2, y, Component.translatable(
                    "gtceu.equipment_foundry.gui.tier",
                    GTValues.VNF[selectedTier[0]]));
            widgets.addWidget(tierLabel);
            int finalY = y;
            ButtonWidget button = new ButtonWidget(
                    font.width(Component.translatable("gtceu.equipment_foundry.gui.tier", "")) +
                            tierLabel.getPositionX(),
                    tierLabel.getPositionY(),
                    font.width(GTValues.VNF[selectedTier[0]]), tierLabel.getSizeHeight(),
                    click -> {
                        if (click.button == GLFW.GLFW_MOUSE_BUTTON_LEFT) selectedTier[0]++;
                        if (click.button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) selectedTier[0]--;
                        selectedTier[0] = Mth.clamp(selectedTier[0], minTier, maxTier);
                        tierLabel.setComponent(Component.translatable("gtceu.equipment_foundry.gui.tier",
                                GTValues.VNF[selectedTier[0]]));
                        if (!desc.isEmpty()) {
                            desc.forEach(widget -> widget.setComponent(Component.literal("")));
                            int i = 0;
                            for (FormattedCharSequence line : font
                                    .split(tieredModules[selectedTier[0] - minTier].getInfo(), 196)) {
                                Component component = GTStringUtils.toComponent(line);
                                if (desc.size() <= i) {
                                    desc.add(new LabelWidget(2, finalY + (i + 2) * font.lineHeight, component));
                                    widgets.addWidget(desc.get(i));
                                }
                                desc.get(i).setComponent(component);
                                i++;
                            }
                        }
                    });
            button.setHoverTooltips(Component.translatable("gtceu.equipment_foundry.gui.tooltip.tier_switch"));
            widgets.addWidget(button);
            y += 2 * font.lineHeight;
        }
        for (FormattedCharSequence line : font.split(recipe.getModules()[0].getInfo(), 196)) {
            desc.add(new LabelWidget(2, y, GTStringUtils.toComponent(line)));
            widgets.addWidget(desc.get(desc.size() - 1));
            y += font.lineHeight;
        }
        return widgets;
    }

    public static void addRecipes(EmiRegistry registry) {
        registry.getRecipeManager()
                .getAllRecipesFor(GTRecipeTypes.EQUIPMENT_FOUNDRY_RECIPES.get())
                .stream()
                .map(GTModuleEMIRecipe::new)
                .forEach(registry::addRecipe);
    }
}
