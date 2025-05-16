package com.gregtechceu.gtceu.integration.xei.handlers;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.mui.screen.ScreenWrapper;
import com.gregtechceu.gtceu.integration.emi.handler.EMIScreenHandler;
import com.gregtechceu.gtceu.integration.jei.handler.JEIScreenHandler;
import com.gregtechceu.gtceu.integration.rei.handler.REIScreenHandler;
import org.jetbrains.annotations.Nullable;

public abstract class RecipeViewerHandler {

    private static boolean isHandlerInit = false;
    private static RecipeViewerHandler current = null;

    @Nullable
    public static RecipeViewerHandler getCurrent() {
        if (!isHandlerInit) {
            if (!GTCEu.Mods.isRecipeViewerLoaded()) {
                isHandlerInit = true;
                return current;
            }

            if (GTCEu.Mods.isEMILoaded()) {
                current = EMIScreenHandler.of(ScreenWrapper.class);
            } else if (GTCEu.Mods.isREILoaded()) {
                current = REIScreenHandler.of(ScreenWrapper.class);
            } else if (GTCEu.Mods.isJEILoaded()) {
                current = JEIScreenHandler.of(ScreenWrapper.class);
            }
            isHandlerInit = true;
        }
        return current;
    }

    public abstract void stopDrag();

    public abstract @Nullable Object getCurrentlyDragged();

    public boolean isDraggingGhostIngredient() {
        return getCurrentlyDragged() != null;
    }

    public boolean isHoveringOver(GhostIngredientSlot<?> slot) {
        if (getCurrentlyDragged() == null) return false;
        return slot.castGhostIngredientIfValid(getCurrentlyDragged()) != null;
    }

}
