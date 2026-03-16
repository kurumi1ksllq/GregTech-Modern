package com.gregtechceu.gtceu.api.machine.mui;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.widget.ParentWidget;
import com.gregtechceu.gtceu.api.mui.widgets.SlotGroupWidget;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Flow;
import com.gregtechceu.gtceu.client.mui.screen.ModularPanel;
import com.gregtechceu.gtceu.common.data.mui.GTMuiWidgets;
import com.gregtechceu.gtceu.common.mui.GTGuiTextures;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

public class MachineUIPanel extends ModularPanel {

    public static final int DEFAULT_CONTENT_WIDTH = 169;
    public static final int DEFAULT_CONTENT_HEIGHT = 77;

    @Getter
    protected final Flow leftConfiguratorPanel, rightConfiguratorPanel;
    @Getter
    protected final ParentWidget<?> mainContents;

    public MachineUIPanel(MetaMachine machine, boolean attachPlayerInventory, boolean centerAttachedInventory, boolean addTitleBar, boolean drawGTLogo) {
        super(machine.getDefinition().getId().getPath());

        leftConfiguratorPanel = Flow.col()
                .coverChildren()
                .rightRel(1.0f)
                .reverseLayout(true)
                .padding(4, 2, 4, 4)
                .bottom(16)
                .excludeAreaInRecipeViewer()
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .background(GTGuiTextures.BACKGROUND.getSubArea(0f, 0f, 0.75f, 1.0f))
                .setEnabledIf(f -> !f.getChildren().isEmpty());

        rightConfiguratorPanel = Flow.col()
                .coverChildren()
                .leftRel(1.0f)
                .reverseLayout(true)
                .padding(2, 4, 4, 4)
                .bottom(16)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .childPadding(2)
                .excludeAreaInRecipeViewer()
                .background(GTGuiTextures.BACKGROUND.getSubArea(0.25f, 0f, 1.0f, 1.0f))
                .setEnabledIf(f -> !f.getChildren().isEmpty());

        Flow panelContents = Flow.col().coverChildren();
        panelContents.margin(4);
        mainContents = new ParentWidget<>()
                .size(DEFAULT_CONTENT_WIDTH, DEFAULT_CONTENT_HEIGHT);

        panelContents.child(mainContents);


        if (attachPlayerInventory) {
            panelContents.marginBottom(8);
            var inventory = SlotGroupWidget.playerInventory((index, slot) -> slot);
            panelContents.child(inventory);
        }

        if (addTitleBar) {
            child(GTMuiWidgets.createTitleBar(machine.getDefinition(), 172));
            child(GTMuiWidgets.createGTLogo().right(7).bottom(7 + (attachPlayerInventory ? 78 : 0)));

        }

        if (drawGTLogo) {
            child(GTMuiWidgets.createGTLogo().right(7).bottom(7 + (attachPlayerInventory ? 78 : 0)));
        }

        child(leftConfiguratorPanel);
        child(rightConfiguratorPanel);
        child(panelContents);
        resizer(new CoverSingleChildResizer(this, panelContents));
        center();
    }
}
