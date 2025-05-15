package com.gregtechceu.gtceu.integration.xei.handlers;

import com.gregtechceu.gtceu.api.mui.base.JeiSettings;
import com.gregtechceu.gtceu.api.mui.base.widget.IWidget;
import com.gregtechceu.gtceu.api.mui.drawable.GuiDraw;
import com.gregtechceu.gtceu.api.mui.utils.Color;
import com.gregtechceu.gtceu.api.mui.widget.Widget;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Rectangle;
import mezz.jei.gui.ghost.GhostIngredientDrag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * An interface for compat with JEI's ghost slots.
 * Implement this on any {@link IWidget}.
 * This slot must then be manually registered in something like {@link Widget#onInit()}
 * with {@link JeiSettings#addGhostIngredientSlot(IWidget) JeiSettings.addGhostIngredientSlot(IWidget)}
 *
 * @param <I> type of the ingredient
 */
public interface GhostIngredientSlot<I> {

    /**
     * Puts the ingredient in this ghost slot.
     * Was cast with {@link #castGhostIngredientIfValid(Object)}.
     *
     * @param ingredient ingredient to put
     */
    void setGhostIngredient(@NotNull I ingredient);

    /**
     * Tries to cast an ingredient to the type of this slot.
     * Returns null if the ingredient can't be cast.
     * Must be consistent.
     *
     * @param ingredient ingredient to cast
     * @return cast ingredient or null
     */
    @Nullable
    I castGhostIngredientIfValid(@NotNull Object ingredient);

    default void drawHighlight(Rectangle area, boolean hovering) {
        int color = hovering ? Color.argb(76, 201, 25, 128) : Color.argb(19, 201, 10, 64);
        GuiDraw.drawRect(0, 0, area.width, area.height, color);
    }

    static <T> boolean insertGhostIngredient(GhostIngredientDrag<?> drag, GhostIngredientSlot<T> slot) {
        T t = slot.castGhostIngredientIfValid(drag.getIngredient());
        if (t != null) {
            slot.setGhostIngredient(t);
            return true;
        }
        return false;
    }
}
