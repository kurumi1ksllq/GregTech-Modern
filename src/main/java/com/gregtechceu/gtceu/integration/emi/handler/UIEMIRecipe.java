package com.gregtechceu.gtceu.integration.emi.handler;

import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.ui.core.ParentUIComponent;
import com.gregtechceu.gtceu.api.ui.core.Size;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.api.ui.ingredient.ClickableIngredientSlot;
import com.gregtechceu.gtceu.integration.emi.handler.widget.NoRenderEMISlotWidget;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.*;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class UIEMIRecipe<T extends UIComponent> implements EmiRecipe {

    protected T component;
    protected EMIUIAdapter adapter;
    @Getter
    protected List<EmiIngredient> inputs = new ArrayList<>();
    @Getter
    protected List<EmiStack> outputs = new ArrayList<>();
    @Getter
    protected List<EmiIngredient> catalysts = new ArrayList<>();
    @Getter
    protected int displayWidth, displayHeight;

    /**
     * Create a new {@code UIEMIRecipe}.
     * The given component MUST have a constant size.
     *
     * @param componentSupplier the supplier to create the UI from.
     */
    public UIEMIRecipe(Supplier<T> componentSupplier) {
        this.component = componentSupplier.get();
        // inflate up to a sane default
        component.inflate(Size.of(200, 200));

        Bounds bounds = new Bounds(0, 0, component.width(), component.height());
        this.adapter = new EMIUIAdapter(bounds);
        this.adapter.rootComponent().child(component);
        this.adapter.prepare();

        this.displayWidth = this.adapter.adapter.width();
        this.displayHeight = this.adapter.adapter.height();

        for (UIComponent c : getFlatWidgetCollection(component)) {
            if (c instanceof ClickableIngredientSlot<?> slot) {
                var io = slot.ingredientIO();

                EmiIngredient ingredient;
                var override = slot.ingredientOverride();
                if (override != null) {
                    ingredient = (EmiIngredient) override;
                } else {
                    var converter = EmiStackConverter.getForNullable(slot.ingredientClass());
                    if (converter == null) {
                        continue;
                    }
                    // noinspection unchecked,rawtypes
                    ingredient = ((EmiStackConverter.Converter) converter).convertTo(slot);
                }

                if (io == IO.IN || io == IO.BOTH) {
                    inputs.add(ingredient);
                }
                if (io == IO.OUT || io == IO.BOTH) {
                    outputs.add(ingredient.getEmiStacks().get(0));
                }
                if (io == IO.NONE) {
                    catalysts.add(ingredient);
                }
            }
        }
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.add(this.adapter);

        for (UIComponent c : getFlatWidgetCollection(this.component)) {
            if (c instanceof ClickableIngredientSlot<?> slot) {
                var io = slot.ingredientIO();
                if (io != null) {
                    EmiIngredient ingredient;

                    var override = slot.ingredientOverride();
                    if (override != null) {
                        ingredient = (EmiIngredient) override;
                    } else {
                        var converter = EmiStackConverter.getForNullable(slot.ingredientClass());
                        if (converter == null) {
                            continue;
                        }
                        // noinspection unchecked,rawtypes
                        ingredient = ((EmiStackConverter.Converter) converter).convertTo(slot);
                    }

                    // don't render the EMI slot widget at all, use the UI for that.
                    // still need to add the slots though, or EMI will complain.
                    SlotWidget slotWidget = new NoRenderEMISlotWidget(ingredient, c.x(), c.y());
                    if (io == IO.NONE) {
                        slotWidget.catalyst(true);
                    } else if (io == IO.OUT) {
                        slotWidget.recipeContext(this);
                    }
                    widgets.add(slotWidget);
                }
            }
        }
    }

    public List<UIComponent> getFlatWidgetCollection(UIComponent widgetIn) {
        List<UIComponent> widgetList = new ArrayList<>();
        if (widgetIn instanceof ParentUIComponent group) {
            group.collectDescendants(widgetList);
        } else {
            widgetList.add(widgetIn);
        }
        return widgetList;
    }
}
