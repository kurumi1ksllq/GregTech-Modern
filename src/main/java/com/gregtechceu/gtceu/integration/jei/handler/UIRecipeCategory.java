package com.gregtechceu.gtceu.integration.jei.handler;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.ui.component.SlotComponent;
import com.gregtechceu.gtceu.api.ui.component.TankComponent;
import com.gregtechceu.gtceu.api.ui.core.ParentUIComponent;
import com.gregtechceu.gtceu.api.ui.core.Size;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.ingredient.ClickableIngredientSlot;
import com.gregtechceu.gtceu.integration.jei.GTJEIPlugin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.ingredients.IIngredientRenderer;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.category.IRecipeCategory;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public abstract class UIRecipeCategory<T extends UIComponent> implements IRecipeCategory<T> {

    private static <T> void addJEISlot(IRecipeLayoutBuilder builder, final ClickableIngredientSlot<T> slot,
                                       RecipeIngredientRole role, int index) {
        var slotName = "slot." + index;
        var slotBuilder = builder.addSlot(role, slot.x(), slot.y());
        // append ingredients
        final var ingredientMap = new HashMap<IIngredientType<T>, List<T>>();

        var override = slot.ingredientOverride();
        if (override != null) {
            Optional<ITypedIngredient<T>> maybeIngredient = GTJEIPlugin.JEI_RUNTIME
                    .getIngredientManager()
                    .createTypedIngredient((T) override);
            maybeIngredient.ifPresent(i -> {
                ingredientMap.computeIfAbsent(i.getType(), t -> new ArrayList<>()).add(i.getIngredient());
            });
        } else {
            for (T ingredient : slot.getIngredients().getStacks()) {
                Optional<ITypedIngredient<T>> maybeIngredient = GTJEIPlugin.JEI_RUNTIME
                        .getIngredientManager()
                        .createTypedIngredient(slot.renderMappingFunction().apply(ingredient));
                maybeIngredient.ifPresent(i -> {
                    ingredientMap.computeIfAbsent(i.getType(), t -> new ArrayList<>()).add(i.getIngredient());
                });
            }
        }
        for (var entry : ingredientMap.entrySet()) {
            var type = entry.getKey();
            var ingredients = entry.getValue();
            slotBuilder.addIngredients(type, ingredients);
            slotBuilder.setCustomRenderer(type, new IIngredientRenderer<>() {

                @Override
                public void render(GuiGraphics guiGraphics, T ingredient) {
                    //GTJEIPlugin.JEI_RUNTIME.getIngredientManager().getIngredientRenderer(type)
                    //        .render(guiGraphics, ingredient);
                    // slot.setCurrentJEIRenderedIngredient(ingredient);
                }

                @SuppressWarnings("removal")
                @Override
                public @NotNull List<Component> getTooltip(T ingredient, TooltipFlag tooltipFlag) {
                    return Collections.emptyList();
                }

                @Override
                public void getTooltip(ITooltipBuilder tooltip, T ingredient, TooltipFlag tooltipFlag) {
                    // empty, the original slot draws its tooltip itself.
                    // tooltip.addAll(slot.getFullTooltipTexts());
                }

                @Override
                public int getWidth() {
                    return slot.width();
                }

                @Override
                public int getHeight() {
                    return slot.height();
                }
            });
        }
        // set slot name
        slotBuilder.setSlotName(slotName);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, T component, IFocusGroup focuses) {
        // inflate up to a sane default
        component.inflate(Size.of(200, 200));

        List<UIComponent> flatVisibleWidgetCollection = getFlatWidgetCollection(component);
        for (int i = 0; i < flatVisibleWidgetCollection.size(); i++) {
            var widget = flatVisibleWidgetCollection.get(i);
            if (widget instanceof ClickableIngredientSlot<?> slot) {
                if (slot.ingredientIO() == null) {
                    continue;
                }
                var role = mapToRole(slot.ingredientIO());
                if (role == null) { // both
                    addJEISlot(builder, slot, RecipeIngredientRole.INPUT, i);
                    addJEISlot(builder, slot, RecipeIngredientRole.OUTPUT, i);
                } else {
                    addJEISlot(builder, slot, role, i);
                }
            }
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, T component, IFocusGroup focuses) {
        JEIUIAdapter adapter = new JEIUIAdapter(component.area());
        adapter.rootComponent().child(component);
        adapter.prepare();

        builder.addWidget(adapter);
        builder.addGuiEventListener(adapter);
    }

    @Override
    public void draw(T recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX,
                     double mouseY) {
        if (!(guiGraphics instanceof UIGuiGraphics)) guiGraphics = UIGuiGraphics.of(guiGraphics);
        var graphics = (UIGuiGraphics) guiGraphics;
        recipe.draw(graphics, (int) mouseX, (int) mouseY, Minecraft.getInstance().getPartialTick(),
                Minecraft.getInstance().getDeltaFrameTime());
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, T recipe, IRecipeSlotsView recipeSlotsView, double mouseX,
                           double mouseY) {
        IRecipeCategory.super.getTooltip(tooltip, recipe, recipeSlotsView, mouseX, mouseY);
        // TODO get tooltips? maybe? or not, since we draw them in the method above anyway
    }

    public List<UIComponent> getFlatWidgetCollection(T widgetIn) {
        List<UIComponent> widgetList = new ArrayList<>();
        if (widgetIn instanceof ParentUIComponent group) {
            group.collectDescendants(widgetList);
        } else {
            widgetList.add(widgetIn);
        }
        return widgetList;
    }

    @Nullable
    public static RecipeIngredientRole mapToRole(IO ingredientIO) {
        return switch (ingredientIO) {
            case IN -> RecipeIngredientRole.INPUT;
            case OUT -> RecipeIngredientRole.OUTPUT;
            case BOTH -> null;
            case NONE -> RecipeIngredientRole.CATALYST;
            // TODO
            // case RENDER_ONLY -> RecipeIngredientRole.RENDER_ONLY;
        };
    }
}
