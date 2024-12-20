package com.gregtechceu.gtceu.api.ui.base;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.inject.GreedyInputUIComponent;
import com.gregtechceu.gtceu.api.ui.util.DisposableScreen;
import com.gregtechceu.gtceu.api.ui.util.UIErrorToast;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.function.BiFunction;

/**
 * A minimal implementation of a Screen which fully
 * supports all aspects of the UI system. Implementing this class
 * is trivial, as you only need to provide implementations for
 * {@link #createAdapter()} to initialize the UI system and {@link #build(ParentUIComponent)}
 * which is where you declare your component hierarchy.
 * <p>
 * Should you be locked into a different superclass on your screen already,
 * you can easily copy all code from this class into your screen - as you
 * can see supporting the entire feature-set of gtceu-ui only requires
 * very few changes to how a vanilla screen works
 *
 * @param <R> The type of root component this screen uses
 */
public abstract class BaseUIScreen<R extends ParentUIComponent> extends Screen implements DisposableScreen {

    /**
     * The UI adapter of this screen. This handles
     * all user input as well as setting up GL state for rendering
     * and managing component focus
     */
    @Getter
    protected UIAdapter<R> uiAdapter = null;

    /**
     * Whether this screen has encountered an unrecoverable
     * error during its lifecycle and should thus close
     * itself on the next frame
     */
    protected boolean invalid = false;

    protected BaseUIScreen(Component title) {
        super(title);
    }

    protected BaseUIScreen() {
        this(Component.empty());
    }

    /**
     * Initialize the UI adapter for this screen. Usually
     * the body of this method will simply consist of a call
     * to {@link UIAdapter#create(Screen, BiFunction)}
     *
     * @return The UI adapter for this screen to use
     */
    protected abstract @NotNull UIAdapter<R> createAdapter();

    /**
     * Build the component hierarchy of this screen,
     * called after the adapter and root component have been
     * initialized by {@link #createAdapter()}
     *
     * @param rootComponent The root component created
     *                      in the previous initialization step
     */
    protected abstract void build(R rootComponent);

    @Override
    protected void init() {
        if (this.invalid) return;

        // Check whether this screen was already initialized
        if (this.uiAdapter != null) {
            // If it was, only resize the adapter instead of recreating it - this preserves UI state
            this.uiAdapter.moveAndResize(0, 0, this.width, this.height);
            // Re-add it as a child to circumvent vanilla clearing them
            this.addRenderableWidget(this.uiAdapter);
            super.init();
        } else {
            try {
                this.uiAdapter = this.createAdapter();
                if (this.uiAdapter == null) {
                    this.invalid = true;
                    this.onClose();
                    return;
                }
                super.init();

                if (!this.renderables.contains(this.uiAdapter)) {
                    this.addRenderableWidget(this.uiAdapter);
                    this.setFocused(this.uiAdapter);
                }

                this.build(this.uiAdapter.rootComponent);
                this.uiAdapter.rootComponent.containerAccess(this.uiAdapter);
                this.uiAdapter.rootComponent.init();

                this.uiAdapter.moveAndResize(0, 0, this.width, this.height);
            } catch (Exception error) {
                GTCEu.LOGGER.warn("Could not initialize gtceu screen", error);
                UIErrorToast.report(error);
                this.invalid = true;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.uiAdapter.rootComponent.tick();
    }

    /**
     * A convenience shorthand for querying a component from the adapter's
     * root component via {@link ParentUIComponent#childById(Class, String)}
     */
    protected <C extends UIComponent> @Nullable C component(Class<C> expectedClass, String id) {
        return this.uiAdapter.rootComponent.childById(expectedClass, id);
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics) {}

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        if (!this.invalid) {
            super.render(context, mouseX, mouseY, delta);
        } else {
            this.onClose();
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.uiAdapter == null) return false;

        if ((modifiers & GLFW.GLFW_MOD_CONTROL) == 0 &&
                this.uiAdapter.rootComponent.focusHandler()
                        .focused() instanceof GreedyInputUIComponent inputComponent &&
                inputComponent.onKeyPress(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
            this.onClose();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.uiAdapter.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return this.uiAdapter;
    }

    @Override
    public void removed() {
        if (this.uiAdapter != null) {
            this.uiAdapter.cursorAdapter.applyStyle(CursorStyle.NONE);
        }
    }

    @Override
    public void dispose() {
        if (this.uiAdapter != null) this.uiAdapter.dispose();
    }
}
