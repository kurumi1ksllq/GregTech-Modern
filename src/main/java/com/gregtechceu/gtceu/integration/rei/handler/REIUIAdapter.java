package com.gregtechceu.gtceu.integration.rei.handler;

import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.UIAdapter;
import com.gregtechceu.gtceu.api.ui.util.ScissorStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.widgets.Widget;
import me.shedaniel.rei.api.client.gui.widgets.WidgetWithBounds;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class REIUIAdapter extends Widget {

    public static final Point LAYOUT = new Point(-69, -69);

    private Consumer<ScreenEvent.Closing> closeListener;
    public final UIAdapter<StackLayout> adapter;

    public REIUIAdapter(Rectangle bounds) {
        this.adapter = UIAdapter.createWithoutScreen(bounds.x, bounds.y, bounds.width, bounds.height,
                UIContainers::stack);
        this.adapter.inspectorZOffset = 900;

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

    public <W extends WidgetWithBounds> REIWidgetComponent wrap(W widget) {
        return new REIWidgetComponent(widget);
    }

    public <W extends WidgetWithBounds> REIWidgetComponent wrap(Function<Point, W> widgetFactory,
                                                                Consumer<W> widgetConfigurator) {
        var widget = widgetFactory.apply(LAYOUT);
        widgetConfigurator.accept(widget);
        return new REIWidgetComponent(widget);
    }

    @Override
    public boolean containsMouse(double mouseX, double mouseY) {
        return this.adapter.isMouseOver(mouseX, mouseY);
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
    public void render(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        ScissorStack.push(this.adapter.x(), this.adapter.y(), this.adapter.width(), this.adapter.height(),
                context.pose());
        this.adapter.render(context, mouseX, mouseY, partialTicks);
        ScissorStack.pop();

        context.flush();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return List.of();
    }
}
