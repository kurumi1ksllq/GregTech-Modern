package com.gregtechceu.gtceu.api.ui.base;

import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.util.FocusHandler;
import com.gregtechceu.gtceu.api.ui.util.Observable;
import com.gregtechceu.gtceu.api.ui.util.ScissorStack;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.util.Mth;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The reference implementation of the {@link ParentUIComponent} interface,
 * serving as a base for all parent components on gtceu-ui. If you need your own parent
 * component, it is often beneficial to subclass one of gtceu-ui's existing layout classes,
 * especially {@link com.gregtechceu.gtceu.api.ui.container.WrappingParentUIComponent} is often useful
 */
@Accessors(fluent = true, chain = true)
public abstract class BaseParentUIComponent extends BaseUIComponent implements ParentUIComponent {

    protected static final int UPDATE_CHILD = 1;

    protected final Observable<VerticalAlignment> verticalAlignment = Observable.of(VerticalAlignment.CENTER);
    protected final Observable<HorizontalAlignment> horizontalAlignment = Observable.of(HorizontalAlignment.CENTER);

    @Getter
    protected final AnimatableProperty<Insets> padding = AnimatableProperty.of(Insets.none());

    protected @NotNull UIComponentMenuAccess parentAccess = new ParentComponentMenuAccess();
    protected @Nullable FocusHandler focusHandler = null;
    protected @Nullable ArrayList<Runnable> taskQueue = null;

    @Getter
    @Setter
    protected Surface surface = Surface.BLANK;
    @Getter
    @Setter
    protected boolean allowOverflow = true;

    protected BaseParentUIComponent(Sizing horizontalSizing, Sizing verticalSizing) {
        this.horizontalSizing.set(horizontalSizing);
        this.verticalSizing.set(verticalSizing);

        Observable.observeAll(this::updateLayout, horizontalAlignment, verticalAlignment, padding);
    }

    @Override
    public final void update(float delta, int mouseX, int mouseY) {
        ParentUIComponent.super.update(delta, mouseX, mouseY);
        super.update(delta, mouseX, mouseY);
        this.parentUpdate(delta, mouseX, mouseY);

        if (this.taskQueue != null) {
            this.taskQueue.forEach(Runnable::run);
            this.taskQueue.clear();
        }
    }

