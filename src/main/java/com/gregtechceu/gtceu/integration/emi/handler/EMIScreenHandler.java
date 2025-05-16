package com.gregtechceu.gtceu.integration.emi.handler;

import com.gregtechceu.gtceu.api.mui.base.IScreenWithMuiScreen;
import com.gregtechceu.gtceu.api.mui.base.widget.IGuiElement;
import com.gregtechceu.gtceu.integration.xei.handlers.GhostIngredientSlot;
import com.gregtechceu.gtceu.integration.xei.handlers.RecipeViewerHandler;
import com.gregtechceu.gtceu.integration.xei.handlers.IngredientProvider;
import com.gregtechceu.gtceu.utils.GTMath;
import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.EmiExclusionArea;
import dev.emi.emi.api.EmiStackProvider;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.EmiStackInteraction;
import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.registry.EmiDragDropHandlers;
import dev.emi.emi.screen.EmiScreenManager;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EMIScreenHandler<T extends Screen & IScreenWithMuiScreen> extends RecipeViewerHandler implements EmiExclusionArea<T>, EmiDragDropHandler<T>, EmiStackProvider<T> {

    private static final Map<Class<?>, EMIScreenHandler<?>> CACHE = new Reference2ReferenceOpenHashMap<>();

    @SuppressWarnings("unchecked")
    public static <T extends Screen & IScreenWithMuiScreen> EMIScreenHandler<T> of(Class<T> cls) {
        return (EMIScreenHandler<T>) CACHE.computeIfAbsent(cls, c -> new EMIScreenHandler<T>());
    }

    private EMIScreenHandler() {}

    @Override
    public boolean dropStack(T screen, EmiIngredient stack, int x, int y) {
        List<GhostIngredientSlot<?>> ghostSlots = screen.getScreen().getContext()
                .getXeiSettings().getGhostIngredientSlots();

        var stacks = stack.getEmiStacks();
        if (stacks.isEmpty()) return false;
        for (EmiStack emiStack : stacks) {
            for (GhostIngredientSlot<?> slot : ghostSlots) {
                if (!slot.isEnabled() || !slot.getArea().contains(x, y)) {
                    continue;
                }
                if (slot.ingredientHandlingOverride(emiStack)) {
                    return true;
                }
                EmiStackConverter.Converter<?> converter = EmiStackConverter
                        .getForNullable(slot.ingredientClass());
                if (converter == null) {
                    continue;
                }
                var converted = converter.convertFrom(emiStack);
                if (converted != null) {
                    // noinspection unchecked,rawtypes
                    ((GhostIngredientSlot) slot).setGhostIngredient(converted);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void addExclusionArea(T screen, Consumer<Bounds> consumer) {
        screen.getScreen().getContext()
                .getXeiSettings().getAllExclusionAreas()
                .stream()
                .map(rect -> new Bounds(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight()))
                .forEach(consumer);
    }

    @Override
    public EmiStackInteraction getStackAt(T screen, int x, int y) {
        IGuiElement hovered = screen.getScreen().getContext().getHovered();
        if (hovered instanceof IngredientProvider<?> provider) {
            var override = provider.ingredientOverride();
            if (override != null) {
                currentIngredient = (EmiIngredient) override;
                return new EmiStackInteraction(currentIngredient);
            }

            EmiStackConverter.Converter<?> converter = EmiStackConverter.getForNullable(provider.ingredientClass());
            if (converter == null) {
                return EmiStackInteraction.EMPTY;
            }
            @SuppressWarnings({ "rawtypes", "unchecked" })
            var converted = ((EmiStackConverter.Converter) converter).convertTo(provider);
            return new EmiStackInteraction(converted, null, false);
        }
        return EmiStackInteraction.EMPTY;
    }

    static EmiIngredient currentIngredient = null;

    /**
     * Someone's going to be mad at me for doing it like this.
     * @author screret
     */
    @Override
    public void stopDrag() {
        EmiScreenManager.pressedStack = EmiStack.EMPTY;
        EmiScreenManager.draggedStack = EmiStack.EMPTY;
    }

    @Override
    public @Nullable Object getCurrentlyDragged() {
        if (currentIngredient == null || currentIngredient.isEmpty()) return null;

        EmiStack stack = currentIngredient.getEmiStacks().get(0);
        if (stack.getKeyOfType(Item.class) != null) {
            Item item = stack.getKeyOfType(Item.class);
            if (item == null || item == Items.AIR) return null;

            ItemStack itemStack = new ItemStack(item, GTMath.saturatedCast(stack.getAmount()));
            itemStack.setTag(stack.getNbt());
            return itemStack;
        } else if (stack.getKeyOfType(Fluid.class) != null) {
            Fluid fluid = stack.getKeyOfType(Fluid.class);
            if (fluid == null || fluid == Fluids.EMPTY) return null;

            FluidStack fluidStack = new FluidStack(fluid, GTMath.saturatedCast(stack.getAmount()));
            fluidStack.setTag(stack.getNbt());
            return fluidStack;
        } else {
            return null;
        }
    }
}
