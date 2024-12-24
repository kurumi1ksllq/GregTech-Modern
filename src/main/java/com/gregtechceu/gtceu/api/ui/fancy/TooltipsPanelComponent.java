package com.gregtechceu.gtceu.api.ui.fancy;

import com.gregtechceu.gtceu.api.ui.base.BaseUIComponent;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TooltipsPanelComponent extends BaseUIComponent {

    @Getter
    protected List<IFancyTooltip> tooltips = new ArrayList<>();

    public TooltipsPanelComponent() {
        positioning(Positioning.absolute(202, 2));
        sizing(Sizing.fixed(20), Sizing.fixed(20));
    }

    public void clear() {
        tooltips.clear();
    }

    public void attachTooltips(IFancyTooltip... tooltips) {
        this.tooltips.addAll(Arrays.asList(tooltips));
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        int offsetY = 0;
        for (IFancyTooltip tooltip : this.tooltips) {
            if (tooltip.showFancyTooltip()) {
                if (UIComponent.isMouseOver(x, y + offsetY, width, height, mouseX, mouseY)) {
                    loadTooltip(tooltip);
                    return;
                }
                offsetY += width + 2;
            }
        }
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        int offsetY = 0;
        for (IFancyTooltip tooltip : this.tooltips) {
            if (tooltip.showFancyTooltip()) {
                // draw icon
                tooltip.getFancyTooltipIcon().draw(graphics, mouseX, mouseY, x, y + offsetY,
                        width, height);
                offsetY += width() + 2;
            }
        }
        // sizing(horizontalSizing.get(), Sizing.fixed(Math.max(0, offsetY)));
        // applySizing();
    }

    protected void loadTooltip(IFancyTooltip tab) {
        List<ClientTooltipComponent> tooltip = new ArrayList<>();
        tab.getFancyTooltip().stream()
                .map(c -> ClientTooltipComponent.create(c.getVisualOrderText()))
                .forEach(tooltip::add);
        if (tab.getFancyComponent() != null) {
            tooltip.add(ClientTooltipComponent.create(tab.getFancyComponent()));
        }
        this.tooltip(tooltip);
    }
}
