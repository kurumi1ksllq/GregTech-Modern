package com.gregtechceu.gtceu.api.ui.core;

import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.parsing.UIModelParsingException;
import com.gregtechceu.gtceu.api.ui.parsing.UIParsing;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public interface Surface {

    Surface UI_BACKGROUND = GuiTextures.BACKGROUND::draw;

    Surface UI_DISPLAY = GuiTextures.DISPLAY::draw;

    Surface UI_BACKGROUND_BRONZE = (graphics, component) -> {
        GuiTextures.BACKGROUND_STEAM.get(false).draw(graphics, component);
    };

    Surface UI_BACKGROUND_STEEL = (graphics, component) -> {
        GuiTextures.BACKGROUND_STEAM.get(true).draw(graphics, component);
    };

    Surface UI_BACKGROUND_INVERSE = GuiTextures.BACKGROUND_INVERSE::draw;

    Surface TITLE_BAR_BACKGROUND = GuiTextures.TITLE_BAR_BACKGROUND::draw;

    Surface VANILLA_TRANSLUCENT = (graphics, component) -> {
        graphics.drawGradientRect(
                component.x(), component.y(), component.width(), component.height(),
                0xC0101010, 0xC0101010, 0xD0101010, 0xD0101010);
    };

    Surface OPTIONS_BACKGROUND = (graphics, component) -> {
        RenderSystem.setShaderColor(64 / 255f, 64 / 255f, 64 / 255f, 1);
        graphics.blit(Screen.BACKGROUND_LOCATION, component.x(), component.y(), 0, 0, component.width(),
                component.height(), 32, 32);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    };

    Surface TOOLTIP = (graphics, component) -> {
        var builder = Tesselator.getInstance().getBuilder();
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        TooltipRenderUtil.renderTooltipBackground(graphics, component.x() + 4, component.y() + 4, component.width() - 8,
                component.height() - 8, 0);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator.getInstance().end();
    };

    static Surface blur(float quality, float size) {
        return (graphics, component) -> {
            var builder = Tesselator.getInstance().getBuilder();
            var matrix = graphics.pose().last().pose();

            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
            builder.vertex(matrix, component.x(), component.y(), 0).endVertex();
            builder.vertex(matrix, component.x(), component.y() + component.height(), 0).endVertex();
            builder.vertex(matrix, component.x() + component.width(), component.y() + component.height(), 0)
                    .endVertex();
            builder.vertex(matrix, component.x() + component.width(), component.y(), 0).endVertex();

            // OwoClient.BLUR_PROGRAM.setParameters(16, quality, size);
            // OwoClient.BLUR_PROGRAM.use();
            Tesselator.getInstance().end();
        };
    }

    Surface BLANK = (graphics, component) -> {};

    static Surface flat(int color) {
        return (graphics, component) -> graphics.fill(component.x(), component.y(), component.x() + component.width(),
                component.y() + component.height(), color);
    }

    static Surface outline(int color) {
        return (graphics, component) -> graphics.drawRectOutline(component.x(), component.y(), component.width(),
                component.height(), color);
    }

    static Surface tiled(ResourceLocation texture, int textureWidth, int textureHeight) {
        return (graphics, component) -> {
            graphics.blit(texture, component.x(), component.y(), 0, 0, component.width(), component.height(),
                    textureWidth, textureHeight);
        };
    }

    static Surface panelWithInset(int insetWidth) {
        return Surface.UI_BACKGROUND_INVERSE.and((graphics, component) -> {
            GuiTextures.BACKGROUND_INVERSE.draw(graphics,
                    component.x() + insetWidth,
                    component.y() + insetWidth,
                    component.width() - insetWidth * 2,
                    component.height() - insetWidth * 2);
        });
    }

    void draw(UIGuiGraphics graphics, ParentUIComponent component);

    default Surface and(Surface surface) {
        return (graphics, component) -> {
            this.draw(graphics, component);
            surface.draw(graphics, component);
        };
    }

    static Surface parse(Element surfaceElement) {
        var children = UIParsing.<Element>allChildrenOfType(surfaceElement, Node.ELEMENT_NODE);
        var surface = BLANK;

        for (var child : children) {
            surface = switch (child.getNodeName()) {
                case "panel" -> surface.and(
                        child.getAttribute("inverse").equalsIgnoreCase("true") ? UI_BACKGROUND_INVERSE : UI_BACKGROUND);
                case "steam-panel" -> surface
                        .and(child.getAttribute("steel").equalsIgnoreCase("true") ? UI_BACKGROUND_STEEL :
                                UI_BACKGROUND_BRONZE);
                case "tiled" -> {
                    UIParsing.expectAttributes(child, "texture-width", "texture-height");
                    yield surface.and(tiled(
                            UIParsing.parseResourceLocation(child),
                            UIParsing.parseUnsignedInt(child.getAttributeNode("texture-width")),
                            UIParsing.parseUnsignedInt(child.getAttributeNode("texture-height"))));
                }
                case "blur" -> {
                    UIParsing.expectAttributes(child, "size", "quality");
                    yield surface.and(blur(
                            UIParsing.parseFloat(child.getAttributeNode("quality")),
                            UIParsing.parseFloat(child.getAttributeNode("size"))));
                }
                case "panel-with-inset" -> surface.and(panelWithInset(UIParsing.parseUnsignedInt(child)));
                case "options-background" -> surface.and(OPTIONS_BACKGROUND);
                case "vanilla-translucent" -> surface.and(VANILLA_TRANSLUCENT);
                case "panel-inset" -> surface.and(UI_BACKGROUND_INVERSE);
                case "tooltip" -> surface.and(TOOLTIP);
                case "outline" -> surface.and(outline(Color.parseAndPack(child)));
                case "flat" -> surface.and(flat(Color.parseAndPack(child)));
                default -> throw new UIModelParsingException("Unknown surface type '" + child.getNodeName() + "'");
            };
        }

        return surface;
    }
}