    /**
     * Update the state of this component before drawing
     * the next frame. This method is separated from
     * {@link #update(float, int, int)} to enforce the task
     * queue always being run last
     *
     * @param delta  The duration of the last frame, in partial ticks
     * @param mouseX The mouse pointer's x-coordinate
     * @param mouseY The mouse pointer's y-coordinate
     */
    protected void parentUpdate(float delta, int mouseX, int mouseY) {}

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        this.surface.draw(graphics, this);
    }

    @Override
    public void queue(Runnable task) {
        if (this.taskQueue == null) {
            this.parent.queue(task);
        } else {
            this.taskQueue.add(task);
        }
    }

    @Override
    public @Nullable FocusHandler focusHandler() {
        if (this.focusHandler == null) {
            return super.focusHandler();
        } else {
            return this.focusHandler;
        }
    }

    @Override
    public ParentUIComponent verticalAlignment(VerticalAlignment alignment) {
        this.verticalAlignment.set(alignment);
        return this;
    }

    @Override
    public VerticalAlignment verticalAlignment() {
        return this.verticalAlignment.get();
    }

    @Override
    public ParentUIComponent horizontalAlignment(HorizontalAlignment alignment) {
        this.horizontalAlignment.set(alignment);
        return this;
    }

    @Override
    public HorizontalAlignment horizontalAlignment() {
        return this.horizontalAlignment.get();
    }

    @Override
    public ParentUIComponent padding(Insets padding) {
        this.padding.set(padding);
        this.updateLayout();
        return this;
    }

    @Override
    public void mount(ParentUIComponent parent, int x, int y) {
        super.mount(parent, x, y);
        if (parent == null && this.focusHandler == null) {
            this.focusHandler = new FocusHandler(this);
            this.taskQueue = new ArrayList<>();
        }
    }

    @Override
    public BaseParentUIComponent inflate(Size space) {
        if (this.space.equals(space) && !this.dirty) return this;
        this.space = space;

        for (var child : this.children()) {
            child.dismount(DismountReason.LAYOUT_INFLATION);
        }

        super.inflate(space);
        this.layout(space);
        super.inflate(space);
        return this;
    }

    protected void updateLayout() {
        if (!this.mounted) return;

        if (this.batchedEvents > 0) {
            this.batchedEvents++;
            return;
        }

        var previousSize = this.fullSize();

        this.dirty = true;
        this.inflate(this.space);

        if (!previousSize.equals(this.fullSize()) && this.parent != null) {
            this.parent.onChildMutated(this);
        }
    }

    @Override
    protected void runAndDeferEvents(Runnable action) {
        try {
            this.batchedEvents = 1;
            action.run();
        } finally {
            if (this.batchedEvents > 1) {
                this.batchedEvents = 0;
                this.updateLayout();
            } else {
                this.batchedEvents = 0;
            }
        }
    }

    @Override
    public void onChildMutated(UIComponent child) {
        this.updateLayout();
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (this.focusHandler != null) {
            this.focusHandler.updateClickFocus(this.x + mouseX, this.y + mouseY);
        }

        return ParentUIComponent.super.onMouseDown(mouseX, mouseY, button) || super.onMouseDown(mouseX, mouseY, button);
    }

    @Override
    public boolean onMouseUp(double mouseX, double mouseY, int button) {
        if (this.focusHandler != null && this.focusHandler.focused() != null) {
            final var focused = this.focusHandler.focused();
            return focused.onMouseUp(this.x + mouseX - focused.x(), this.y + mouseY - focused.y(), button);
        } else {
            return super.onMouseUp(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        return ParentUIComponent.super.onMouseScroll(mouseX, mouseY, amount) ||
                super.onMouseScroll(mouseX, mouseY, amount);
    }

    @Override
    public boolean onMouseMoved(double mouseX, double mouseY) {
        return ParentUIComponent.super.onMouseMoved(mouseX, mouseY) ||
                super.onMouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (this.focusHandler != null && this.focusHandler.focused() != null) {
            final var focused = this.focusHandler.focused();
            return focused.onMouseDrag(this.x + mouseX - focused.x(), this.y + mouseY - focused.y(), deltaX, deltaY,
                    button);
        } else {
            return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
        }
    }

    @Override
    public boolean onKeyPress(int keyCode, int scanCode, int modifiers) {
        if (this.focusHandler == null) return false;

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            this.focusHandler.cycle((modifiers & GLFW.GLFW_MOD_SHIFT) == 0);
        } else if ((keyCode == GLFW.GLFW_KEY_RIGHT || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_DOWN ||
                keyCode == GLFW.GLFW_KEY_UP) && (modifiers & GLFW.GLFW_MOD_ALT) != 0) {
                    this.focusHandler.moveFocus(keyCode);
                } else
            if (this.focusHandler.focused() != null) {
                return this.focusHandler.focused().onKeyPress(keyCode, scanCode, modifiers);
            }

        return super.onKeyPress(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean onCharTyped(char chr, int modifiers) {
        if (this.focusHandler == null) return false;

        if (this.focusHandler.focused() != null) {
            return this.focusHandler.focused().onCharTyped(chr, modifiers);
        }

        return super.onCharTyped(chr, modifiers);
    }

    /*
     * @Override
     * public void receiveMessage(int id, FriendlyByteBuf buf) {
     * if (id == UPDATE_CHILD) {
     * int index = buf.readVarInt();
     * int updateId = buf.readVarInt();
     * children().get(index).receiveMessage(updateId, buf);
     * }
     * }
     */

    protected class ParentComponentMenuAccess implements UIComponentMenuAccess {

        @Override
        public AbstractContainerScreen<?> screen() {
            if (BaseParentUIComponent.this.containerAccess() == null) return null;
            return BaseParentUIComponent.this.containerAccess().screen();
        }

        @Override
        public UIAdapter<?> adapter() {
            if (BaseParentUIComponent.this.containerAccess() == null) return null;
            return BaseParentUIComponent.this.containerAccess().adapter();
        }
    }

    @Override
    public boolean isMouseOverElement(double mouseX, double mouseY) {
        for (int i = children().size() - 1; i >= 0; i--) {
            UIComponent widget = children().get(i);
            if (widget.isMouseOverElement(mouseX, mouseY)) {
                return true;
            }
        }
        return super.isMouseOverElement(mouseX, mouseY);
    }

    @Override
    public BaseParentUIComponent x(int x) {
        int offset = x - this.x;
        super.x(x);

        for (var child : this.children()) {
            child.x(child.baseX() + offset);
        }
        return this;
    }

    @Override
    public BaseParentUIComponent y(int y) {
        int offset = y - this.y;
        super.y(y);

        for (var child : this.children()) {
            child.y(child.baseY() + offset);
        }
        return this;
    }

    /**
     * @return The offset from the origin of this component
     *         at which children can start to be mounted. Accumulates
     *         padding as well as padding from content sizing
     */
    protected Size childMountingOffset() {
        var padding = this.padding.get();
        return Size.of(padding.left(), padding.top());
    }

    /**
     * Mount a child using the given mounting function if its positioning
     * is equal to {@link Positioning#layout()}, or according to its
     * intrinsic positioning otherwise
     *
     * @param child      The child to mount
     * @param layoutFunc The mounting function for components which follow the layout
     */
    protected void mountChild(@Nullable UIComponent child, Consumer<UIComponent> layoutFunc) {
        if (child == null) return;

        final var positioning = child.positioning().get();
        final var componentMargins = child.margins().get();
        final var padding = this.padding.get();

        switch (positioning.type) {
            case LAYOUT -> layoutFunc.accept(child);
            case ABSOLUTE -> child.mount(
                    this,
                    this.x + positioning.x + componentMargins.left() + padding.left(),
                    this.y + positioning.y + componentMargins.top() + padding.top());
            case RELATIVE -> child.mount(
                    this,
                    this.x + padding.left() + componentMargins.left() +
                            Math.round((positioning.x / 100f) *
                                    (this.width() - child.fullSize().width() - padding.horizontal())),
                    this.y + padding.top() + componentMargins.top() + Math.round(
                            (positioning.y / 100f) * (this.height() - child.fullSize().height() - padding.vertical())));
            case ACROSS -> child.mount(
                    this,
                    this.x + padding.left() + componentMargins.left() +
                            Math.round((positioning.x / 100f) * (this.width() - padding.horizontal())),
                    this.y + padding.top() + componentMargins.top() +
                            Math.round((positioning.y / 100f) * (this.height() - padding.vertical())));
        }
    }

    /**
     * Draw the children of this component along with
     * their focus outline and tooltip, optionally clipping
     * them if {@link #allowOverflow} is {@code false}
     *
     * @param children The list of children to draw
     */
    protected void drawChildren(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta,
                                List<? extends UIComponent> children) {
        if (!this.allowOverflow) {
            var padding = this.padding.get();
            ScissorStack.push(this.x + padding.left(), this.y + padding.top(), this.width - padding.horizontal(),
                    this.height - padding.vertical(), graphics.pose());
        }

        var focusHandler = this.focusHandler();
        for (final UIComponent child : children) {
            if (!child.enabled()) {
                continue;
            }

            if (!ScissorStack.isVisible(child, graphics.pose())) continue;
            graphics.pose().translate(0, 0, child.zIndex() + 1);

            child.draw(graphics, mouseX, mouseY, partialTicks, delta);
            if (focusHandler != null && focusHandler.lastFocusSource() == FocusSource.KEYBOARD_CYCLE &&
                    focusHandler.focused() == child) {
                child.drawFocusHighlight(graphics, mouseX, mouseY, partialTicks, delta);
            }

            graphics.pose().translate(0, 0, -child.zIndex() - 1);
        }

        if (!this.allowOverflow) {
            ScissorStack.pop();
        }
    }

    /**
     * Calculate the space for child inflation. If a given axis
     * is content-sized, return the respective value from {@code thisSpace}
     *
     * @param thisSpace The space for layout inflation of this widget
     * @return The available space for child inflation
     */
    protected Size calculateChildSpace(Size thisSpace) {
        final var padding = this.padding.get();

        return Size.of(
                Mth.lerpInt(this.horizontalSizing.get().contentFactor(), this.width - padding.horizontal(),
                        thisSpace.width() - padding.horizontal()),
                Mth.lerpInt(this.verticalSizing.get().contentFactor(), this.height - padding.vertical(),
                        thisSpace.height() - padding.vertical()));
    }

    @Override
    public BaseParentUIComponent positioning(Positioning positioning) {
        return (BaseParentUIComponent) super.positioning(positioning);
    }

    @Override
    public BaseParentUIComponent margins(Insets margins) {
        return (BaseParentUIComponent) super.margins(margins);
    }
}
