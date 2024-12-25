package com.gregtechceu.gtceu.api.ui.component.directional;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.component.SceneComponent;
import com.gregtechceu.gtceu.api.ui.component.TextureComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.fancy.FancyMachineUIComponent;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.client.scene.ISceneBlockRenderHook;
import com.lowdragmc.lowdraglib.client.scene.WorldSceneRenderer;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.utils.BlockPosFace;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class CombinedDirectionalConfigurator extends FlowLayout {

    protected final static int MOUSE_CLICK_CLIENT_ACTION_ID = 0x0001_0001;
    protected final static int UPDATE_UI_ID = 0x0001_0002;

    protected final IDirectionalConfigHandler[] configHandlers;
    private final FancyMachineUIComponent machineUI;
    private final MetaMachine machine;

    protected SceneComponent sceneComponent;
    protected TextureComponent textureComponent;

    protected @Nullable BlockPos selectedPos;
    protected @Nullable Direction selectedSide;

    public CombinedDirectionalConfigurator(FancyMachineUIComponent machineUI,
                                           IDirectionalConfigHandler[] configHandlers,
                                           MetaMachine machine, int width, int height) {
        super(Sizing.fixed(width), Sizing.fixed(height), Algorithm.HORIZONTAL);
        this.padding(Insets.of(4));

        this.machineUI = machineUI;
        this.configHandlers = configHandlers;
        this.machine = machine;
    }

    @Override
    public void init() {
        super.init();

        child(textureComponent = UIComponents.texture(GuiTextures.BACKGROUND_INVERSE));
        textureComponent.sizing(Sizing.fill(), Sizing.fill());
        child(sceneComponent = createSceneComponent());

        for (IDirectionalConfigHandler configHandler : configHandlers) {
            configHandler.addAdditionalUIElements(this);
        }

        addConfigWidgets(sceneComponent);
    }

    private SceneComponent createSceneComponent() {
        var pos = this.machine.getPos();

        SceneComponent sceneWidget = new SceneComponent(Sizing.fill(), Sizing.fill(), this.machine.getLevel())
                .renderedCore(List.of(pos), null)
                .renderSelect(false)
                .onSelected(this::onSideSelected);

        sceneWidget.renderer().addRenderedBlocks(
                List.of(pos.above(), pos.below(), pos.north(), pos.south(), pos.east(), pos.west()),
                new ISceneBlockRenderHook() {

                    @Override
                    @OnlyIn(Dist.CLIENT)
                    public void apply(boolean isTESR, RenderType layer) {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);
                    }
                });

        sceneWidget.renderer().setAfterWorldRender(this::renderOverlays);

        var playerRotation = player().getRotationVector();
        sceneWidget.cameraYawAndPitch(playerRotation.x, playerRotation.y - 90);

        sceneWidget.surface(Surface.flat(Color.BLACK.rgb()));
        return sceneWidget;
    }

    private void renderOverlays(WorldSceneRenderer renderer) {
        sceneComponent.renderBlockOverLay(renderer);

        for (Direction face : GTUtil.DIRECTIONS) {
            for (IDirectionalConfigHandler configHandler : configHandlers) {
                configHandler.renderOverlay(sceneComponent, new BlockPosFace(machine.getPos(), face));
            }
        }
    }

    private void addConfigWidgets(SceneComponent sceneWidget) {
        int yOffsetLeft = 0, yOffsetRight = 0;

        for (IDirectionalConfigHandler configHandler : configHandlers) {
            UIComponent widget = configHandler.getSideSelectorWidget(sceneWidget, machineUI);

            if (widget == null)
                continue;

            final Size widgetSize = widget.fullSize();
            switch (configHandler.getScreenSide()) {
                case LEFT -> {
                    widget.positioning(Positioning.absolute(6, height - 6 - widgetSize.height() - yOffsetLeft));
                    yOffsetLeft += widgetSize.height() + 3;
                }
                case RIGHT -> {
                    widget.positioning(Positioning.absolute(width - widgetSize.width() - 6,
                            height - 6 - widgetSize.height() - yOffsetRight));
                    yOffsetRight += widgetSize.height() + 3;
                }
            }

            this.child(widget);
        }
    }

    protected void onSideSelected(BlockPos pos, Direction side) {
        if (!pos.equals(machine.getPos()))
            return;

        if (this.selectedSide == side)
            return; // No need to do anything if the same side is already selected

        this.selectedSide = side;

        for (IDirectionalConfigHandler configWidget : this.configHandlers) {
            configWidget.onSideSelected(pos, side);
        }
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        var lastSide = this.selectedSide;

        var result = super.onMouseDown(mouseX, mouseY, button);

        if (isMouseOverElement(mouseX, mouseY) && this.selectedSide == lastSide && this.selectedSide != null) {
            var hover = sceneComponent.hoverPosFace();

            if (hover != null && hover.pos.equals(machine.getPos()) && hover.facing == this.selectedSide) {
                var cd = new ClickData();
                /*
                sendMessage(MOUSE_CLICK_CLIENT_ACTION_ID, buf -> {
                    cd.writeToBuf(buf);
                    buf.writeByte(this.selectedSide.ordinal());
                });
                */
            }
        }
        return result;
    }

    /*
    @Override
    public void receiveMessage(int id, FriendlyByteBuf buf) {
        if (id != MOUSE_CLICK_CLIENT_ACTION_ID) {
            super.receiveMessage(id, buf);
            return;
        }

        var side = GTUtil.DIRECTIONS[buf.readByte()];

        for (IDirectionalConfigHandler configHandler : configHandlers) {
            configHandler.handleClick(null, side);
        }
    }
    */
}
