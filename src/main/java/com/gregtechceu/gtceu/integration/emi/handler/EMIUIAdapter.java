package com.gregtechceu.gtceu.integration.emi.handler;

import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;
import com.gregtechceu.gtceu.api.ui.util.ScissorStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.ContainerEventHandler;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import dev.emi.emi.api.widget.Bounds;
import dev.emi.emi.api.widget.Widget;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.util.List;
import java.util.function.Consumer;

public class EMIUIAdapter extends Widget implements ContainerEventHandler {

    public final UIAdapter<StackLayout> adapter;

    @Getter
    private final Bounds bounds;
    @Getter
    @Setter
    private boolean isDragging;
    @Getter
    @Setter
    private GuiEventListener focused;

    private Consumer<ScreenEvent.Closing> closeListener;

    public EMIUIAdapter(Bounds bounds) {
        this.adapter = UIAdapter.createWithoutScreen(bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                UIContainers::stack);
        this.adapter.inspectorZOffset = 900;
        this.bounds = bounds;

        if (Minecraft.getInstance().screen != null) {
            this.closeListener = (ScreenEvent.Closing event) -> {
                // this.adapter.dispose();
                MinecraftForge.EVENT_BUS.unregister(this.closeListener);
            };
            MinecraftForge.EVENT_BUS.register(this.closeListener);
        }
    }

    public void prepare() {
        this.adapter.inflateAndMount();
    }

    public StackLayout rootComponent() {
        return this.adapter.rootComponent;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.adapter.isMouseOver(mouseX, mouseY);
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of();
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return this.adapter.mouseClicked(mouseX - this.adapter.x(), mouseY - this.adapter.y(), button);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.adapter.mouseClicked(mouseX - this.adapter.x(), mouseY - this.adapter.y(), button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.adapter.mouseScrolled(mouseX - this.adapter.x(), mouseY - this.adapter.y(), amount);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.adapter.mouseReleased(mouseX - this.adapter.x(), mouseY - this.adapter.y(), button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.adapter.mouseDragged(mouseX - this.adapter.x(), mouseY - this.adapter.y(), button, deltaX, deltaY);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float partialTick) {
        ScissorStack.push(this.adapter.x(), this.adapter.y(), this.adapter.width(), this.adapter.height(),
                context.pose());
        this.adapter.render(context, mouseX, mouseY, partialTick);
        ScissorStack.pop();

        context.flush();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return this.adapter.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return this.adapter.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return this.adapter.charTyped(chr, modifiers);
    }

    @Override
    @Tolerate
    public void setFocused(boolean focused) {
        adapter.setFocused(focused);
    }

    @Override
    public boolean isFocused() {
        return adapter.isFocused();
    }
}
