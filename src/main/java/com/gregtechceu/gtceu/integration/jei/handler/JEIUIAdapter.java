package com.gregtechceu.gtceu.integration.jei.handler;

import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;
import com.gregtechceu.gtceu.api.ui.util.ScissorStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.Rect2i;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import lombok.Getter;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeWidget;

import java.util.function.Consumer;

public class JEIUIAdapter implements IRecipeWidget, IJeiGuiEventListener {

    public final UIAdapter<StackLayout> adapter;

    @Getter
    private final ScreenPosition position;
    @Getter
    private final ScreenRectangle area;

    private Consumer<ScreenEvent.Closing> closeListener;

    public JEIUIAdapter(Rect2i bounds) {
        this.adapter = UIAdapter.createWithoutScreen(bounds.getX(), bounds.getY(), bounds.getWidth(),
                bounds.getHeight(), UIContainers::stack);
        this.adapter.inspectorZOffset = 900;
        this.position = new ScreenPosition(bounds.getX(), bounds.getY());
        this.area = new ScreenRectangle(position, bounds.getWidth(), bounds.getHeight());

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
    public void mouseMoved(double mouseX, double mouseY) {
        this.adapter.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.adapter.mouseDragged(mouseX - this.adapter.x(), mouseY - this.adapter.y(), button, deltaX, deltaY);
    }

    @Override
    public boolean keyPressed(double mouseX, double mouseY, int keyCode, int scanCode, int modifiers) {
        return this.adapter.keyPressed(keyCode, scanCode, modifiers);
    }

    /*
     * @Override
     * public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
     * return this.adapter.keyReleased(keyCode, scanCode, modifiers);
     * }
     * 
     * @Override
     * public boolean charTyped(char chr, int modifiers) {
     * return this.adapter.charTyped(chr, modifiers);
     * }
     */

    @Override
    public void drawWidget(GuiGraphics context, double mouseX, double mouseY) {
        ScissorStack.push(this.adapter.x(), this.adapter.y(), this.adapter.width(), this.adapter.height(),
                context.pose());
        this.adapter.render(context, (int) mouseX, (int) mouseY, Minecraft.getInstance().getPartialTick());
        ScissorStack.pop();

        context.flush();
    }
}
