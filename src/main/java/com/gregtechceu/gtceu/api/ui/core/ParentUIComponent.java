package com.gregtechceu.gtceu.api.ui.core;

import com.gregtechceu.gtceu.api.ui.parsing.IncompatibleUIModelException;
import com.gregtechceu.gtceu.api.ui.parsing.UIModel;
import com.gregtechceu.gtceu.api.ui.parsing.UIParsing;
import com.gregtechceu.gtceu.api.ui.util.ScissorStack;

import com.google.common.base.Strings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.regex.Pattern;

public interface ParentUIComponent extends UIComponent {

    /**
     * Recalculate the layout of this component
     */
    void layout(Size space);

    /**
     * Called when a child of this parent component has been mutated in some way
     * that would affect the layout of this component
     *
     * @param child The child that has been mutated
     */
    void onChildMutated(UIComponent child);

    /**
     * Queue a task to be run after the
     * entire UI has finished updating
     *
     * @param task The task to run
     */
    void queue(Runnable task);

    /**
     * Set how this component should arrange its children
     *
     * @param horizontalAlignment The horizontal alignment method to use
     * @param verticalAlignment   The vertical alignment method to use
     */
    default ParentUIComponent alignment(HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment) {
        this.horizontalAlignment(horizontalAlignment);
        this.verticalAlignment(verticalAlignment);
        return this;
    }

    /**
     * Set how this component should vertically arrange its children
     *
     * @param alignment The new alignment method to use
     */
    ParentUIComponent verticalAlignment(VerticalAlignment alignment);

    /**
     * @return How this component vertically arranges its children
     */
    VerticalAlignment verticalAlignment();

    /**
     * Set how this component should horizontally arrange its children
     *
     * @param alignment The new alignment method to use
     */
    ParentUIComponent horizontalAlignment(HorizontalAlignment alignment);

    /**
     * @return How this component horizontally arranges its children
     */
    HorizontalAlignment horizontalAlignment();

    /**
     * Set the internal padding of this component
     *
     * @param padding The new padding to use
     */
    ParentUIComponent padding(Insets padding);

    /**
     * @return The internal padding of this component
     */
    AnimatableProperty<Insets> padding();

    /**
     * Set if this component should let its children overflow
     * its bounding box
     *
     * @param allowOverflow {@code true} if this component should let
     *                      its children overflow its bounding box
     */
    ParentUIComponent allowOverflow(boolean allowOverflow);

    /**
     * @return {@code true} if this component allows its
     *         children to overflow its bounding box
     */
    boolean allowOverflow();

    /**
     * Set the surface this component uses
     *
     * @param surface The new surface to use
     */
    ParentUIComponent surface(Surface surface);

    /**
     * @return The surface this component currently uses
     */
    Surface surface();

    /**
     * @return The children of this component
     */
    List<UIComponent> children();

    /**
     * Remove the given child from this component
     */
    ParentUIComponent removeChild(UIComponent child);

    @Override
    default void drawTooltip(UIGuiGraphics g, int mouseX, int mouseY, float partialTicks, float delta) {
        UIComponent.super.drawTooltip(g, mouseX, mouseY, partialTicks, delta);

        if (!this.allowOverflow()) {
            var padding = this.padding().get();
            ScissorStack.push(this.x() + padding.left(), this.y() + padding.top(), this.width() - padding.horizontal(),
                    this.height() - padding.vertical(), g.pose());
        }

        for (var child : this.children()) {
            if (!child.enabled() || !ScissorStack.isVisible(mouseX, mouseY, g.pose())) continue;

            g.pose().pushPose();
            g.pose().translate(0, 0, child.zIndex());
            child.drawTooltip(g, mouseX, mouseY, partialTicks, delta);
            g.pose().popPose();
        }

        if (!this.allowOverflow()) {
            ScissorStack.pop();
        }
    }

    @Override
    default boolean onMouseMoved(double mouseX, double mouseY) {
        var iter = this.children().listIterator(this.children().size());

        while (iter.hasPrevious()) {
            var child = iter.previous();
            if (!child.enabled() || !child.isInBoundingBox(this.x() + mouseX, this.y() + mouseY)) continue;
            if (child.onMouseMoved(this.x() + mouseX - child.x(), this.y() + mouseY - child.y())) {
                return true;
            }
        }

        return false;
    }

    @Override
    default boolean onMouseDown(double mouseX, double mouseY, int button) {
        var iter = this.children().listIterator(this.children().size());

        while (iter.hasPrevious()) {
            var child = iter.previous();
            if (!child.enabled() || !child.isInBoundingBox(this.x() + mouseX, this.y() + mouseY)) continue;
            if (child.onMouseDown(this.x() + mouseX - child.x(), this.y() + mouseY - child.y(), button)) {
                return true;
            }
        }

        return false;
    }

