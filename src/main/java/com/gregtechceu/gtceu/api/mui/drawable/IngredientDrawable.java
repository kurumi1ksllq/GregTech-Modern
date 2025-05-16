package com.gregtechceu.gtceu.api.mui.drawable;

import com.gregtechceu.gtceu.api.mui.base.IJsonSerializable;
import com.gregtechceu.gtceu.api.mui.base.drawable.IDrawable;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;
import com.gregtechceu.gtceu.api.mui.theme.WidgetTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class IngredientDrawable implements IDrawable, IJsonSerializable {

    private ItemStack[] items;

    public IngredientDrawable(Ingredient ingredient) {
        this(ingredient.getMatchingStacks());
    }

    public IngredientDrawable(ItemStack... items) {
        setItems(items);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        if (this.items.length == 0) return;
        ItemStack item = this.items[(int) (Minecraft.getSystemTime() % (1000 * this.items.length)) / 1000];
        if (item != null) {
            GuiDraw.drawItem(item, x, y, width, height, context.getCurrentDrawingZ());
        }
    }

    public ItemStack[] getItems() {
        return this.items;
    }

    public void setItems(ItemStack... items) {
        this.items = items;
    }

    public void setItems(Ingredient ingredient) {
        setItems(ingredient.getMatchingStacks());
    }
}
