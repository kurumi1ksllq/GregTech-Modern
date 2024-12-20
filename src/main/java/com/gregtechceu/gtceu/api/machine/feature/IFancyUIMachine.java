package com.gregtechceu.gtceu.api.machine.feature;

import com.gregtechceu.gtceu.api.capability.IControllable;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.CombinedDirectionalFancyConfigurator;
import com.gregtechceu.gtceu.api.machine.fancyconfigurator.MachineModeFancyConfigurator;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.fancy.*;
import com.gregtechceu.gtceu.api.ui.fancy.IFancyUIProvider;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;

import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

/**
 * @author KilaBash
 * @date 2023/6/28
 * @implNote IFancyUIMachine
 */
public interface IFancyUIMachine extends IUIMachine, IFancyUIProvider {

    @OnlyIn(Dist.CLIENT)
    default void loadClientUI(Player player, UIAdapter<StackLayout> adapter, MetaMachine holder) {
        adapter.rootComponent
                .child(new FancyMachineUIComponent(this, Sizing.fixed(176), Sizing.fixed(166))
                        .positioning(Positioning.relative(50, 50)));
    }

    /**
     * We should not override this method in general, and use
     * {@link IFancyUIMachine#createBaseUIComponent(FancyMachineUIComponent)} instead,
     */
    @Override
    default ParentUIComponent createMainPage(FancyMachineUIComponent component) {
        var editableUI = self().getDefinition().getEditableUI();
        if (editableUI != null) {
            var template = editableUI.createCustomUI();
            if (template == null) {
                template = editableUI.createDefault();
            }
            // noinspection unchecked
            editableUI.setupUI(template,
                    (UIAdapter<StackLayout>) component.containerAccess().adapter(),
                    self());
            return template;
        }
        return createBaseUIComponent(component);
    }

    /**
     * Create the core widget of this machine.
     */
    default ParentUIComponent createBaseUIComponent(FancyMachineUIComponent component) {
        var group = UIContainers.stack(Sizing.content(), Sizing.content());

        group.child(UIComponents.texture(GuiTextures.SCENE)
                .sizing(Sizing.fixed(48), Sizing.fixed(16))
                .positioning(Positioning.absolute((100 - 48) / 2, 60)));
        // TODO scene component
        /*
         * TrackedDummyWorld world = new TrackedDummyWorld();
         * world.addBlock(BlockPos.ZERO, BlockInfo.fromBlockState(self().getBlockState()));
         * SceneComponent sceneWidget = new SceneComponent(0, 0, 100, 100, world) {
         * 
         * @Override
         * 
         * @OnlyIn(Dist.CLIENT)
         * public void drawInBackground(@NotNull GuiGraphics graphics, int mouseX, int mouseY,
         * float partialTicks) {
         * // AUTO ROTATION
         * if (renderer != null) {
         * this.rotationPitch = (partialTicks + getGui().getTickCount()) * 2;
         * renderer.setCameraLookAt(this.center, 0.1f, Math.toRadians(this.rotationPitch),
         * Math.toRadians(this.rotationYaw));
         * }
         * super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
         * }
         * };
         * sceneWidget.useOrtho(true)
         * .setOrthoRange(0.5f)
         * .setScalable(false)
         * .setDraggable(false)
         * .setRenderFacing(false)
         * .setRenderSelect(false);
         * sceneWidget.getRenderer().setFov(30);
         * group.child(sceneWidget);
         * sceneWidget.setRenderedCore(List.of(BlockPos.ZERO), null);
         */
        return group;
    }

    @Override
    default UITexture getTabIcon() {
        return UITextures.item(self().getDefinition().getItem().getDefaultInstance());
    }

    @Override
    default void attachSideTabs(TabsComponent sideTabs) {
        sideTabs.setMainTab(this);

        if (this instanceof IRecipeLogicMachine rLMachine && rLMachine.getRecipeTypes().length > 1) {
            sideTabs.attachSubTab(new MachineModeFancyConfigurator(rLMachine));
        }
        var directionalConfigurator = CombinedDirectionalFancyConfigurator.of(self(), self());
        if (directionalConfigurator != null)
            sideTabs.attachSubTab(directionalConfigurator);
    }

    @Override
    default void attachConfigurators(ConfiguratorPanelComponent configuratorPanel) {
        if (this instanceof IControllable controllable) {
            configuratorPanel.attachConfigurators(new IFancyConfiguratorButton.Toggle(
                    GuiTextures.BUTTON_POWER.getSubTexture(0, 0, 1, 0.5),
                    GuiTextures.BUTTON_POWER.getSubTexture(0, 0.5, 1, 0.5),
                    controllable::isWorkingEnabled, (button, pressed) -> controllable.setWorkingEnabled(pressed))
                    .setTooltipsSupplier(pressed -> List.of(
                            Component.translatable(
                                    pressed ? "behaviour.soft_hammer.enabled" : "behaviour.soft_hammer.disabled"))));
        }
        if (this instanceof MetaMachine machine) {
            for (var direction : Direction.values()) {
                if (machine.getCoverContainer().hasCover(direction)) {
                    var configurator = machine.getCoverContainer().getCoverAtSide(direction).getConfigurator();
                    if (configurator != null)
                        configuratorPanel.attachConfigurators(configurator);
                }
            }
        }
    }

    @Override
    default void attachTooltips(TooltipsPanelComponent tooltipsPanel) {
        tooltipsPanel.attachTooltips(self());
        self().getTraits().stream().filter(IFancyTooltip.class::isInstance).map(IFancyTooltip.class::cast)
                .forEach(tooltipsPanel::attachTooltips);
    }

    @Override
    default List<Component> getTabTooltips() {
        var list = new ArrayList<Component>();
        list.add(Component.translatable(self().getDefinition().getDescriptionId()));
        return list;
    }

    @Override
    default Component getTitle() {
        return Component.translatable(self().getDefinition().getDescriptionId());
    }
}
