package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.parsing.UIModel;
import com.gregtechceu.gtceu.api.ui.parsing.UIModelParsingException;
import com.gregtechceu.gtceu.api.ui.parsing.UIParsing;
import com.gregtechceu.gtceu.api.ui.texture.NinePatchTexture;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.util.ClickData;
import com.gregtechceu.gtceu.core.mixins.ui.accessor.AbstractWidgetAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.function.Consumer;

@Accessors(fluent = true, chain = true)
public class ButtonComponent extends Button {

    public static final ResourceLocation ACTIVE_TEXTURE = new ResourceLocation("owo", "button/active");
    public static final ResourceLocation HOVERED_TEXTURE = new ResourceLocation("owo", "button/hovered");
    public static final ResourceLocation DISABLED_TEXTURE = new ResourceLocation("owo", "button/disabled");

    @Getter
    @Setter
    protected Renderer renderer = Renderer.VANILLA;
    @Getter
    @Setter
    protected boolean textShadow = true;
    @Setter
    protected Consumer<ClickData> onPress;

    protected ButtonComponent(Component message, Consumer<ClickData> onPress) {
        super(0, 0, 0, 0, message, button -> {}, Button.DEFAULT_NARRATION);
        this.onPress = onPress;
        this.sizing(Sizing.content());
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        this.renderer.draw((UIGuiGraphics) graphics, this, delta);

        var textRenderer = Minecraft.getInstance().font;
        int color = this.active ? 0xffffff : 0xa0a0a0;

        if (this.textShadow) {
            graphics.drawCenteredString(textRenderer, this.getMessage(), this.getX() + this.width / 2,
                    this.getY() + (this.height - 8) / 2, color);
        } else {
            graphics.drawString(textRenderer, this.getMessage(),
                    (int) (this.getX() + this.width / 2f - textRenderer.width(this.getMessage()) / 2f),
                    (int) (this.getY() + (this.height - 8) / 2f), color, false);
        }

        Tooltip tooltip = ((AbstractWidgetAccessor) this).gtceu$getTooltip();
        if (this.isHovered && tooltip != null)
            graphics.renderTooltip(textRenderer, tooltip.toCharSequence(Minecraft.getInstance()),
                    DefaultTooltipPositioner.INSTANCE, mouseX, mouseY);
    }

    public ButtonComponent active(boolean active) {
        this.active = active;
        return this;
    }

    public boolean active() {
        return this.active;
    }

    public ButtonComponent visible(boolean visible) {
        this.visible = visible;
        return this;
    }

    @Override
    public ButtonComponent enabled(boolean enabled) {
        super.enabled(enabled);
        this.active(enabled);
        this.visible(enabled);
        return this;
    }

    public boolean visible() {
        return this.visible;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.enabled() && this.visible() && isMouseOverElement(mouseX, mouseY)) {
            ClickData clickData = new ClickData(button);
            //sendMessage(1, clickData::writeToBuf);
            if (onPress != null) {
                onPress.accept(clickData);
            }
            UIComponent.playButtonClickSound();
            return true;
        }
        return false;
    }

    @Override
    public void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        super.parseProperties(model, element, children);
        UIParsing.apply(children, "text", UIParsing::parseComponent, this::setMessage);
        UIParsing.apply(children, "text-shadow", UIParsing::parseBool, this::textShadow);
        UIParsing.apply(children, "renderer", Renderer::parse, this::renderer);
    }

    protected CursorStyle gtceu$preferredCursorStyle() {
        return CursorStyle.HAND;
    }

    @FunctionalInterface
    public interface Renderer {

        Renderer EMPTY = (graphics, button, delta) -> {};

        Renderer VANILLA = (graphics, button, delta) -> {
            RenderSystem.enableDepthTest();

            var texture = button.active ? button.isHovered ? HOVERED_TEXTURE : ACTIVE_TEXTURE : DISABLED_TEXTURE;
            NinePatchTexture.draw(texture, graphics, button.getX(), button.getY(), button.width, button.height);
        };

        static Renderer flat(int color, int hoveredColor, int disabledColor) {
            return (context, button, delta) -> {
                RenderSystem.enableDepthTest();

                if (button.active) {
                    if (button.isHovered) {
                        context.fill(button.getX(), button.getY(), button.getX() + button.width,
                                button.getY() + button.height, hoveredColor);
                    } else {
                        context.fill(button.getX(), button.getY(), button.getX() + button.width,
                                button.getY() + button.height, color);
                    }
                } else {
                    context.fill(button.getX(), button.getY(), button.getX() + button.width,
                            button.getY() + button.height, disabledColor);
                }
            };
        }

        static Renderer texture(ResourceLocation texture, int u, int v, int textureWidth, int textureHeight) {
            return (context, button, delta) -> {
                int renderV = v;
                if (!button.active) {
                    renderV += button.height * 2;
                } else if (button.isHovered()) {
                    renderV += button.height;
                }

                context.blit(texture, button.getX(), button.getY(), u, renderV, button.width, button.height,
                        textureWidth, textureHeight);
            };
        }

        static Renderer texture(UITexture texture) {
            return (graphics, button, delta) -> texture.draw(graphics, button);
        }

        static Renderer texture(UITexture base, UITexture hovered, UITexture disabled) {
            return (graphics, button, delta) -> {
                if (button.active) {
                    if (button.isHovered) {
                        hovered.draw(graphics, 0, 0, button.getX(), button.getY(), button.width, button.height);
                    } else {
                        base.draw(graphics, 0, 0, button.getX(), button.getY(), button.width, button.height);
                    }
                } else {
                    disabled.draw(graphics, 0, 0, button.getX(), button.getY(), button.width, button.height);
                }
            };
        }

        void draw(UIGuiGraphics context, ButtonComponent button, float delta);

        static Renderer parse(Element element) {
            var children = UIParsing.<Element>allChildrenOfType(element, Node.ELEMENT_NODE);
            if (children.size() > 1)
                throw new UIModelParsingException("'renderer' declaration may only contain a single child");

            var rendererElement = children.get(0);
            return switch (rendererElement.getNodeName()) {
                case "vanilla" -> VANILLA;
                case "flat" -> {
                    UIParsing.expectAttributes(rendererElement, "color", "hovered-color", "disabled-color");
                    yield flat(
                            Color.parseAndPack(rendererElement.getAttributeNode("color")),
                            Color.parseAndPack(rendererElement.getAttributeNode("hovered-color")),
                            Color.parseAndPack(rendererElement.getAttributeNode("disabled-color")));
                }
                case "texture" -> {
                    UIParsing.expectAttributes(rendererElement, "texture", "u", "v", "texture-width", "texture-height");
                    yield texture(
                            UIParsing.parseResourceLocation(rendererElement.getAttributeNode("texture")),
                            UIParsing.parseUnsignedInt(rendererElement.getAttributeNode("u")),
                            UIParsing.parseUnsignedInt(rendererElement.getAttributeNode("v")),
                            UIParsing.parseUnsignedInt(rendererElement.getAttributeNode("texture-width")),
                            UIParsing.parseUnsignedInt(rendererElement.getAttributeNode("texture-height")));
                }
                default -> throw new UIModelParsingException(
                        "Unknown button renderer '" + rendererElement.getNodeName() + "'");
            };
        }
    }
}
