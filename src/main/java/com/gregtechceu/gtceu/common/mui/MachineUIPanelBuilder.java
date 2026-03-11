package com.gregtechceu.gtceu.common.mui;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.widget.ParentWidget;
import com.gregtechceu.gtceu.api.mui.widgets.SlotGroupWidget;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Flow;
import com.gregtechceu.gtceu.client.mui.screen.ModularPanel;
import com.gregtechceu.gtceu.common.data.mui.GTMuiWidgets;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.BiConsumer;

@Accessors(fluent = true)
@Setter
public class MachineUIPanelBuilder {

    @Setter
    private int width = 176;
    @Setter
    private int height = 166;

    private boolean drawGTLogo = false;
    private boolean attachInventory = true;
    private boolean addTitleBar = true;

    private final MetaMachine machine;

    private BiConsumer<Flow, ModularPanel> leftConfigurators = (f, p) -> {};
    private BiConsumer<Flow, ModularPanel> rightConfigurators = (f, p) -> {};
    private BiConsumer<ParentWidget<?>, ModularPanel> mainContents = (f, p)-> {};

    protected MachineUIPanelBuilder(MetaMachine machine) {
        this.machine = machine;
    }

    public static MachineUIPanelBuilder defaultSimpleSingleblockMachinePanel(MetaMachine machine) {
        return new MachineUIPanelBuilder(machine).drawGTLogo(true);
    }

    public static MachineUIPanelBuilder defaultMachinePanel(MetaMachine machine) {
        return new MachineUIPanelBuilder(machine);
    }

    public ModularPanel build() {
        var panel = new ModularPanel(machine.getDefinition().getId().getPath());
        panel.size(width, height);

        var attachLeft = Flow.col()
                .coverChildren()
                .rightRel(1.0f)
                .reverseLayout(true)
                .padding(4, 2, 4, 4)
                .bottom(16)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .background(GTGuiTextures.BACKGROUND.getSubArea(0f, 0f, 0.75f, 1.0f))
                .setEnabledIf(f -> !f.getChildren().isEmpty());


        var attachRight = Flow.col()
                .coverChildren()
                .leftRel(1.0f)
                .reverseLayout(true)
                .padding(2, 4, 4, 4)
                .bottom(16)
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .childPadding(2)
                .background(GTGuiTextures.BACKGROUND.getSubArea(0.25f, 0f, 1.0f, 1.0f))
                .setEnabledIf(f -> !f.getChildren().isEmpty());

        var main = new ParentWidget<>()
                .margin(4)
                .widthRelOffset(1,-4)
                .heightRelOffset(1, attachInventory ? -89 : -4);

        leftConfigurators.accept(attachLeft, panel);
        rightConfigurators.accept(attachRight, panel);
        mainContents.accept(main, panel);

        panel.child(attachLeft);
        panel.child(attachRight);
        panel.child(main);
        panel.childIf(addTitleBar, () -> GTMuiWidgets.createTitleBar(machine.getDefinition(), width));
        panel.childIf(attachInventory, () -> SlotGroupWidget.playerInventory(false).left(7).bottom(7));
        panel.childIf(drawGTLogo, () -> GTMuiWidgets.createGTLogo()
                .right(7).bottom(7 + (attachInventory ? 78 : 0)));

        return panel;
    }
}
