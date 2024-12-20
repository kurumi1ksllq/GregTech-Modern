package com.gregtechceu.gtceu.api.ui.base;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.ui.component.SlotComponent;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.ingredient.ClickableIngredientSlot;
import com.gregtechceu.gtceu.api.ui.ingredient.GhostIngredientSlot;
import com.gregtechceu.gtceu.api.ui.inject.GreedyInputUIComponent;
import com.gregtechceu.gtceu.api.ui.util.DisposableScreen;
import com.gregtechceu.gtceu.api.ui.util.UIErrorToast;
import com.gregtechceu.gtceu.api.ui.util.pond.UISlotExtension;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;

import lombok.Getter;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.function.BiFunction;
import java.util.stream.Stream;

public abstract class BaseContainerScreen<R extends ParentUIComponent, C extends AbstractContainerMenu>
                                         extends AbstractContainerScreen<C> implements DisposableScreen {

    /**
     * The UI adapter of this screen. This handles
     * all user input as well as setting up GL state for rendering
     * and managing component focus
     */
    @Getter
    @Nullable
    protected UIAdapter<R> uiAdapter = null;

    /**
     * Whether this screen has encountered an unrecoverable
     * error during its lifecycle and should thus close
     * itself on the next frame
     */
    protected boolean invalid = false;

    protected BaseContainerScreen(C handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }

    /**
     * Initialize the UI adapter for this screen. Usually
     * the body of this method will simply consist of a call
     * to {@link UIAdapter#create(Screen, BiFunction)}
     *
     * @return The UI adapter for this screen to use
     */
    protected abstract @Nullable UIAdapter<R> createAdapter();

    /**
     * Build the component hierarchy of this screen,
     * called after the adapter and root component have been
     * initialized by {@link #createAdapter()}
     *
     * @param rootComponent The root component created in the previous initialization step
     */
    protected abstract void build(R rootComponent);

    @Override
    protected void init() {
        if (this.invalid) return;

        // Check whether this screen was already initialized
        if (this.uiAdapter != null) {
            super.init();
            this.uiAdapter.leftPos(leftPos);
            this.uiAdapter.topPos(topPos);
            // If it was, only resize the adapter instead of recreating it - this preserves UI state
            this.uiAdapter.moveAndResize(0, 0, this.width, this.height);
            // Re-add it as a child to circumvent vanilla clearing them
            this.addRenderableWidget(this.uiAdapter);
        } else {
            try {
                this.uiAdapter = this.createAdapter();
                if (this.uiAdapter == null) {
                    this.invalid = true;
                    this.onClose();
                    return;
                }
                if (!this.renderables.contains(this.uiAdapter)) {
                    this.addRenderableWidget(this.uiAdapter);
                    this.setFocused(this.uiAdapter);
                }

                this.uiAdapter.screen(this);
                this.build(this.uiAdapter.rootComponent);
                this.uiAdapter.rootComponent.containerAccess(this.uiAdapter);
                this.uiAdapter.rootComponent.init();

                MutableInt width = new MutableInt(0);
                MutableInt height = new MutableInt(0);
                this.uiAdapter.rootComponent.forEachDescendant(child -> {
                    if (child.width() > width.getValue()) {
                        width.setValue(child.width());
                    }
                    if (child.height() > height.getValue()) {
                        height.setValue(child.height());
                    }
                });
                this.imageWidth = width.getValue();
                this.imageHeight = height.getValue();
                super.init();
                this.uiAdapter.leftPos(leftPos);
                this.uiAdapter.topPos(topPos);

                this.uiAdapter.moveAndResize(0, 0, this.width, this.height);
            } catch (Exception error) {
                GTCEu.LOGGER.warn("Could not initialize gtceu screen", error);
                UIErrorToast.report(error);
                this.invalid = true;
            }
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.uiAdapter.rootComponent.tick();
    }

    /**
     * Disable the slot at the given index. Note
     * that this is hard override and the slot cannot
     * re-enable itself
     *
     * @param index The index of the slot to disable
     */
    protected void disableSlot(int index) {
        disableSlot(this.menu.slots.get(index));
    }

    /**
     * Disable the given slot. Note that
     * this is hard override and the slot cannot
     * re-enable itself
     */
    protected void disableSlot(Slot slot) {
        ((UISlotExtension) slot).gtceu$setDisabledOverride(true);
    }

    /**
     * Enable the slot at the given index. Note
     * that this is an override and cannot enable
     * a slot that is disabled through its own will
     *
     * @param index The index of the slot to enable
     */
    protected void enableSlot(int index) {
        enableSlot(this.menu.slots.get(index));
    }

    /**
     * Enable the given slot. Note that
     * this is an override and cannot enable
     * a slot that is disabled through its own will
     */
    protected void enableSlot(Slot slot) {
        ((UISlotExtension) slot).gtceu$setDisabledOverride(false);
    }

    protected boolean isSlotEnabled(int index) {
        return ((UISlotExtension) this.menu.slots.get(index)).gtceu$getDisabledOverride();
    }

    protected boolean isSlotEnabled(Slot slot) {
        return ((UISlotExtension) slot).gtceu$getDisabledOverride();
    }

    /**
     * A convenience shorthand for querying a component from the adapter's
     * root component via {@link ParentUIComponent#childById(Class, String)}
     */
    protected <C extends UIComponent> @Nullable C component(Class<C> expectedClass, String id) {
        return this.uiAdapter.rootComponent.childById(expectedClass, id);
    }

    /**
     * Compute a stream of all components for which to
     * generate exclusion areas in a recipe viewer overlay.
     * Called by the JEI, REI and EMI plugins
     */
    @ApiStatus.OverrideOnly
    public Stream<UIComponent> componentsForExclusionAreas() {
        if (this.children().isEmpty()) return Stream.of();

        var rootComponent = uiAdapter.rootComponent;
        var children = new ArrayList<UIComponent>();

        rootComponent.collectDescendants(children);
        children.remove(rootComponent);

        return children.stream()
                .filter(component -> !(component instanceof ParentUIComponent parent) ||
                        parent.surface() != Surface.BLANK);
    }

    /**
     * Compute a stream of all components for which to
     * generate exclusion areas in a recipe viewer overlay.
     * Called by the JEI, REI and EMI plugins
     */
    @ApiStatus.OverrideOnly
    public Stream<GhostIngredientSlot<?>> componentsForGhostIngredients() {
        if (this.children().isEmpty()) return Stream.of();

        var rootComponent = uiAdapter.rootComponent;
        var children = new ArrayList<UIComponent>();

        rootComponent.collectDescendants(children);
        children.remove(rootComponent);

        return children.stream()
                .filter(component -> !(component instanceof ParentUIComponent parent) ||
                        parent.surface() != Surface.BLANK)
                .filter(component -> component instanceof GhostIngredientSlot<?>)
                .map(component -> (GhostIngredientSlot<?>) component);
    }

    /**
     * Compute a stream of all components for which to
     * generate exclusion areas in a recipe viewer overlay.
     * Called by the JEI, REI and EMI plugins
     */
    @ApiStatus.OverrideOnly
    public Stream<ClickableIngredientSlot<?>> componentsForClickableIngredients() {
        if (this.children().isEmpty()) return Stream.of();

        var rootComponent = uiAdapter.rootComponent;
        var children = new ArrayList<UIComponent>();

        rootComponent.collectDescendants(children);
        children.remove(rootComponent);

        return children.stream()
                .filter(component -> !(component instanceof ParentUIComponent parent) ||
                        parent.surface() != Surface.BLANK)
                .filter(component -> component instanceof ClickableIngredientSlot<?>)
                .map(component -> (ClickableIngredientSlot<?>) component);
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        if (this.invalid) {
            this.onClose();
            return;
        }
        var context = UIGuiGraphics.of(guiGraphics);
        super.render(context, mouseX, mouseY, delta);

        if (this.uiAdapter.enableInspector) {
            context.pose().translate(0, 0, 500);

            for (int i = 0; i < this.menu.slots.size(); i++) {
                var slot = this.menu.slots.get(i);
                if (!slot.hasItem()) continue;

                context.drawText(Component.literal("H:" + i),
                        this.leftPos + slot.x + 15, this.topPos + slot.y + 9, .5f, 0x0096FF,
                        UIGuiGraphics.TextAnchor.BOTTOM_RIGHT);
                context.drawText(Component.literal("I:" + slot.getContainerSlot()),
                        this.leftPos + slot.x + 15, this.topPos + slot.y + 15, .5f, 0x5800FF,
                        UIGuiGraphics.TextAnchor.BOTTOM_RIGHT);
            }

            context.pose().translate(0, 0, -500);
        }
        this.renderTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int x, int y) {
        if (uiAdapter.enableInspector) {
            return;
        }
        super.renderTooltip(guiGraphics, x, y);
        var context = UIGuiGraphics.of(guiGraphics);
        uiAdapter.renderTooltip(context, x, y);
    }

    // stop the MC labels from rendering entirely.
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {}

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
            this.onClose();
            return true;
        }

        return (modifiers & GLFW.GLFW_MOD_CONTROL) == 0 &&
                this.uiAdapter.rootComponent.focusHandler().focused() instanceof GreedyInputUIComponent inputComponent ?
                        inputComponent.onKeyPress(keyCode, scanCode, modifiers) :
                        super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return this.uiAdapter.mouseDragged(mouseX, mouseY, button, deltaX, deltaY) ||
                super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    protected void slotClicked(Slot slot, int slotId, int mouseButton, ClickType type) {
        if (slot != null &&
                uiAdapter.rootComponent.getHoveredComponent(slot.x - 1 + leftPos, slot.y - 1 + topPos) instanceof SlotComponent slotComponent) {
            if (slotComponent.slotClick(mouseButton, type, this.menu.player())) {
                return;
            }
        }
        super.slotClicked(slot, slotId, mouseButton, type);
    }

    @Nullable
    @Override
    public GuiEventListener getFocused() {
        return this.uiAdapter;
    }

    @Override
    public void removed() {
        super.removed();
        if (this.uiAdapter != null) {
            this.uiAdapter.cursorAdapter.applyStyle(CursorStyle.NONE);
        }
    }

    @Override
    public void dispose() {
        if (this.uiAdapter != null) this.uiAdapter.dispose();
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float delta, int mouseX, int mouseY) {}

    @Override
    public void onClose() {
        super.onClose();
        if (this.uiAdapter != null) this.uiAdapter.dispose();
    }
}
