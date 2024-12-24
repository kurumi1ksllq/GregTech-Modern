package com.gregtechceu.gtceu.integration.emi.handler.widget;

import net.minecraft.client.gui.GuiGraphics;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.SlotWidget;

public class NoRenderEMISlotWidget extends SlotWidget {

    public NoRenderEMISlotWidget(EmiIngredient stack, int x, int y) {
        super(stack, x, y);
    }

    @Override
    public void render(GuiGraphics draw, int mouseX, int mouseY, float delta) {
        // only render the overlays.
        this.drawOverlay(draw, mouseX, mouseY, delta);
    }
}
