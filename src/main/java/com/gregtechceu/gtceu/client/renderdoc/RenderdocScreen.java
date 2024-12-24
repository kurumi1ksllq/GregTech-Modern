package com.gregtechceu.gtceu.client.renderdoc;

import com.gregtechceu.gtceu.api.ui.base.BaseUIScreen;
import com.gregtechceu.gtceu.api.ui.component.ButtonComponent;
import com.gregtechceu.gtceu.api.ui.component.CheckboxComponent;
import com.gregtechceu.gtceu.api.ui.component.LabelComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.ops.ComponentOps;
import com.gregtechceu.gtceu.api.ui.util.ClickData;
import com.gregtechceu.gtceu.utils.CommandOpenedScreen;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class RenderdocScreen extends BaseUIScreen<FlowLayout> implements CommandOpenedScreen {

    private int ticks = 0;
    private boolean setCaptureKey = false;
    private @Nullable RenderDoc.Key scheduledKey = null;

    private ButtonComponent captureKeyButton = null;
    private LabelComponent captureLabel = null;

    @Override
    protected @NotNull UIAdapter<FlowLayout> createAdapter() {
        return UIAdapter.create(this, UIContainers::verticalFlow);
    }

    @Override
    protected void build(FlowLayout rootComponent) {
        rootComponent.surface(Surface.VANILLA_TRANSLUCENT);

        var overlayState = RenderDoc.getOverlayOptions();
        rootComponent.child(
                UIContainers.verticalFlow(Sizing.content(), Sizing.content())
                        .child(UIComponents.label(Component.literal("RenderDoc Controls")).shadow(true)
                                .margins(Insets.top(5).withBottom(10)))
                        .child(
                                UIContainers.grid(Sizing.content(), Sizing.content(), 2, 2)
                                        .child(overlayControl(Component.literal("Enabled"), overlayState,
                                                RenderDoc.OverlayOption.ENABLED), 0, 0)
                                        .child(overlayControl(Component.literal("Capture List"), overlayState,
                                                RenderDoc.OverlayOption.CAPTURE_LIST), 0, 1)
                                        .child(overlayControl(Component.literal("Frame Rate"), overlayState,
                                                RenderDoc.OverlayOption.FRAME_RATE), 1, 0)
                                        .child(overlayControl(Component.literal("Frame Number"), overlayState,
                                                RenderDoc.OverlayOption.FRAME_NUMBER), 1, 1))
                        .child(
                                UIComponents.box(Sizing.fixed(175), Sizing.fixed(1))
                                        .color(Color.ofFormatting(ChatFormatting.DARK_GRAY))
                                        .fill(true)
                                        .margins(Insets.vertical(5)))
                        .child(
                                UIContainers.grid(Sizing.content(), Sizing.content(), 2, 2)
                                        .child(UIComponents.button(
                                                Component.literal("Launch UI"),
                                                (ClickData cd) -> RenderDoc.launchReplayUI(true))
                                                .horizontalSizing(Sizing.fixed(90)).margins(Insets.of(2)), 0, 0)
                                        .child((this.captureKeyButton = UIComponents.button(
                                                Component.literal("Capture Hotkey"),
                                                (ClickData cd) -> {
                                                    this.captureKeyButton.active(false);
                                                    this.captureKeyButton.setMessage(Component.literal("Press..."));

                                                    this.setCaptureKey = true;
                                                })).horizontalSizing(Sizing.fixed(90)).margins(Insets.of(2)), 1, 0)
                                        .child(UIComponents.button(
                                                Component.literal("Capture Frame"),
                                                (ClickData cd) -> RenderDoc.triggerCapture())
                                                .horizontalSizing(Sizing.fixed(90)).margins(Insets.of(2)), 0, 1)
                                        .child(this.captureLabel = UIComponents.label(
                                                this.createCapturesText()), 1, 1)
                                        .verticalAlignment(VerticalAlignment.CENTER)
                                        .horizontalAlignment(HorizontalAlignment.CENTER))
                        .horizontalAlignment(HorizontalAlignment.CENTER)
                        .padding(Insets.of(5))
                        .surface(Surface.flat(0x77000000).and(Surface.outline(0x77000000))))
                .verticalAlignment(VerticalAlignment.CENTER)
                .horizontalAlignment(HorizontalAlignment.CENTER);
    }

    @Override
    public void tick() {
        super.tick();
        if (++this.ticks % 10 != 0) return;

        if (this.scheduledKey != null) {
            RenderDoc.setCaptureKeys(this.scheduledKey);
            this.scheduledKey = null;
        }

        this.captureLabel.text(this.createCapturesText());
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.setCaptureKey) {
            this.captureKeyButton.active = true;
            this.captureKeyButton.setMessage(Component.literal("Capture Hotkey"));

            this.setCaptureKey = false;

            var key = RenderDoc.Key.fromGLFW(keyCode);
            if (key != null) {
                this.ticks = 0;
                this.scheduledKey = key;

                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private Component createCapturesText() {
        return ComponentOps.withColor("Captures: §" + RenderDoc.getNumCaptures(),
                ComponentOps.color(ChatFormatting.WHITE), 0x00D7FF);
    }

    private static CheckboxComponent overlayControl(Component name, EnumSet<RenderDoc.OverlayOption> state,
                                                    RenderDoc.OverlayOption option) {
        var checkbox = UIComponents.checkbox(name);
        checkbox.margins(Insets.of(3)).horizontalSizing(Sizing.fixed(100));
        checkbox.checked(state.contains(option));
        checkbox.onChanged(enabled -> {
            if (enabled) {
                RenderDoc.enableOverlayOptions(option);
            } else {
                RenderDoc.disableOverlayOptions(option);
            }
        });
        return checkbox;
    }
}
