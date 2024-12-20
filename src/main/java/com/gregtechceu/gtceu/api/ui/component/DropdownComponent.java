package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.base.BaseParentUIComponent;
import com.gregtechceu.gtceu.api.ui.base.BaseUIComponent;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.container.UIContainers;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.parsing.UIModel;
import com.gregtechceu.gtceu.api.ui.parsing.UIParsing;

import com.gregtechceu.gtceu.api.ui.util.ClickData;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@Accessors(fluent = true, chain = true)
public class DropdownComponent extends FlowLayout {

    protected static final ResourceLocation ICONS_TEXTURE = new ResourceLocation("owo",
            "textures/gui/dropdown_icons.png");
    protected final FlowLayout entries;
    @Getter
    @Setter
    protected boolean closeWhenNotHovered = false;

    protected DropdownComponent(Sizing horizontalSizing) {
        super(Sizing.content(), Sizing.content(), FlowLayout.Algorithm.HORIZONTAL);

        this.entries = UIContainers.verticalFlow(horizontalSizing, Sizing.content());
        this.entries.padding(Insets.of(1));
        this.entries.allowOverflow(true);
        this.entries.surface(Surface.flat(0xC7000000)
                .and(Surface.blur(3, 5))
                .and(Surface.outline(0xFF121212)));

        this.child(this.entries);
    }

    /**
     * Open a context menu at the given location in the given screen,
     * adjusting the position if needed to avoid overflowing screen space
     *
     * @param screen        The screen on which to operate
     * @param rootComponent The root component onto which to mount the dropdown
     * @param mountFunction The mounting function to use
     * @param mouseX        The x-coordinate at which to open the dropdown
     * @param mouseY        The y-coordinate at which to open the dropdown
     * @param builder       A function to add entries to the dropdown
     */
    public static <
            R extends ParentUIComponent> DropdownComponent openContextMenu(Screen screen, R rootComponent,
                                                                           BiConsumer<R, DropdownComponent> mountFunction,
                                                                           double mouseX, double mouseY,
                                                                           Consumer<DropdownComponent> builder) {
        var dropdown = new DropdownComponent(Sizing.content());
        builder.accept(dropdown);

        mountFunction.accept(rootComponent, dropdown);

        int xLocation = (int) mouseX - rootComponent.x();
        int yLocation = (int) mouseY - rootComponent.y();

        if (xLocation + dropdown.width() > screen.width) {
            xLocation -= xLocation + dropdown.width() - screen.width;
        }
        if (yLocation + dropdown.height() > screen.height) {
            yLocation -= yLocation + dropdown.height() - screen.height;
        }

        dropdown.positioning(Positioning.absolute(xLocation, yLocation));

        var dismounted = new MutableBoolean(false);
        MinecraftForge.EVENT_BUS.addListener((ScreenEvent.MouseButtonPressed.Pre event) -> {
            if (dismounted.isTrue() || dropdown.isInBoundingBox(event.getMouseX(), event.getMouseY())) return;

            rootComponent.removeChild(dropdown);
            dismounted.setTrue();
        });

        return dropdown;
    }

    @Override
    public BaseParentUIComponent surface(Surface surface) {
        return this.entries.surface(surface);
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        super.draw(graphics, mouseX, mouseY, partialTicks, delta);
        if (this.closeWhenNotHovered && !this.isInBoundingBox(mouseX, mouseY)) {
            this.queue(() -> {
                this.closeWhenNotHovered(false);
                this.parent.removeChild(this);
            });
        }
    }

    @Override
    public void layout(Size space) {
        super.layout(space);

        var entries = this.entries.children();
        for (UIComponent entry : entries) {
            if (!(entry instanceof ResizeableComponent sizeable)) continue;

            sizeable.setWidth(this.entries.width() - this.entries.padding().get().horizontal() -
                    entry.margins().get().horizontal());
        }
    }

    public DropdownComponent divider() {
        this.entries.child(new Divider());
        return this;
    }

    public DropdownComponent text(Component text) {
        this.entries
                .child(UIComponents.label(text).color(Color.ofFormatting(ChatFormatting.GRAY)).margins(Insets.of(2)));
        return this;
    }

    public DropdownComponent button(Component text, BiConsumer<ClickData, DropdownComponent> onClick) {
        this.entries.child(new Button(this, text, onClick).margins(Insets.of(2)));
        return this;
    }

    public DropdownComponent checkbox(Component text, boolean state, Consumer<Boolean> onClick) {
        this.entries.child(new Checkbox(this, text, state, onClick).margins(Insets.of(2)));
        return this;
    }

    public DropdownComponent nested(Component text, Sizing horizontalSizing, Consumer<DropdownComponent> builder) {
        var nested = new DropdownComponent(horizontalSizing);
        builder.accept(nested);
        this.entries.child(new NestEntry(this, text, nested).margins(Insets.of(2)));
        return this;
    }

    @Override
    public FlowLayout removeChild(UIComponent child) {
        if (child == this.entries) {
            this.queue(() -> {
                this.closeWhenNotHovered(false);
                this.parent.removeChild(this);
            });
        }
        return super.removeChild(child);
    }

