package com.gregtechceu.gtceu.integration.jei.handler;

import com.gregtechceu.gtceu.api.mui.base.IScreenWithMuiScreen;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Area;
import com.gregtechceu.gtceu.client.mui.screen.ClientScreenHandler;
import com.gregtechceu.gtceu.client.mui.screen.ScreenWrapper;
import com.gregtechceu.gtceu.integration.jei.GTJEIPlugin;
import com.gregtechceu.gtceu.integration.xei.handlers.GhostIngredientSlot;
import com.gregtechceu.gtceu.integration.xei.handlers.RecipeViewerHandler;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.gui.handlers.IGuiProperties;
import mezz.jei.api.gui.handlers.IScreenHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.gui.GuiProperties;
import mezz.jei.gui.input.handlers.DragRouter;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JEIScreenHandler<T extends Screen & IScreenWithMuiScreen> extends RecipeViewerHandler implements IGhostIngredientHandler<T>, IScreenHandler<T> {

    private static final Map<Class<?>, JEIScreenHandler<?>> CACHE = new Reference2ReferenceOpenHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends Screen & IScreenWithMuiScreen> JEIScreenHandler<T> of(Class<T> cls) {
        return (JEIScreenHandler<T>) CACHE.computeIfAbsent(cls, c -> new JEIScreenHandler<T>());
    }

    private JEIScreenHandler() {}

    @Override
    public <I> @NotNull List<IGhostIngredientHandler.Target<I>> getTargetsTyped(T screen,
                                                                                @NotNull ITypedIngredient<I> ingredient,
                                                                                boolean doStart) {
        currentIngredient = ingredient;

        List<GhostIngredientSlot<?>> ghostSlots = screen.getScreen().getContext()
                .getXeiSettings().getGhostIngredientSlots();
        List<Target<I>> ghostHandlerTargets = new ArrayList<>();
        for (var slot : ghostSlots) {
            if (slot.isEnabled() && slot.castGhostIngredientIfValid(ingredient.getIngredient()) != null) {
                @SuppressWarnings("unchecked")
                GhostIngredientSlot<I> slotWithType = (GhostIngredientSlot<I>) slot;
                ghostHandlerTargets.add(new GhostIngredientTarget<>(slotWithType));
            }
        }
        return ghostHandlerTargets;
    }

    @Override
    public void onComplete() {
        currentIngredient = null;
    }

    @Override
    public @Nullable IGuiProperties apply(@NotNull T screen) {
        if (ClientScreenHandler.shouldUnfocusRecipeViewer) {
            return null;
        } else {
            Area area = screen.getScreen().getMainPanel().getArea();
            Area screenArea = screen.getScreen().getContext().getScreenArea();
            return new GuiProperties(
                    ScreenWrapper.class,
                    area.getX(), area.getY(), area.getWidth(), area.getHeight(),
                    screenArea.getWidth(), screenArea.getHeight()
            );
        }
    }

    static ITypedIngredient<?> currentIngredient = null;
    public static DragRouter dragRouter = null;

    @Override
    public void stopDrag() {
        if (dragRouter != null) {
            dragRouter.cancelDrag();
        }
    }

    @Override
    public @Nullable Object getCurrentlyDragged() {
        if (currentIngredient == null) return null;
        return currentIngredient.getIngredient();
    }
}
