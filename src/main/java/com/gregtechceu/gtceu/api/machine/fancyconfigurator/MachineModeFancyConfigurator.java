package com.gregtechceu.gtceu.api.machine.fancyconfigurator;

import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.core.ParentUIComponent;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.Surface;
import com.gregtechceu.gtceu.api.ui.fancy.FancyMachineUIComponent;
import com.gregtechceu.gtceu.api.ui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.ui.texture.TextTexture;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;
import com.gregtechceu.gtceu.common.data.GTItems;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rundas/Screret
 * @implNote MachineModeFancyConfigurator
 */
public class MachineModeFancyConfigurator implements IFancyUIProvider {

    protected IRecipeLogicMachine machine;

    public MachineModeFancyConfigurator(IRecipeLogicMachine machine) {
        this.machine = machine;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("gtceu.gui.machinemode.title");
    }

    @Override
    public UITexture getTabIcon() {
        return UITextures.item(GTItems.ROBOT_ARM_LV.asStack());
    }

    @Override
    public ParentUIComponent createMainPage(FancyMachineUIComponent widget) {
        var group = new MachineModeConfigurator(Sizing.fixed(140),
                Sizing.fixed(20 * machine.getRecipeTypes().length + 4));
        group.surface(Surface.UI_BACKGROUND_INVERSE);
        for (int i = 0; i < machine.getRecipeTypes().length; i++) {
            int finalI = i;
            group.child(UIComponents.button(Component.empty(),
                    cd -> machine.setActiveRecipeType(finalI))
                    .positioning(Positioning.absolute(2, 2 + i * 20))
                    .sizing(Sizing.fixed(136), Sizing.fixed(20)));
            group.child(UIComponents.texture(UITextures.dynamic(() -> UITextures.group(
                    GuiTextures.VANILLA_BUTTON.copy()
                            .color(machine.getActiveRecipeType() == finalI ? ColorPattern.CYAN.color : -1),
                    UITextures
                            .text(Component.translatable(machine.getRecipeTypes()[finalI].registryName.toLanguageKey()))
                            .maxWidth(136)
                            .textType(TextTexture.TextType.ROLL))))
                    .positioning(Positioning.absolute(2, 2 + i * 20))
                    .sizing(Sizing.fixed(136), Sizing.fixed(20)));

        }
        return group;
    }

    @Override
    public List<Component> getTabTooltips() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.literal("Change active Machine Mode"));
        return tooltip;
    }

    public class MachineModeConfigurator extends FlowLayout {

        public MachineModeConfigurator(Sizing horizontalSizing, Sizing verticalSizing) {
            super(horizontalSizing, verticalSizing, Algorithm.HORIZONTAL);
        }

        @Override
        public void init() {
            super.init();
        }

        /*
         * @Override
         * public void writeInitialData(FriendlyByteBuf buffer) {
         * buffer.writeVarInt(machine.getActiveRecipeType());
         * }
         * 
         * @Override
         * public void readInitialData(FriendlyByteBuf buffer) {
         * machine.setActiveRecipeType(buffer.readVarInt());
         * }
         * 
         * // TODO implement?
         * 
         * @Override
         * public void detectAndSendChanges() {
         * this.sendMessage(0, buf -> buf.writeVarInt(machine.getActiveRecipeType()));
         * }

        @Override
        public void receiveMessage(int id, FriendlyByteBuf buf) {
            if (id == 0) {
                machine.setActiveRecipeType(buf.readVarInt());
            }
        }
         */

        @Override
        protected void parentUpdate(float delta, int mouseX, int mouseY) {
            super.parentUpdate(delta, mouseX, mouseY);
            //this.sendMessage(0, buf -> buf.writeVarInt(machine.getActiveRecipeType()));
        }
    }
}
