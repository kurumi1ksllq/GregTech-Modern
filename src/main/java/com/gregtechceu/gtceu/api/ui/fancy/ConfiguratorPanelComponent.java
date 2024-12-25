package com.gregtechceu.gtceu.api.ui.fancy;

import com.gregtechceu.gtceu.api.ui.component.ButtonComponent;
import com.gregtechceu.gtceu.api.ui.component.UIComponents;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

// TODO remove fixed offsets in favor of dynamic ones
@ApiStatus.Internal
public class ConfiguratorPanelComponent extends FlowLayout {

    @Getter
    protected List<Tab> tabs = new ArrayList<>();
    @Getter
    @Nullable
    protected Tab expanded;
    @Setter
    protected int border = 4;
    @Setter
    protected Surface texture = Surface.UI_BACKGROUND;

    public ConfiguratorPanelComponent() {
        super(Sizing.fixed(24), Sizing.content(2), Algorithm.HORIZONTAL);
    }

    public void clear() {
        clearChildren();
        tabs.clear();
        expanded = null;
    }

    public int getTabSize() {
        return width;
    }

    public void attachConfigurators(IFancyConfigurator... fancyConfigurators) {
        for (IFancyConfigurator fancyConfigurator : fancyConfigurators) {
            var tab = new Tab(fancyConfigurator);
            tab.surface(texture);
            tabs.add(tab);
            tab.sizing(Sizing.fixed(0));
            tab.horizontalSizing().animate(getAnimationTime(), Easing.QUADRATIC, Sizing.fixed(0));
            tab.verticalSizing().animate(getAnimationTime(), Easing.QUADRATIC, Sizing.fixed(0));
        }
        applySizing();
    }

    public void expandTab(Tab tab) {
        tab.expand();
        int i = 0;
        for (Tab otherTab : tabs) {
            if (otherTab != tab) {
                otherTab.collapseTo(0, i++ * (getTabSize() + 2));
            }
        }
        expanded = tab;
    }

    public void collapseTab() {
        if (expanded != null) {
            for (int i = 0; i < tabs.size(); i++) {
                tabs.get(i).collapseTo(0, i * (getTabSize() + 2));
            }
            if (expanded instanceof FloatingTab) {
                expanded.collapseTo(0, 0);
            }
        }
        expanded = null;
    }

    @Override
    protected void drawChildren(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta,
                                List<? extends UIComponent> children) {
        for (UIComponent widget : children) {
            if (widget != expanded) {
                RenderSystem.setShaderColor(1, 1, 1, 1);
                RenderSystem.enableBlend();
                widget.draw(graphics, mouseX, mouseY, partialTicks, delta);
            }
        }
        if (expanded != null) {
            graphics.pose().pushPose();
            graphics.pose().translate(0, 0, 300);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableBlend();
            expanded.draw(graphics, mouseX, mouseY, partialTicks, delta);
            graphics.pose().popPose();
        }
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (expanded != null && expanded.onMouseDown(mouseX, mouseY, button)) {
            return true;
        }
        return super.onMouseDown(mouseX, mouseY, button);
    }

    public FloatingTab createFloatingTab(IFancyConfigurator configurator) {
        return new FloatingTab(configurator);
    }

    public class Tab extends FlowLayout {

        protected final IFancyConfigurator configurator;
        protected final ButtonComponent button;
        @Nullable
        protected FlowLayout view;
        // dragging
        protected double lastDeltaX, lastDeltaY;
        protected int dragOffsetX, dragOffsetY;
        protected boolean isDragging;

        public Tab(IFancyConfigurator configurator) {
            super(Sizing.fixed(getTabSize()), Sizing.fixed(getTabSize()), Algorithm.HORIZONTAL);
            padding(Insets.of(getTabSize()));
            positioning(Positioning.absolute(0, tabs.size() * (getTabSize() + 2)));
            this.configurator = configurator;
            this.button = new ButtonComponent(Component.empty(), b -> {}) {

                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                    if (this.active && this.visible) {
                        Tab.this.onClick(this, button);
                        this.playDownSound(Minecraft.getInstance().getSoundManager());
                        return true;
                    }
                    return false;
                }

                @Override
                public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
                    if (!(configurator instanceof IFancyCustomMouseWheelAction hasActions)) return false;
                    if (isMouseOverElement(mouseX, mouseY)) {
                        //return hasActions.mouseWheelMove(this::sendMessage, mouseX, mouseY, delta);
                    }
                    return false;
                }

