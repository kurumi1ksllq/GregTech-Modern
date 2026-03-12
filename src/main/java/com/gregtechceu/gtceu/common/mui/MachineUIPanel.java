package com.gregtechceu.gtceu.common.mui;

import com.gregtechceu.gtceu.api.capability.IWorkable;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.SimpleTieredMachine;
import com.gregtechceu.gtceu.api.machine.feature.IHasCircuitSlot;
import com.gregtechceu.gtceu.api.machine.trait.feature.IAttachConfiguratorsTrait;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.value.sync.PanelSyncManager;
import com.gregtechceu.gtceu.api.mui.widget.ParentWidget;
import com.gregtechceu.gtceu.api.mui.widgets.SlotGroupWidget;
import com.gregtechceu.gtceu.api.mui.widgets.layout.Flow;
import com.gregtechceu.gtceu.client.mui.screen.ModularPanel;
import com.gregtechceu.gtceu.common.data.mui.GTMuiWidgets;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.function.BiConsumer;

public class MachineUIPanel extends ModularPanel {

    public final Flow leftConfigurators, rightConfigurators;
    public final ParentWidget<?> mainContents;

    private MachineUIPanel(MetaMachine machine, int width, int height, boolean attachInventory, boolean addTitleBar, boolean drawGTLogo) {
        super(machine.getDefinition().getId().getPath());
        size(width, height);

        leftConfigurators = Flow.col()
                .coverChildren()
                .rightRel(1.0f)
                .reverseLayout(true)
                .padding(4, 2, 4, 4)
                .bottom(16)
                .excludeAreaInRecipeViewer()
                .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                .background(GTGuiTextures.BACKGROUND.getSubArea(0f, 0f, 0.75f, 1.0f))
                .setEnabledIf(f -> !f.getChildren().isEmpty());

        rightConfigurators = Flow.col()
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

        mainContents = new ParentWidget<>()
                .margin(4)
                .widthRelOffset(1,-4)
                .heightRelOffset(1, attachInventory ? -89 : -4);

        child(leftConfigurators);
        child(rightConfigurators);
        child(mainContents);
        childIf(addTitleBar, () -> GTMuiWidgets.createTitleBar(machine.getDefinition(), width));
        childIf(attachInventory, () -> SlotGroupWidget.playerInventory(false).left(7).bottom(7));
        childIf(drawGTLogo, () -> GTMuiWidgets.createGTLogo()
                .right(7).bottom(7 + (attachInventory ? 78 : 0)));
    }

    @Accessors(fluent = true)
    @Setter
    public static class Builder {
        private int width = 176;
        private int height = 166;

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

        private BiConsumer<Flow, ModularPanel> leftConfigurators = (f, p) -> {};
        private BiConsumer<Flow, ModularPanel> rightConfigurators = (f, p) -> {};
        private BiConsumer<ParentWidget<?>, ModularPanel> mainContents = (f, p)-> {};

        protected Builder(MetaMachine machine, PanelSyncManager syncManager) {
            this.machine = machine;
            this.syncManager = syncManager;
        }

        public MachineUIPanel build() {
            var panel = new MachineUIPanel(machine, width, height, attachInventory, addTitleBar, drawGTLogo);

            if (addDefaultConfigurators) {
                if (machine instanceof IHasCircuitSlot circuitSlot && circuitSlot.isCircuitSlotEnabled()) {
                    panel.leftConfigurators.child(GTMuiWidgets.createCircuitSlotPanel(circuitSlot, panel, syncManager));
                }

                if (machine instanceof IWorkable workable) {
                    panel.rightConfigurators.child(GTMuiWidgets.createPowerButton(workable, syncManager));
                }
                if (machine instanceof SimpleTieredMachine simpleTieredMachine) {
                    panel.rightConfigurators.child(GTMuiWidgets.createBatterySlot(simpleTieredMachine, syncManager));
                }
            }

            leftConfigurators.accept(panel.leftConfigurators, panel);
            rightConfigurators.accept(panel.rightConfigurators, panel);
            mainContents.accept(panel.mainContents, panel);

            if (addTraitConfigurators) {
                for (var trait: machine.getTraitHolder().getAllTraits()) {
                    if (trait instanceof IAttachConfiguratorsTrait attachConfiguratorsTrait) {
                        attachConfiguratorsTrait.attachLeftConfigurators(panel.leftConfigurators, panel, syncManager);
                        attachConfiguratorsTrait.attachRightConfigurators(panel.rightConfigurators, panel, syncManager);
                    }
                }
            }

            return panel;
        }

        public static Builder defaultSimpleSingleblockMachinePanel(MetaMachine machine, PanelSyncManager syncManager) {
            return new Builder(machine, syncManager).drawGTLogo(true);
        }

        public static Builder defaultMachinePanel(MetaMachine machine, PanelSyncManager syncManager) {
            return new Builder(machine, syncManager);
        }
    }
}
