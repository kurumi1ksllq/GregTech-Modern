package com.gregtechceu.gtceu.api.mui.widgets.slot;

import com.gregtechceu.gtceu.api.mui.base.widget.Interactable;
import com.gregtechceu.gtceu.api.mui.drawable.InteractableIcon;
import com.gregtechceu.gtceu.api.mui.drawable.text.TextRenderer;
import com.gregtechceu.gtceu.api.mui.utils.Color;
import com.gregtechceu.gtceu.api.mui.value.sync.FluidTankSyncHandler;
import com.gregtechceu.gtceu.api.mui.widget.Widget;
import net.minecraftforge.fluids.IFluidTank;

public class FluidTank extends Widget<FluidTank> implements Interactable {

    private final TextRenderer textRenderer = new TextRenderer();
    private boolean disableBackground = false;
    private FluidTankSyncHandler syncHandler;

    public FluidTank() {
        tooltip().titleMargin();
    }

    public static FluidTankSyncHandler sync(IFluidTank tank) {
        return new FluidTankSyncHandler(tank);
    }

    @Override
    public void onInit() {
        this.textRenderer.setShadow(true);
        this.textRenderer.setScale(0.5f);
        this.textRenderer.setColor(Color.WHITE.main);

        tooltipBuilder(syncHandler::handleT);
        syncHandler.setChangeCon($ -> markTooltipDirty());
    }
}
