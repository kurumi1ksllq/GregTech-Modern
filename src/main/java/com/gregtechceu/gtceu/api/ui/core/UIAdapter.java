package com.gregtechceu.gtceu.api.ui.core;

import com.gregtechceu.gtceu.api.ui.UIContainerMenu;
import com.gregtechceu.gtceu.api.ui.serialization.SyncedProperty;
import com.gregtechceu.gtceu.api.ui.util.CursorAdapter;
import com.gregtechceu.gtceu.client.renderdoc.RenderDoc;
import com.gregtechceu.gtceu.config.ConfigHolder;

import com.lowdragmc.lowdraglib.Platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * A UI adapter constitutes the main entrypoint to using gtceu-ui.
 * It takes care of rendering the UI tree correctly, handles input events
 * and cursor styling as well as the component inspector.
 * <p>
 * Additionally, the adapter implements all interfaces required for it
 * to be treated as a normal widget by the vanilla screen system - this means
 * even if you choose to not use {@link com.gregtechceu.gtceu.api.ui.base.BaseContainerScreen}
 * you can always simply add it as a widget and get most of the functionality
 * working out of the box
 *
 * @see com.gregtechceu.gtceu.api.ui.base.BaseContainerScreen
 */
@Accessors(fluent = true, chain = true)
public class UIAdapter<R extends ParentUIComponent>
                      implements GuiEventListener, Renderable, NarratableEntry, UIComponentMenuAccess {

    @Getter
    private static boolean isRendering = false;

    @Getter
    @Setter
    public AbstractContainerScreen<?> screen;

    public final R rootComponent;
    public final CursorAdapter cursorAdapter;

    protected boolean disposed = false;
    protected boolean captureFrame = false;

    @Getter
    @Setter
    protected int x, y;
    @Getter
    @Setter
    protected int leftPos, topPos;
    @Getter
    @Setter
    protected int width, height;

    public boolean enableInspector = ConfigHolder.INSTANCE.dev.debug;
    public boolean globalInspector = false;
    public int inspectorZOffset = 1000;

    protected UIAdapter(int x, int y, int width, int height, R rootComponent, boolean isScreen) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.cursorAdapter = CursorAdapter.ofClientWindow();
        this.rootComponent = rootComponent;
        this.rootComponent.containerAccess(this);
        if (isScreen) {
            this.rootComponent.surface(Surface.VANILLA_TRANSLUCENT);
        }
    }

    /**
     * Create a UI adapter for the given screen. This also sets it up
     * to be rendered and receive input events, without needing you to
     * do any more setup
     *
     * @param screen             The screen for which to create an adapter
     * @param rootComponentMaker A function which will create the root component of this screen
     * @param <R>                The type of root component the created adapter will use
     * @return The new UI adapter, already set up for the given screen
     */
    public static <R extends ParentUIComponent> UIAdapter<R> create(Screen screen,
                                                                    BiFunction<Sizing, Sizing, R> rootComponentMaker) {
        var rootComponent = rootComponentMaker.apply(Sizing.fill(), Sizing.fill());

        var adapter = new UIAdapter<>(0, 0, screen.width, screen.height, rootComponent, true);
        screen.addRenderableWidget(adapter);
        screen.setFocused(adapter);

        return adapter;
    }

    /**
     * Create a new UI adapter without the specific context of a screen - use this
     * method when you want to embed gtceu-ui into a different context
     *
     * @param x                  The x-coordinate of the top-left corner of the root component
     * @param y                  The y-coordinate of the top-left corner of the root component
     * @param width              The width of the available area, in pixels
     * @param height             The height of the available area, in pixels
     * @param rootComponentMaker A function which will create the root component of the adapter
     * @param <R>                The type of root component the created adapter will use
     * @return The new UI adapter, ready for layout inflation
     */
    public static <R extends ParentUIComponent> UIAdapter<R> createWithoutScreen(int x, int y, int width, int height,
                                                                                 BiFunction<Sizing, Sizing, R> rootComponentMaker) {
        var rootComponent = rootComponentMaker.apply(Sizing.fill(), Sizing.fill());
        return new UIAdapter<>(x, y, width, height, rootComponent, false);
    }

    /**
     * Begin the layout process of the UI tree and
     * mount the tree once the layout is inflated
     * <p>
     * After this method has executed, this adapter is ready for rendering
     */
    public void inflateAndMount() {
        this.rootComponent.inflate(Size.of(this.width, this.height));
        this.rootComponent.mount(null, this.x, this.y);
    }

    public void moveAndResize(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;

        this.inflateAndMount();
    }

    /**
     * Dispose this UI adapter - this will destroy the cursor
     * objects held onto by this adapter and stop updating the cursor style
     * <p>
     * After this method has executed, this adapter can safely be garbage-collected
     */
    public void dispose() {
        this.cursorAdapter.dispose();
        this.rootComponent.dispose();
        this.disposed = true;
    }

    /**
     * @return Toggle rendering of the inspector
     */
    public boolean toggleInspector() {
        return this.enableInspector = !this.enableInspector;
    }

    /**
     * @return Toggle the inspector between
     *         hovered and global mode
     */
    public boolean toggleGlobalInspector() {
        return this.globalInspector = !this.globalInspector;
    }

    @Override
    public void sendMessage(UIComponent component, int id, Consumer<FriendlyByteBuf> writer) {
        if (menu() instanceof UIContainerMenu<?> menu) {
            menu.sendMessage(id, writer);
        }
    }

    @Override
    public UIAdapter<?> adapter() {
        return this;
    }

    @Nullable
    public AbstractContainerMenu menu() {
        return screen() != null ? screen().getMenu() : null;
    }

    public <R> SyncedProperty<R> getMenuProperty(String name) {
        return screen().getMenu().getProperty(name);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (!(graphics instanceof UIGuiGraphics)) graphics = UIGuiGraphics.of(graphics);
        var uiGraphics = (UIGuiGraphics) graphics;

        try {
            isRendering = true;

            if (this.captureFrame) RenderDoc.startFrameCapture();

            final var delta = Minecraft.getInstance().getDeltaFrameTime();
            final var window = Minecraft.getInstance().getWindow();

            this.rootComponent.update(delta, mouseX, mouseY);

            RenderSystem.enableDepthTest();
            RenderSystem.enableScissor(0, 0, window.getWidth(), window.getHeight());

            this.rootComponent.draw(uiGraphics, mouseX, mouseY, partialTicks, delta);

            RenderSystem.depthMask(true);
            RenderSystem.depthMask(false);

            RenderSystem.disableScissor();
            RenderSystem.disableDepthTest();

            final var hovered = this.rootComponent.childAt(mouseX, mouseY);
            if (!disposed && hovered != null) {
                this.cursorAdapter.applyStyle(hovered.cursorStyle());
            }

            if (this.enableInspector) {
                uiGraphics.pose().pushPose();
                uiGraphics.pose().translate(0, 0, this.inspectorZOffset);
                uiGraphics.drawInspector(this.rootComponent, mouseX, mouseY, !this.globalInspector);
                uiGraphics.pose().popPose();
            }

            if (this.captureFrame) RenderDoc.endFrameCapture();
        } finally {
            isRendering = false;
            this.captureFrame = false;
        }
    }

    public void renderTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        try {
            isRendering = true;
            if (this.captureFrame) RenderDoc.startFrameCapture();

            if (!(graphics instanceof UIGuiGraphics)) graphics = UIGuiGraphics.of(graphics);
            var uiGraphics = (UIGuiGraphics) graphics;

            final var partialTicks = Minecraft.getInstance().getPartialTick();
            final var delta = Minecraft.getInstance().getDeltaFrameTime();

            this.rootComponent.drawTooltip(uiGraphics, mouseX, mouseY, partialTicks, delta);

            if (this.captureFrame) RenderDoc.endFrameCapture();
        } finally {
            isRendering = false;
            this.captureFrame = false;
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.rootComponent.isInBoundingBox(mouseX, mouseY);
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    public boolean isFocused() {
        return true;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        GuiEventListener.super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return this.rootComponent.onMouseDown(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return this.rootComponent.onMouseUp(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return this.rootComponent.onMouseScroll(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.rootComponent.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (Platform.isDevEnv() && keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) {
            if ((modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                this.toggleInspector();
            } else if ((modifiers & GLFW.GLFW_MOD_ALT) != 0) {
                this.toggleGlobalInspector();
            }
        }

        if (Platform.isDevEnv() && keyCode == GLFW.GLFW_KEY_R && RenderDoc.isAvailable()) {
            if ((modifiers & GLFW.GLFW_MOD_ALT) != 0 && (modifiers & GLFW.GLFW_MOD_CONTROL) != 0) {
                this.captureFrame = true;
            }
        }

        return this.rootComponent.onKeyPress(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return this.rootComponent.onCharTyped(chr, modifiers);
    }

    @Override
    public NarrationPriority narrationPriority() {
        if (this.rootComponent.focusHandler() != null &&
                this.rootComponent.focusHandler().focused() instanceof NarratableEntry narratable) {
            return narratable.narrationPriority();
        }
        return NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput output) {
        if (this.rootComponent.focusHandler() != null &&
                this.rootComponent.focusHandler().focused() instanceof NarratableEntry narratable) {
            narratable.updateNarration(output);
        }
    }
}
