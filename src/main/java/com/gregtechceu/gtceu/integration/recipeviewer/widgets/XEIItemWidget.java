package com.gregtechceu.gtceu.integration.recipeviewer.widgets;

import com.gregtechceu.gtceu.api.mui.base.ITheme;
import com.gregtechceu.gtceu.api.mui.base.drawable.IDrawable;
import com.gregtechceu.gtceu.api.mui.drawable.GuiDraw;
import com.gregtechceu.gtceu.api.mui.theme.WidgetThemeEntry;
import com.gregtechceu.gtceu.api.mui.widget.Widget;
import com.gregtechceu.gtceu.client.mui.screen.RichTooltip;
import com.gregtechceu.gtceu.client.mui.screen.viewport.ModularGuiContext;
import com.gregtechceu.gtceu.integration.recipeviewer.RecipeSlotRole;
import com.gregtechceu.gtceu.integration.recipeviewer.entry.EntryList;
import com.gregtechceu.gtceu.integration.recipeviewer.entry.item.ItemStackList;
import com.gregtechceu.gtceu.integration.recipeviewer.handlers.IngredientProvider;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A lightweight client-side widget for displaying item ingredients in recipe viewers (JEI/EMI/REI).
 * Unlike ItemSlot, this does not require a sync handler or inventory backing.
 * It holds a list of ItemStacks for ingredient cycling and implements IngredientProvider.
 */
public class XEIItemWidget extends Widget<XEIItemWidget> implements IngredientProvider<ItemStack> {

    public static final int SIZE = 18;

    private final ItemStackList ingredients;
    private RecipeSlotRole role = RecipeSlotRole.RENDER_ONLY;
    private IDrawable overlay = null;
    private final List<Component> tooltipLines = new ArrayList<>();

    public XEIItemWidget() {
        this.ingredients = new ItemStackList();
        size(SIZE);
    }

    public XEIItemWidget(List<ItemStack> stacks) {
        this.ingredients = ItemStackList.of(stacks);
        size(SIZE);
    }

    public XEIItemWidget ingredient(ItemStack stack) {
        this.ingredients.add(stack);
        return this;
    }

    public XEIItemWidget ingredients(List<ItemStack> stacks) {
        this.ingredients.addAll(stacks);
        return this;
    }

    public XEIItemWidget ingredients(ItemStack... stacks) {
        this.ingredients.addAll(Arrays.asList(stacks));
        return this;
    }

    public XEIItemWidget role(RecipeSlotRole role) {
        this.role = role;
        return this;
    }

    public XEIItemWidget overlay(IDrawable overlay) {
        this.overlay = overlay;
        return this;
    }

    public XEIItemWidget addTooltipLine(Component line) {
        this.tooltipLines.add(line);
        return this;
    }

    @Override
    public void onInit() {
        size(SIZE);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> widgetTheme) {
        List<ItemStack> stacks = ingredients.getStacks();
        if (!stacks.isEmpty()) {
            // Cycle through ingredients based on time for visual display
            long tick = System.currentTimeMillis() / 1000;
            int index = (int) (tick % stacks.size());
            ItemStack display = stacks.get(index);
            if (!display.isEmpty()) {
                GuiDraw.drawItem(context.getGraphics(), display, 1, 1, 16, 16, context.getCurrentDrawingZ());
                if (display.getCount() > 1) {
                    GuiDraw.drawStandardSlotAmountText(context, display.getCount(), null, getArea(),
                            context.getCurrentDrawingZ());
                }
            }
        }
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
    protected WidgetThemeEntry<?> getWidgetThemeInternal(ITheme theme) {
        return theme.getItemSlotTheme();
    }

    @Override
    public @NotNull EntryList<ItemStack> getIngredients() {
        return ingredients;
    }

    @Override
    public @NotNull Class<ItemStack> ingredientClass() {
        return ItemStack.class;
    }

    @Override
    public @NotNull RecipeSlotRole recipeRole() {
        return role;
    }
}
