package com.gregtechceu.gtceu.integration.recipeviewer.widgets;

import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import com.gregtechceu.gtceu.common.item.behavior.IntCircuitBehaviour;

import brachy.modularui.integration.recipeviewer.RecipeSlotRole;
import brachy.modularui.widget.ParentWidget;
import brachy.modularui.widgets.layout.Grid;
import brachy.modularui.widgets.slot.ItemSlot;
import brachy.modularui.widgets.slot.ModularSlot;

public class GTProgrammedCircuitWidget extends ParentWidget<GTProgrammedCircuitWidget> {

    public GTProgrammedCircuitWidget() {
        super();
        size(150, 80);

        CustomItemStackHandler handler = new CustomItemStackHandler(32);

        for (int i = 0; i<32; i++) {
            handler.setStackInSlot(i, IntCircuitBehaviour.stack(i+1));
        }

        Grid circuits = new Grid()
                .coverChildren()
                .gridOfSizeWidth(32, 8, (x, y, i) -> new ItemSlot().recipeRole(RecipeSlotRole.INPUT)
                        .slot(new ModularSlot(handler, i).accessibility(false, false)));

        child(circuits.horizontalCenter());
    }
}