    @Override
    default boolean onMouseScroll(double mouseX, double mouseY, double amount) {
        var iter = this.children().listIterator(this.children().size());

        while (iter.hasPrevious()) {
            var child = iter.previous();
            if (!child.enabled() || !child.isInBoundingBox(this.x() + mouseX, this.y() + mouseY)) continue;
            if (child.onMouseScroll(this.x() + mouseX - child.x(), this.y() + mouseY - child.y(), amount)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @apiNote When overriding update and calling {@code ParentComponent.super.update()},
     *          ensure that {@link UIComponent#update(float, int, int)} is called as well, through some means
     */
    @Override
    default void update(float delta, int mouseX, int mouseY) {
        UIComponent.super.update(delta, mouseX, mouseY);
        this.padding().update(delta);

        for (int i = 0; i < this.children().size(); i++) {
            if (!this.children().get(i).enabled()) continue;
            this.children().get(i).update(delta, mouseX, mouseY);
        }
    }

    @Override
    default void tick() {
        UIComponent.super.tick();
        for (UIComponent child : children()) {
            if (!child.enabled()) continue;
            child.tick();
        }
    }

    @Override
    default void init() {
        UIComponent.super.init();
        for (UIComponent child : children()) {
            child.init();
        }
    }

    @Override
    default void dispose() {
        UIComponent.super.dispose();
        for (UIComponent child : children()) {
            child.dispose();
        }
    }

    @Override
    default void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        UIComponent.super.parseProperties(model, element, children);
        UIParsing.apply(children, "padding", Insets::parse, this::padding);
        UIParsing.apply(children, "surface", Surface::parse, this::surface);
        UIParsing.apply(children, "vertical-alignment", VerticalAlignment::parse, this::verticalAlignment);
        UIParsing.apply(children, "horizontal-alignment", HorizontalAlignment::parse, this::horizontalAlignment);
        UIParsing.apply(children, "allow-overflow", UIParsing::parseBool, this::allowOverflow);
    }

    /**
     * Recursively find the child with the given id in the
     * hierarchy below this component
     *
     * @param id The id to search for
     * @return The child with the given id, or {@code null} if
     *         none was found
     */
    @SuppressWarnings("unchecked")
    default <T extends UIComponent> T childById(@NotNull Class<T> expectedClass, @NotNull String id) {
        var iter = this.children().listIterator(this.children().size());

        while (iter.hasPrevious()) {
            var child = iter.previous();
            if (Objects.equals(child.id(), id)) {

                if (!expectedClass.isAssignableFrom(child.getClass())) {
                    throw new IncompatibleUIModelException(
                            "Expected child with id '" + id + "'" + " to be a " + expectedClass.getSimpleName() +
                                    " but it is a " + child.getClass().getSimpleName());
                }

                return (T) child;
            } else if (child instanceof ParentUIComponent parent) {
                var candidate = parent.childById(expectedClass, id);
                if (candidate != null) return candidate;
            }
        }

        return null;
    }

    default List<UIComponent> childrenByPattern(Pattern regex) {
        List<UIComponent> list = new ArrayList<>();
        childrenByPattern(list, regex);
        return list;
    }

    private void childrenByPattern(List<UIComponent> list, Pattern regex) {
        for (UIComponent component : this.children()) {
            if (regex.matcher(Strings.nullToEmpty(component.id())).find()) {
                list.add(component);
            }
            if (component instanceof ParentUIComponent parentComponent) {
                parentComponent.childrenByPattern(list, regex);
            }
        }
    }

    /**
     * Get the most specific child at the given coordinates
     *
     * @param x The x-coordinate to query
     * @param y The y-coordinate to query
     * @return The most specific child at the given coordinates,
     *         or {@code null} if there is none
     */
    default @Nullable UIComponent childAt(int x, int y) {
        var iter = this.children().listIterator(this.children().size());

        while (iter.hasPrevious()) {
            var child = iter.previous();
            if (child.isInBoundingBox(x, y)) {
                if (child instanceof ParentUIComponent parent) {
                    return parent.childAt(x, y);
                } else {
                    return child;
                }
            }
        }

        return this.isInBoundingBox(x, y) ? this : null;
    }

    @Nullable
    @Override
    default UIComponent getHoveredComponent(int mouseX, int mouseY) {
        for (int i = children().size() - 1; i >= 0; i--) {
            UIComponent child = children().get(i);
            if (child.enabled()) {
                child = child.getHoveredComponent(mouseX, mouseY);
                if (child != null) {
                    return child;
                }
            }
        }
        return UIComponent.super.getHoveredComponent(mouseX, mouseY);
    }

    /**
     * Collect the entire component hierarchy below the given component
     * into the given list
     *
     * @param into The list into which to collect the hierarchy
     */
    default void collectDescendants(List<UIComponent> into) {
        this.forEachDescendant(into::add);
    }

    /**
     * Run the given callback function for every
     * descendant of this component
     *
     * @param action The action to execute for each descendant
     */
    default void forEachDescendant(Consumer<UIComponent> action) {
        action.accept(this);
        for (var child : this.children()) {
            if (child instanceof ParentUIComponent parent) {
                parent.forEachDescendant(action);
            } else {
                action.accept(child);
            }
        }
    }
}
