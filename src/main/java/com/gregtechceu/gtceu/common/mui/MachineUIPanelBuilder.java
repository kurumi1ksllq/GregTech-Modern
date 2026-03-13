package com.gregtechceu.gtceu.common.mui;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.trait.feature.IAttachConfiguratorsTrait;
import com.gregtechceu.gtceu.api.mui.drawable.UITexture;
import com.gregtechceu.gtceu.api.mui.theme.ThemeAPI;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.value.sync.PanelSyncManager;
import com.gregtechceu.gtceu.api.mui.widget.ParentWidget;
import com.gregtechceu.gtceu.api.mui.widgets.SlotGroupWidget;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Flow;
import com.gregtechceu.gtceu.client.mui.screen.ModularPanel;
import com.gregtechceu.gtceu.common.data.mui.GTMuiWidgets;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.Consumer;

@Accessors(fluent = true)
@Setter
public class MachineUIPanelBuilder {

    public static final int DEFAULT_WIDTH = 172;
    public static final int DEFAULT_HEIGHT = 77;

    /**
     * Should the GregTech logo be drawn in the bottom right corner of the panel.
     */
    private boolean drawGTLogo = false;
    /**
     * Should the player inventory be attached to the bottom of the panel.
     */
    private boolean attachInventory = true;
    /**
     * Should a fancy title bar be created for this panel.
     */
    private boolean addTitleBar = true;
    /**
     * If machine traits should be allowed to attach configurators to the sides of the panel.
     */
    private boolean addTraitConfigurators = true;
    /**
     * If the default configurators (circuit slot, battery slot, power button) should be added to this machine, provided the machine supports them.
     */
    private boolean addDefaultConfigurators = true;
    private final MetaMachine machine;
    private final PanelSyncManager syncManager;

    private Consumer<Flow> leftConfigurators = (f) -> {};
    private Consumer<Flow> rightConfigurators = (f) -> {};
    private Consumer<ParentWidget<?>> mainContents = (p)-> {};

    protected MachineUIPanelBuilder(MetaMachine machine, PanelSyncManager syncManager) {
        this.machine = machine;
        this.syncManager = syncManager;
    }

    public ModularPanel build() {

        var panel = new ModularPanel(machine.getDefinition().getId().getPath());

        Flow attachLeft = Flow.col()
                .coverChildren()
                .rightRel(1.0f)
                .reverseLayout(true)
                .padding(4, 2, 4, 4)
                .bottom(16)
                .excludeAreaInRecipeViewer()
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .background(GTGuiTextures.BACKGROUND.getSubArea(0f, 0f, 0.75f, 1.0f))
                .setEnabledIf(f -> !f.getChildren().isEmpty());

        Flow attachRight = Flow.col()
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

        ParentWidget<?> attachMain = new ParentWidget<>()
                .margin(4);

        panel.relative(attachMain)
                .widthRelOffset(1, 4)
                .heightRelOffset(1, attachInventory ? 89 : 4);

        panel.child(attachMain);
        panel.child(attachLeft);
        panel.child(attachRight);

        if (addDefaultConfigurators) {
            if (machine instanceof IHasCircuitSlot circuitSlot && circuitSlot.isCircuitSlotEnabled()) {
                attachLeft.child(GTMuiWidgets.createCircuitSlotPanel(circuitSlot, panel, syncManager));
            }

            if (machine instanceof IControllable controllable) {
                attachRight.child(GTMuiWidgets.createPowerButton(controllable, syncManager));
            }
            if (machine instanceof SimpleTieredMachine simpleTieredMachine) {
                attachRight.child(GTMuiWidgets.createBatterySlot(simpleTieredMachine, syncManager));
            }
        }

        leftConfigurators.accept(attachLeft);
        rightConfigurators.accept(attachRight);
        mainContents.accept(attachMain);

        var uiTheme = ThemeAPI.INSTANCE.getTheme(machine.getDefinition().getThemeId());
        panel.childIf(addTitleBar, () -> GTMuiWidgets.createTitleBar(machine.getDefinition(), attachMain.getArea().width, (UITexture) uiTheme.getPanelTheme().getTheme()
                .getBackground()));
        panel.childIf(attachInventory, () -> SlotGroupWidget.playerInventory(false).left(7).bottom(7));
        panel.childIf(drawGTLogo, () -> GTMuiWidgets.createGTLogo()
                .right(7).bottom(7 + (attachInventory ? 78 : 0)));

        if (addTraitConfigurators) {
            for (var trait: machine.getTraitHolder().getAllTraits()) {
                if (trait instanceof IAttachConfiguratorsTrait attachConfiguratorsTrait) {
                    attachConfiguratorsTrait.attachLeftConfigurators(attachLeft, panel, syncManager);
                    attachConfiguratorsTrait.attachRightConfigurators(attachRight, panel, syncManager);
                }
            }
        }

        return panel;
    }

    public static MachineUIPanelBuilder defaultSimpleSingleblockMachinePanel(MetaMachine machine, PanelSyncManager syncManager) {
        return new MachineUIPanelBuilder(machine, syncManager).drawGTLogo(true);
    }

    public static MachineUIPanelBuilder defaultMachinePanel(MetaMachine machine, PanelSyncManager syncManager) {
        return new MachineUIPanelBuilder(machine, syncManager);
    }
}