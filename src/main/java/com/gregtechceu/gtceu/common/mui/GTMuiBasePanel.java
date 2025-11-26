package com.gregtechceu.gtceu.common.mui;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IMuiMachine;
import com.gregtechceu.gtceu.api.mui.base.drawable.IKey;
import com.gregtechceu.gtceu.api.mui.factory.PosGuiData;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.value.sync.PanelSyncManager;
import com.gregtechceu.gtceu.api.mui.widget.ParentWidget;
import com.gregtechceu.gtceu.api.mui.widget.Widget;
import com.gregtechceu.gtceu.api.mui.widgets.SlotGroupWidget;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Column;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Flow;
import com.gregtechceu.gtceu.client.mui.screen.ModularPanel;
import com.gregtechceu.gtceu.client.mui.screen.UISettings;
import com.gregtechceu.gtceu.common.data.mui.GTMuiWidgets;

public interface GTMuiBasePanel extends IMuiMachine {

    default Widget<?> createTitleBar(){
        if(this instanceof MetaMachine machine){
            return GTMuiWidgets.createTitleBar(machine.getDefinition(), 176);
        }
        throw new IllegalStateException("GTMuiBasePanel.createTitleBar should not be called unless instanceof MetaMachine");
    }

    default String getDefinitionName(){
        if(this instanceof MetaMachine machine){
            return machine.getDefinition().getName();
        }
        return this.getClass().getName();
    }

    @Override
    default public ModularPanel buildUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        // TODO: Allow more customization, extract more stuff into methods
        // TODO: How do we wanna handle width/height?
        // TODO: default createAutoOutputItemFluidButtons?
        return new ModularPanel(getDefinitionName())
                .size(176, 100)
                .childIf(this instanceof MetaMachine, createTitleBar())
                .child(new ParentWidget<>()
                        .widthRel(1)
                        .child(Flow.row()
                                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                                .align(Alignment.CENTER)
                                .coverChildren()
                                .child(innerUI(data, syncManager, settings))))
                .child(SlotGroupWidget.playerInventory(false).left(7).bottom(7))
                .child(new Column()
                        .coverChildren()
                        .leftRel(1.0f)
                        .reverseLayout(true)
                        .bottom(16)
                        .padding(0, 8, 4, 4)
                        .childPadding(2)
                        .background(GTGuiTextures.BACKGROUND.getSubArea(0.25f, 0f, 1.0f, 1.0f))
                        /*
                        .child(createAutoOutputItemButton(syncManager))
                        .child(createAutoOutputFluidButton(syncManager))
                        .child(createInputFromOutputItem(syncManager))
                        .child(createInputFromOutputFluid(syncManager))
                         */
                        .excludeAreaInXei());
    }

    default public Widget<?> innerUI(PosGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return IKey.str("Default widget, override innerUI").asWidget();
    }
}