    @Override
    public void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        super.parseProperties(model, element, children);
        UIParsing.apply(children, "entries", Function.identity(), this::parseAndApplyEntries);
        UIParsing.apply(children, "close-when-not-hovered", UIParsing::parseBool, this::closeWhenNotHovered);
    }

    protected void parseAndApplyEntries(Element container) {
        for (var node : UIParsing.allChildrenOfType(container, Node.ELEMENT_NODE)) {
            var entry = (Element) node;

            switch (entry.getNodeName()) {
                case "divider" -> this.divider();
                case "text" -> this.text(UIParsing.parseComponent(entry));
                case "button" -> {
                    var children = UIParsing.childElements(entry);
                    UIParsing.expectChildren(entry, children, "text");

                    var text = UIParsing.parseComponent(children.get("text"));
                    this.button(text, (cd, dropdownComponent) -> {});
                }
                case "checkbox" -> {
                    var children = UIParsing.childElements(entry);
                    UIParsing.expectChildren(entry, children, "text", "checked");

                    var text = UIParsing.parseComponent(children.get("text"));
                    var checked = UIParsing.parseBool(children.get("checked"));

                    this.checkbox(text, checked, aBoolean -> {});
                }
                case "nested" -> {
                    var text = entry.getAttribute("translate").equals("true") ?
                            Component.translatable(entry.getAttribute("name")) :
                            Component.literal(entry.getAttribute("name"));
                    this.nested(text, Sizing.content(),
                            dropdownComponent -> dropdownComponent.parseAndApplyEntries(entry));
                }
            }
        }
    }

    protected static void drawIconFromTexture(UIGuiGraphics graphics, ParentUIComponent dropdown, int y, int u, int v) {
        graphics.blit(ICONS_TEXTURE,
                dropdown.x() + dropdown.width() - dropdown.padding().get().right() - 10, y,
                u, v,
                9, 9,
                32, 32);
    }

    protected interface ResizeableComponent {

        void setWidth(int width);
    }

    protected static class Divider extends BaseUIComponent implements ResizeableComponent {

        public Divider() {
            this.sizing(Sizing.fixed(1));
        }

        @Override
        public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
            var margins = this.margins.get();
            graphics.fill(
                    this.x - margins.left(),
                    this.y - margins.top(),
                    this.x + this.width + margins.right(),
                    this.y + this.height + margins.bottom(),
                    0xFF121212);
        }

        @Override
        public void setWidth(int width) {
            this.width = width;
        }
    }

    protected static class NestEntry extends LabelComponent {

        private final DropdownComponent child;

        protected NestEntry(DropdownComponent parentDropdown, Component text, DropdownComponent child) {
            super(text);
            this.child = child;

            this.mouseEnter().subscribe(() -> {
                child.margins(Insets.top(this.y - parentDropdown.y));

                parentDropdown.queue(() -> {
                    parentDropdown.removeChild(child);
                    parentDropdown.child(child);
                });
            });
        }

        @Override
        public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
            super.draw(graphics, mouseX, mouseY, partialTicks, delta);
            drawIconFromTexture(graphics, this.parent, this.y, 0, 16);

            this.child.closeWhenNotHovered(!PositionedRectangle.of(this.x, this.y, this.parent.width(), this.height)
                    .isInBoundingBox(mouseX, mouseY));
        }

        @Override
        public int determineHorizontalContentSize(Sizing sizing) {
            return super.determineHorizontalContentSize(sizing) + 17;
        }
    }

    protected static class Button extends LabelComponent implements ResizeableComponent {

        protected final DropdownComponent parentDropdown;
        protected BiConsumer<ClickData, DropdownComponent> onClick;

        protected Button(DropdownComponent parentDropdown, Component text, BiConsumer<ClickData, DropdownComponent> onClick) {
            super(text);
            this.onClick = onClick;
            this.parentDropdown = parentDropdown;

            this.margins(Insets.vertical(1));
            this.cursorStyle(CursorStyle.HAND);
        }

        public void setWidth(int width) {
            this.width = width;
        }

        @Override
        public boolean onMouseDown(double mouseX, double mouseY, int button) {
            super.onMouseDown(mouseX, mouseY, button);

            ClickData cd = new ClickData(button);
            this.onClick.accept(cd, this.parentDropdown);

            return true;
        }

        @Override
        public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
            if (this.isInBoundingBox(mouseX, mouseY)) {
                var margins = this.margins.get();
                graphics.fill(
                        this.x - margins.left(),
                        this.y - margins.top(),
                        this.x + this.width + margins.right(),
                        this.y + this.height + margins.bottom(),
                        0x44FFFFFF);
            }

            super.draw(graphics, mouseX, mouseY, partialTicks, delta);
        }
    }

    protected static class Checkbox extends Button {

        protected boolean state;

        public Checkbox(DropdownComponent parentDropdown, Component text, boolean state, Consumer<Boolean> onClick) {
            super(parentDropdown, text, (cd, dropdownComponent) -> {});

            this.state = state;
            this.onClick = (cd, dropdownComponent) -> {
                this.state = !this.state;
                onClick.accept(this.state);
            };
        }

        @Override
        public void draw(UIGuiGraphics context, int mouseX, int mouseY, float partialTicks, float delta) {
            super.draw(context, mouseX, mouseY, partialTicks, delta);
            drawIconFromTexture(context, this.parent, this.y, this.state ? 16 : 0, 0);
        }

        @Override
        public int determineHorizontalContentSize(Sizing sizing) {
            return super.determineHorizontalContentSize(sizing) + 17;
        }
    }
}
