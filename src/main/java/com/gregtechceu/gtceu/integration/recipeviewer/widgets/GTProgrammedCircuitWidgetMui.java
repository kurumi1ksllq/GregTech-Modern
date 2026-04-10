package com.gregtechceu.gtceu.integration.recipeviewer.widgets;

import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import net.minecraftforge.items.ItemStackHandler;

import brachy.modularui.integration.recipeviewer.RecipeSlotRole;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.layout.Grid;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

public class GTProgrammedCircuitWidgetMui extends ParentWidget<GTProgrammedCircuitWidgetMui> {

    public GTProgrammedCircuitWidgetMui() {
        super();
        size(150, 80);

        ItemStackHandler handler = new CustomItemStackHandler(32);

        Grid circuits = new Grid()
                .coverChildren()
                .mapTo(8, 4, i -> new ItemSlot().recipeRole(RecipeSlotRole.INPUT)
                        .slot(new ModularSlot(handler, i).accessibility(false, false)));

        child(circuits.center());
    }
}
