package com.gregtechceu.gtceu.integration.recipeviewer.widgets;

import com.gregtechceu.gtceu.api.mui.base.ITheme;
import com.gregtechceu.gtceu.api.mui.base.drawable.IDrawable;
import com.gregtechceu.gtceu.api.mui.drawable.GuiDraw;
import com.gregtechceu.gtceu.api.mui.theme.WidgetThemeEntry;
import com.gregtechceu.gtceu.api.mui.widgets.AbstractFluidDisplayWidget;
import com.gregtechceu.gtceu.client.mui.screen.RichTooltip;
import com.gregtechceu.gtceu.client.mui.screen.viewport.ModularGuiContext;
import com.gregtechceu.gtceu.integration.recipeviewer.RecipeSlotRole;
import com.gregtechceu.gtceu.integration.recipeviewer.entry.EntryList;
import com.gregtechceu.gtceu.integration.recipeviewer.entry.fluid.FluidStackList;
import com.gregtechceu.gtceu.integration.recipeviewer.handlers.IngredientProvider;

import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A lightweight client-side widget for displaying fluid ingredients in recipe viewers (JEI/EMI/REI).
 * Unlike FluidSlot, this does not require a sync handler or fluid tank backing.
 * It holds a list of FluidStacks for ingredient cycling and implements IngredientProvider.
 */
public class XEIFluidWidget extends AbstractFluidDisplayWidget<XEIFluidWidget>
                             implements IngredientProvider<FluidStack> {

    public static final int SIZE = 18;

    private final FluidStackList ingredients;
    private RecipeSlotRole role = RecipeSlotRole.RENDER_ONLY;
    private IDrawable overlay = null;
    private final List<Component> tooltipLines = new ArrayList<>();

    public XEIFluidWidget() {
        this.ingredients = new FluidStackList();
        size(SIZE);
    }

    public XEIFluidWidget(List<FluidStack> stacks) {
        this.ingredients = FluidStackList.of(stacks);
        size(SIZE);
    }

    public XEIFluidWidget ingredient(FluidStack stack) {
        this.ingredients.add(stack);
        return this;
    }

    public XEIFluidWidget ingredients(List<FluidStack> stacks) {
        this.ingredients.addAll(stacks);
        return this;
    }

    public XEIFluidWidget ingredients(FluidStack... stacks) {
        this.ingredients.addAll(Arrays.asList(stacks));
        return this;
    }

    public XEIFluidWidget role(RecipeSlotRole role) {
        this.role = role;
        return this;
    }

    public XEIFluidWidget overlay(IDrawable overlay) {
        this.overlay = overlay;
        return this;
    }

    public XEIFluidWidget addTooltipLine(Component line) {
        this.tooltipLines.add(line);
        return this;
    }

    @Override
    public void onInit() {
        size(SIZE);
    }

    @Override
    protected boolean displayAmountText() {
        List<FluidStack> stacks = ingredients.getStacks();
        if (stacks.isEmpty()) return false;
        long tick = System.currentTimeMillis() / 1000;
        int index = (int) (tick % stacks.size());
        return stacks.get(index).getAmount() > 0;
    }

    @Override
    @Nullable
    protected FluidStack getFluidStack() {
        List<FluidStack> stacks = ingredients.getStacks();
        if (stacks.isEmpty()) return null;
        long tick = System.currentTimeMillis() / 1000;
        int index = (int) (tick % stacks.size());
        return stacks.get(index);
    }

    @Override
    public void drawOverlay(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        super.drawOverlay(context, widgetTheme);
        if (overlay != null) {
            overlay.draw(context, 1, 1, 16, 16, widgetTheme.getTheme());
        }
    }

    @Override
    public void drawForeground(ModularGuiContext context) {
        if (isHoveringFor(0) && !tooltipLines.isEmpty()) {
            RichTooltip tooltip = new RichTooltip().parent(this);
            for (Component line : tooltipLines) {
                tooltip.addLine(line);
            }
            tooltip.draw(context);
        }
    }

    @Override
    public @NotNull EntryList<FluidStack> getIngredients() {
        return ingredients;
    }

    @Override
    public @NotNull Class<FluidStack> ingredientClass() {
        return FluidStack.class;
    }

    @Override
    public @NotNull RecipeSlotRole recipeRole() {
        return role;
    }
}
