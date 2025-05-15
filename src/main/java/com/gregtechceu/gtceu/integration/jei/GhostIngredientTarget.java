package com.gregtechceu.gtceu.integration.jei;

import com.gregtechceu.gtceu.api.mui.base.widget.IGuiElement;
import com.gregtechceu.gtceu.api.mui.base.widget.IWidget;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Rectangle;
import com.gregtechceu.gtceu.integration.xei.handlers.GhostIngredientSlot;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import net.minecraft.client.renderer.Rect2i;
import org.jetbrains.annotations.NotNull;

public class GhostIngredientTarget<I> implements IGhostIngredientHandler.Target<I> {

    private final IGuiElement guiElement;
    private final GhostIngredientSlot<I> ghostSlot;

    public static <I> GhostIngredientTarget<I> of(GhostIngredientSlot<I> slot) {
        if (slot instanceof IGuiElement guiElement) {
            return new GhostIngredientTarget<>(guiElement, slot);
        }
        throw new IllegalArgumentException();
    }

    public static <I, W extends IWidget & GhostIngredientSlot<I>> GhostIngredientTarget<I> of(W slot) {
        return new GhostIngredientTarget<>(slot, slot);
    }

    public GhostIngredientTarget(IGuiElement guiElement, GhostIngredientSlot<I> ghostSlot) {
        this.guiElement = guiElement;
        this.ghostSlot = ghostSlot;
    }

    @Override
    public @NotNull Rect2i getArea() {
        return this.guiElement.getArea().asVanillaRect();
    }

    @Override
    public void accept(@NotNull I ingredient) {
        ingredient = this.ghostSlot.castGhostIngredientIfValid(ingredient);
        if (ingredient == null) {
            throw new IllegalStateException("Ghost slot did accept ingredient before, but now it doesn't.");
        }
        this.ghostSlot.setGhostIngredient(ingredient);
    }
}