                /*
                @Override
                public void receiveMessage(int id, FriendlyByteBuf buf) {
                    if (configurator instanceof IFancyCustomClientActionHandler handler && id > 1)
                        handler.receiveMessage(id, buf);
                    else
                        super.receiveMessage(id, buf);
                }
                */
            };
            button.positioning(Positioning.relative(100, 0))
                    .sizing(Sizing.fill());
            if (configurator instanceof IFancyConfiguratorButton) {
                this.view = null;
                this.child(button);
            }
            // moved else block to containerAccess() below
        }

        @Override
        public void containerAccess(UIComponentMenuAccess access) {
            super.containerAccess(access);
            if (!(configurator instanceof IFancyConfiguratorButton)) {
                var config = configurator.createConfigurator((UIAdapter<StackLayout>) access.adapter());
                config.positioning(Positioning.absolute(border, 0));

                this.view = UIContainers.horizontalFlow(Sizing.content(), Sizing.content());
                this.view.padding(Insets.both(getTabSize() + border, border * 2))
                        .<FlowLayout>configure(c -> {
                            c.enabled(false)
                                    .sizing(Sizing.fixed(config.width() + border * 2),
                                            Sizing.fixed(config.height() + button.height() + border));
                        })
                        .child(config)
                        .child(UIComponents.label(configurator.getTitle())
                                .maxWidth(config.width() - getTabSize())
                                .sizing(Sizing.fixed(config.width() - getTabSize() - 5),
                                        Sizing.fixed(getTabSize() - border))
                                .positioning(Positioning.relative(border + 5, border)));
                this.child(button);
                this.child(view);
            }
        }

        /*
        @Override
        public void receiveMessage(int id, FriendlyByteBuf buf) {
            if (id == 0) {
                configurator.readUpdateInfo(buf.readVarInt(), buf);
            } else {
                super.receiveMessage(id, buf);
            }
        }
        */

        @Override
        public void onChildMutated(UIComponent child) {
            super.onChildMutated(child);
            if (this.view != null && this.view == child) {
                if (expanded == this) {
                    positioning().animate(getAnimationTime(), Easing.QUADRATIC,
                            Positioning.absolute(dragOffsetX + (-width() + (tabs.size() > 1 ? -2 : getTabSize())),
                                    dragOffsetY))
                            .finished().subscribe((dir, looping) -> {
                                child(view);
                            });
                    horizontalSizing().animate(getAnimationTime(), Easing.QUADRATIC, Sizing.content());
                    verticalSizing().animate(getAnimationTime(), Easing.QUADRATIC, Sizing.content());
                }
            }
        }

        private void onClick(ButtonComponent clickData, int button) {
            if (button == 2 && configurator instanceof IFancyCustomMiddleClickAction middleAction) {
                //middleAction.onMiddleClick(this::sendMessage);
            } else if (configurator instanceof IFancyConfiguratorButton fancyButton) {
                fancyButton.onClick(clickData);
            } else {
                if (expanded == this) {
                    collapseTab();
                } else {
                    expandTab(this);
                }
            }
        }

        private void expand() {
            if (view == null) return;
            this.dragOffsetX = 0;
            this.dragOffsetY = 0;
            if (parent().x() - width + (tabs.size() > 1 ? -2 : getTabSize()) < 0) {
                this.dragOffsetX -= (this.x() - width +
                        (tabs.size() > 1 ? -2 : getTabSize()));
            }
            if (parent().y() + height > containerAccess().adapter().height()) {
                this.dragOffsetY -= this.y() + height - containerAccess().adapter().height();
            }

            positioning().animate(getAnimationTime(), Easing.QUADRATIC, Positioning.absolute(
                    dragOffsetX - width + (tabs.size() > 1 ? -2 : getTabSize()),
                    dragOffsetY))
                    .finished().subscribe((dir, looping) -> {
                        child(view);
                    });
            horizontalSizing().animate(getAnimationTime(), Easing.QUADRATIC, Sizing.content());
            verticalSizing().animate(getAnimationTime(), Easing.QUADRATIC, Sizing.content());
        }

        protected void collapseTo(int x, int y) {
            if (view != null) {
                removeChild(view);
            }

            positioning().animate(getAnimationTime(), Easing.QUADRATIC, Positioning.absolute(x, y));
            horizontalSizing().animate(getAnimationTime(), Easing.QUADRATIC, Sizing.fixed(getTabSize()));
            verticalSizing().animate(getAnimationTime(), Easing.QUADRATIC, Sizing.fixed(getTabSize()));
        }

        @Override
        protected void parentUpdate(float delta, int mouseX, int mouseY) {
            super.parentUpdate(delta, mouseX, mouseY);
            if (UIComponent.isMouseOver(x + width - 20, y + 4, 16, 16, mouseX, mouseY)) {
                tooltip(configurator.getTooltips());
            }

            /*
            configurator.detectAndSendChange((id, sender) -> sendMessage(0, buf -> {
                buf.writeVarInt(id);
                sender.accept(buf);
            }));
            */
        }

        @Override
        public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
            if (this.positioning.animation() == null || this.positioning().animation().isFinished()) {
                graphics.enableScissor(x + border - 1, y + border - 1,
                        x + border - 1 + width - (border - 1) * 2,
                        y + border - 1 + height - (border - 1) * 2);
                drawChildren(graphics, mouseX, mouseY, partialTicks, delta, this.children);
                graphics.disableScissor();
            } else {
                drawChildren(graphics, mouseX, mouseY, partialTicks, delta, this.children);
            }
            configurator.getIcon().draw(graphics, mouseX, mouseY, x + width - 20, y + 4, 16, 16);
        }

        @Override
        public boolean onMouseDown(double mouseX, double mouseY, int button) {
            this.lastDeltaX = 0;
            this.lastDeltaY = 0;
            this.isDragging = false;
            if (expanded == this && UIComponent.isMouseOver(x, y, width - getTabSize(),
                    getTabSize(), mouseX, mouseY)) {
                isDragging = true;
                return true;
            }
            return super.onMouseDown(mouseX, mouseY, button) || isMouseOverElement(mouseX, mouseY);
        }

        @Override
        public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
            double dx = deltaX + lastDeltaX;
            double dy = deltaY + lastDeltaY;
            deltaX = (int) dx;
            deltaY = (int) dy;
            lastDeltaX = dx - deltaX;
            lastDeltaY = dy - deltaY;
            if (isDragging) {
                this.dragOffsetX += (int) deltaX;
                this.dragOffsetY += (int) deltaY;
                this.moveTo(this.x() + (int) deltaX, this.y() + (int) deltaY);
            }
            return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button) || isMouseOverElement(mouseX, mouseY);
        }

        @Override
        public boolean onMouseUp(double mouseX, double mouseY, int button) {
            this.lastDeltaX = 0;
            this.lastDeltaY = 0;
            this.isDragging = false;
            return super.onMouseUp(mouseX, mouseY, button) || isMouseOverElement(mouseX, mouseY);
        }

        @Override
        public boolean onMouseScroll(double mouseX, double mouseY, double amount) {
            return super.onMouseScroll(mouseX, mouseY, amount) || isMouseOverElement(mouseX, mouseY);
        }

        @Override
        public boolean onMouseMoved(double mouseX, double mouseY) {
            return super.onMouseMoved(mouseX, mouseY) || isMouseOverElement(mouseX, mouseY);
        }
    }

    public class FloatingTab extends Tab {

        protected Runnable closeCallback = () -> {};

        public FloatingTab(IFancyConfigurator configurator) {
            super(configurator);
            if (this.view != null) {
                this.view.surface(Surface.UI_BACKGROUND);
            }
        }

        @Override
        public void collapseTo(int x, int y) {
            super.collapseTo(x, y);
            ConfiguratorPanelComponent.this.removeChild(this);
            closeCallback.run();
        }

        public void onClose(Runnable closeCallback) {
            this.closeCallback = closeCallback;
        }
    }

    private static int getAnimationTime() {
        return ConfigHolder.INSTANCE.client.animationTime;
    }
}
