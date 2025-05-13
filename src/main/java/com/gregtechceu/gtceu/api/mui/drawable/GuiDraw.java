package com.gregtechceu.gtceu.api.mui.drawable;

import com.gregtechceu.gtceu.api.mui.drawable.text.TextRenderer;
import com.gregtechceu.gtceu.api.mui.utils.Color;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

public class GuiDraw {

    public static final double PI2 = Math.PI * 2;
    public static final double PI_2 = Math.PI / 2;

    public static void drawRect(float x0, float y0, float w, float h, int color) {
        drawRect(x0, y0, w, h, color, color, color, color);
    }

    public static void drawHorizontalGradientRect(float x0, float y0, float w, float h, int colorLeft, int colorRight) {
        drawRect(x0, y0, w, h, colorLeft, colorRight, colorLeft, colorRight);
    }

    public static void drawVerticalGradientRect(float x0, float y0, float w, float h, int colorTop, int colorBottom) {
        drawRect(x0, y0, w, h, colorTop, colorTop, colorBottom, colorBottom);
    }

    public static void drawRect(float x0, float y0, float w, float h, int colorTL, int colorTR, int colorBL, int colorBR) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        float x1 = x0 + w, y1 = y0 + h;
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferbuilder.vertex(x0, y0, 0.0f).color(Color.getRed(colorTL), Color.getGreen(colorTL), Color.getBlue(colorTL), Color.getAlpha(colorTL)).endVertex();
        bufferbuilder.vertex(x0, y1, 0.0f).color(Color.getRed(colorBL), Color.getGreen(colorBL), Color.getBlue(colorBL), Color.getAlpha(colorBL)).endVertex();
        bufferbuilder.vertex(x1, y1, 0.0f).color(Color.getRed(colorBR), Color.getGreen(colorBR), Color.getBlue(colorBR), Color.getAlpha(colorBR)).endVertex();
        bufferbuilder.vertex(x1, y0, 0.0f).color(Color.getRed(colorTR), Color.getGreen(colorTR), Color.getBlue(colorTR), Color.getAlpha(colorTR)).endVertex();
        tesselator.end();
        RenderSystem.disableBlend();
    }

    public static void drawCircle(float x0, float y0, float diameter, int color, int segments) {
        drawEllipse(x0, y0, diameter, diameter, color, color, segments);
    }

    public static void drawCircle(float x0, float y0, float diameter, int centerColor, int outerColor, int segments) {
        drawEllipse(x0, y0, diameter, diameter, centerColor, outerColor, segments);
    }

    public static void drawEllipse(float x0, float y0, float w, float h, int color, int segments) {
        drawEllipse(x0, y0, w, h, color, color, segments);
    }

    public static void drawEllipse(float x0, float y0, float w, float h, int centerColor, int outerColor, int segments) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        float x_2 = x0 + w / 2f, y_2 = y0 + h / 2f;
        bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        // start at center
        bufferbuilder.vertex(x_2, y_2, 0.0f).color(Color.getRed(centerColor), Color.getGreen(centerColor), Color.getBlue(centerColor), Color.getAlpha(centerColor)).endVertex();
        int a = Color.getAlpha(outerColor), r = Color.getRed(outerColor), g = Color.getGreen(outerColor), b = Color.getBlue(outerColor);
        float incr = (float) (PI2 / segments);
        for (int i = 0; i <= segments; i++) {
            float angle = incr * i;
            float x = (float) (Math.sin(angle) * (w / 2) + x_2);
            float y = (float) (Math.cos(angle) * (h / 2) + y_2);
            bufferbuilder.vertex(x, y, 0.0f).color(r, g, b, a).endVertex();
        }
        tesselator.end();
        RenderSystem.disableBlend();
    }

    public static void drawRoundedRect(float x0, float y0, float w, float h, int color, int cornerRadius, int segments) {
        drawRoundedRect(x0, y0, w, h, color, color, color, color, cornerRadius, segments);
    }

    public static void drawVerticalGradientRoundedRect(float x0, float y0, float w, float h, int colorTop, int colorBottom, int cornerRadius, int segments) {
        drawRoundedRect(x0, y0, w, h, colorTop, colorTop, colorBottom, colorBottom, cornerRadius, segments);
    }

    public static void drawHorizontalGradientRoundedRect(float x0, float y0, float w, float h, int colorLeft, int colorRight, int cornerRadius, int segments) {
        drawRoundedRect(x0, y0, w, h, colorLeft, colorRight, colorLeft, colorRight, cornerRadius, segments);
    }

    public static void drawRoundedRect(float x0, float y0, float w, float h, int colorTL, int colorTR, int colorBL, int colorBR, int cornerRadius, int segments) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        float x1 = x0 + w, y1 = y0 + h;
        bufferbuilder.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        int color = Color.average(colorBL, colorBR, colorTR, colorTL);
        // start at center
        bufferbuilder.vertex(x0 + w / 2f, y0 + h / 2f, 0.0f).color(Color.getRed(color), Color.getGreen(color), Color.getBlue(color), Color.getAlpha(color)).endVertex();
        // left side
        bufferbuilder.vertex(x0, y0 + cornerRadius, 0.0f).color(Color.getRed(colorTL), Color.getGreen(colorTL), Color.getBlue(colorTL), Color.getAlpha(colorTL)).endVertex();
        bufferbuilder.vertex(x0, y1 - cornerRadius, 0.0f).color(Color.getRed(colorBL), Color.getGreen(colorBL), Color.getBlue(colorBL), Color.getAlpha(colorBL)).endVertex();
        // bottom left corner
        for (int i = 1; i <= segments; i++) {
            float x = (float) (x0 + cornerRadius - Math.cos(PI_2 / segments * i) * cornerRadius);
            float y = (float) (y1 - cornerRadius + Math.sin(PI_2 / segments * i) * cornerRadius);
            bufferbuilder.vertex(x, y, 0.0f).color(Color.getRed(colorBL), Color.getGreen(colorBL), Color.getBlue(colorBL), Color.getAlpha(colorBL)).endVertex();
        }
        // bottom side
        bufferbuilder.vertex(x1 - cornerRadius, y1, 0.0f).color(Color.getRed(colorBR), Color.getGreen(colorBR), Color.getBlue(colorBR), Color.getAlpha(colorBR)).endVertex();
        // bottom right corner
        for (int i = 1; i <= segments; i++) {
            float x = (float) (x1 - cornerRadius + Math.sin(PI_2 / segments * i) * cornerRadius);
            float y = (float) (y1 - cornerRadius + Math.cos(PI_2 / segments * i) * cornerRadius);
            bufferbuilder.vertex(x, y, 0.0f).color(Color.getRed(colorBR), Color.getGreen(colorBR), Color.getBlue(colorBR), Color.getAlpha(colorBR)).endVertex();
        }
        // right side
        bufferbuilder.vertex(x1, y0 + cornerRadius, 0.0f).color(Color.getRed(colorTR), Color.getGreen(colorTR), Color.getBlue(colorTR), Color.getAlpha(colorTR)).endVertex();
        // top right corner
        for (int i = 1; i <= segments; i++) {
            float x = (float) (x1 - cornerRadius + Math.cos(PI_2 / segments * i) * cornerRadius);
            float y = (float) (y0 + cornerRadius - Math.sin(PI_2 / segments * i) * cornerRadius);
            bufferbuilder.vertex(x, y, 0.0f).color(Color.getRed(colorTR), Color.getGreen(colorTR), Color.getBlue(colorTR), Color.getAlpha(colorTR)).endVertex();
        }
        // top side
        bufferbuilder.vertex(x0 + cornerRadius, y0, 0.0f).color(Color.getRed(colorTL), Color.getGreen(colorTL), Color.getBlue(colorTL), Color.getAlpha(colorTL)).endVertex();
        // top left corner
        for (int i = 1; i <= segments; i++) {
            float x = (float) (x0 + cornerRadius - Math.sin(PI_2 / segments * i) * cornerRadius);
            float y = (float) (y0 + cornerRadius - Math.cos(PI_2 / segments * i) * cornerRadius);
            bufferbuilder.vertex(x, y, 0.0f).color(Color.getRed(colorTL), Color.getGreen(colorTL), Color.getBlue(colorTL), Color.getAlpha(colorTL)).endVertex();
        }
        bufferbuilder.vertex(x0, y0 + cornerRadius, 0.0f).color(Color.getRed(colorTL), Color.getGreen(colorTL), Color.getBlue(colorTL), Color.getAlpha(colorTL)).endVertex();
        tesselator.end();
        RenderSystem.disableBlend();
    }

    public static void drawTexture(ResourceLocation location, float x, float y, float w, float h, int u, int v, int textureWidth, int textureHeight) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, location);
        drawTexture(x, y, u, v, w, h, textureWidth, textureHeight);
        RenderSystem.disableBlend();
    }

    public static void drawTexture(float x, float y, int u, int v, float w, float h, int textureW, int textureH) {
        drawTexture(x, y, u, v, w, h, textureW, textureH, 0);
    }

    /**
     * Draw a textured quad with given UV, dimensions and custom texture size
     */
    public static void drawTexture(float x, float y, int u, int v, float w, float h, int textureW, int textureH, float z) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        drawTexture(buffer, x, y, u, v, w, h, textureW, textureH, z);
        tesselator.end();
    }

    public static void drawTexture(BufferBuilder buffer, float x, float y, int u, int v, float w, float h, int textureW, int textureH, float z) {
        float tw = 1F / textureW;
        float th = 1F / textureH;

        buffer.vertex(x, y + h, z).uv(u * tw, (v + h) * th).endVertex();
        buffer.vertex(x + w, y + h, z).uv((u + w) * tw, (v + h) * th).endVertex();
        buffer.vertex(x + w, y, z).uv((u + w) * tw, v * th).endVertex();
        buffer.vertex(x, y, z).uv(u * tw, v * th).endVertex();
    }

    public static void drawTexture(float x, float y, int u, int v, float w, float h, int textureW, int textureH, int tu, int tv) {
        drawTexture(x, y, u, v, w, h, textureW, textureH, tu, tv, 0);
    }

    /**
     * Draw a textured quad with given UV, dimensions and custom texture size
     */
    public static void drawTexture(float x, float y, int u, int v, float w, float h, int textureW, int textureH, int tu, int tv, float z) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        drawTexture(buffer, x, y, u, v, w, h, textureW, textureH, tu, tv, z);
        tesselator.end();
    }

    public static void drawTexture(BufferBuilder buffer, float x, float y, int u, int v, float w, float h, int textureW, int textureH, int tu, int tv, float z) {
        float tw = 1F / textureW;
        float th = 1F / textureH;

        buffer.vertex(x, y + h, z).uv(u * tw, tv * th).endVertex();
        buffer.vertex(x + w, y + h, z).uv(tu * tw, tv * th).endVertex();
        buffer.vertex(x + w, y, z).uv(tu * tw, v * th).endVertex();
        buffer.vertex(x, y, z).uv(u * tw, v * th).endVertex();
    }

    public static void drawTexture(ResourceLocation location, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, location);
        drawTexture(x0, y0, x1, y1, u0, v0, u1, v1, 0);
        RenderSystem.disableBlend();
    }

    public static void drawTexture(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1) {
        drawTexture(x0, y0, x1, y1, u0, v0, u1, v1, 0);
    }

    public static void drawTexture(float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float z) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        drawTexture(buffer, x0, y0, x1, y1, u0, v0, u1, v1, z);
        tesselator.end();
    }

    public static void drawTexture(BufferBuilder buffer, float x0, float y0, float x1, float y1, float u0, float v0, float u1, float v1, float z) {
        buffer.vertex(x0, y1, z).uv(u0, v1).endVertex();
        buffer.vertex(x1, y1, z).uv(u1, v1).endVertex();
        buffer.vertex(x1, y0, z).uv(u1, v0).endVertex();
        buffer.vertex(x0, y0, z).uv(u0, v0).endVertex();
    }

    public static void drawTiledTexture(ResourceLocation location, float x, float y, float w, float h, int u, int v, int tileW, int tileH, int tw, int th, float z) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, location);
        drawTiledTexture(x, y, w, h, u, v, tileW, tileH, tw, th, z);
        RenderSystem.disableBlend();
    }

    public static void drawTiledTexture(float x, float y, float w, float h, int u, int v, int tileW, int tileH, int tw, int th, float z) {
        int countX = (((int) w - 1) / tileW) + 1;
        int countY = (((int) h - 1) / tileH) + 1;
        float fillerX = w - (countX - 1) * tileW;
        float fillerY = h - (countY - 1) * tileH;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (int i = 0, c = countX * countY; i < c; i++) {
            int ix = i % countX;
            int iy = i / countX;
            float xx = x + ix * tileW;
            float yy = y + iy * tileH;
            float xw = ix == countX - 1 ? fillerX : tileW;
            float yh = iy == countY - 1 ? fillerY : tileH;

            drawTexture(buffer, xx, yy, u, v, xw, yh, tw, th, z);
        }

        tesselator.end();
    }

    public static void drawTiledTexture(ResourceLocation location, float x, float y, float w, float h, float u0, float v0, float u1, float v1, int textureWidth, int textureHeight, float z) {
        RenderSystem.enableBlend();
        RenderSystem.setShaderTexture(0, location);
        drawTiledTexture(x, y, w, h, u0, v0, u1, v1, textureWidth, textureHeight, z);
        RenderSystem.disableBlend();
    }

    public static void drawTiledTexture(float x, float y, float w, float h, float u0, float v0, float u1, float v1, int tileWidth, int tileHeight, float z) {
        int countX = (((int) w - 1) / tileWidth) + 1;
        int countY = (((int) h - 1) / tileHeight) + 1;
        float fillerX = w - (countX - 1) * tileWidth;
        float fillerY = h - (countY - 1) * tileHeight;
        float fillerU = u0 + (u1 - u0) * fillerX / tileWidth;
        float fillerV = v0 + (v1 - v0) * fillerY / tileHeight;

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

        for (int i = 0, c = countX * countY; i < c; i++) {
            int ix = i % countX;
            int iy = i / countX;
            float xx = x + ix * tileWidth;
            float yy = y + iy * tileHeight;
            float xw = tileWidth, yh = tileHeight, uEnd = u1, vEnd = v1;
            if (ix == countX - 1) {
                xw = fillerX;
                uEnd = fillerU;
            }
            if (iy == countY - 1) {
                yh = fillerY;
                vEnd = fillerV;
            }

            drawTexture(buffer, xx, yy, xx + xw, yy + yh, u0, v0, uEnd, vEnd, z);
        }

        tesselator.end();
    }

    public static void drawItem(GuiGraphics graphics, ItemStack item, int x, int y, float width, float height, int z) {
        if (item.isEmpty()) return;
        graphics.pose().pushPose();
        RenderSystem.enableDepthTest();
        graphics.pose().translate(x, y, 0);
        graphics.pose().scale(width / 16f, height / 16f, 1);
        graphics.renderItem(item, 0, 0);
        graphics.renderItemDecorations(Minecraft.getInstance().font, item, 0, 0);

        RenderSystem.disableDepthTest();
        graphics.pose().popPose();
    }

    public static void drawFluidTexture(GuiGraphics graphics, FluidStack content,
                                        float x0, float y0, float width, float height, float z) {
        if (content == null) {
            return;
        }
        Fluid fluid = content.getFluid();
        ResourceLocation fluidStill = IClientFluidTypeExtensions.of(fluid).getStillTexture(content);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(fluidStill);
        int fluidColor = IClientFluidTypeExtensions.of(fluid).getTintColor(content);
        graphics.setColor(Color.getRedF(fluidColor), Color.getGreenF(fluidColor), Color.getBlueF(fluidColor), Color.getAlphaF(fluidColor));
        drawTiledTexture(InventoryMenu.BLOCK_ATLAS, x0, y0, width, height,
                sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(),
                sprite.contents().width(), sprite.contents().height(), z);
        graphics.setColor(1f, 1f, 1f, 1f);
    }

    public static void drawSprite(TextureAtlasSprite sprite, float x0, float y0, float w, float h) {
        drawSprite(Minecraft.getInstance().getModelManager().getAtlas(InventoryMenu.BLOCK_ATLAS), sprite, x0, y0, w, h);
    }

    public static void drawSprite(TextureAtlas textureMap, TextureAtlasSprite sprite, float x0, float y0, float w, float h) {
        RenderSystem.enableBlend();
        RenderSystem.bindTexture(textureMap.getId());
        drawTexture(x0, y0, x0 + w, y0 + h, sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1());
        RenderSystem.disableBlend();
    }

    public static void drawOutlineCenter(GuiGraphics graphics,
                                         int x, int y, int offset, int color) {
        drawOutlineCenter(graphics, x, y, offset, color, 1);
    }

    public static void drawOutlineCenter(GuiGraphics graphics,
                                         int x, int y, int offset, int color, int border) {
        drawOutline(graphics, x - offset, y - offset, x + offset, y + offset, color, border);
    }

    public static void drawOutline(GuiGraphics graphics, int left, int top, int right, int bottom, int color) {
        drawOutline(graphics, left, top, right, bottom, color, 1);
    }

    /**
     * Draw rectangle outline with given border
     */
    public static void drawOutline(GuiGraphics graphics,
                                   int left, int top, int right, int bottom, int color, int border) {
        graphics.fill(left, top, left + border, bottom, color);
        graphics.fill(right - border, top, right, bottom, color);
        graphics.fill(left + border, top, right - border, top + border, color);
        graphics.fill(left + border, bottom - border, right - border, bottom, color);
    }

    /**
     * Draws a rectangular shadow
     *
     * @param x      left of solid shadow part
     * @param y      top of solid shadow part
     * @param w      width of solid shadow part
     * @param h      height of solid shadow part
     * @param oX     shadow gradient size in x
     * @param oY     shadow gradient size in y
     * @param opaque solid shadow color
     * @param shadow gradient end color
     */
    public static void drawDropShadow(int x, int y, int w, int h, int oX, int oY, int opaque, int shadow) {

        float a1 = Color.getAlphaF(opaque);
        float r1 = Color.getRedF(opaque);
        float g1 = Color.getGreenF(opaque);
        float b1 = Color.getBlueF(opaque);
        float a2 = Color.getAlphaF(shadow);
        float r2 = Color.getRedF(shadow);
        float g2 = Color.getGreenF(shadow);
        float b2 = Color.getBlueF(shadow);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        float x1 = x + w, y1 = y + h;

        /* Draw opaque part */
        buffer.vertex(x1, y, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(x, y, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(x, y1, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(x1, y1, 0).color(r1, g1, b1, a1).endVertex();

        /* Draw top shadow */
        buffer.vertex(x1 + oX, y - oY, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(x - oX, y - oY, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(x, y, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(x1, y, 0).color(r1, g1, b1, a1).endVertex();

        /* Draw bottom shadow */
        buffer.vertex(x1, y1, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(x, y1, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(x - oX, y1 + oY, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(x1 + oX, y1 + oY, 0).color(r2, g2, b2, a2).endVertex();

        /* Draw left shadow */
        buffer.vertex(x, y, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(x - oX, y - oY, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(x - oX, y1 + oY, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(x, y1, 0).color(r1, g1, b1, a1).endVertex();

        /* Draw right shadow */
        buffer.vertex(x1 + oX, y - oY, 0).color(r2, g2, b2, a2).endVertex();
        buffer.vertex(x1, y, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(x1, y1, 0).color(r1, g1, b1, a1).endVertex();
        buffer.vertex(x1 + oX, y1 + oY, 0).color(r2, g2, b2, a2).endVertex();

        tesselator.end();

        RenderSystem.disableBlend();
    }

    public static void drawDropCircleShadow(int x, int y, int radius, int segments, int opaque, int shadow) {
        float a1 = Color.getAlphaF(opaque);
        float r1 = Color.getRedF(opaque);
        float g1 = Color.getGreenF(opaque);
        float b1 = Color.getBlueF(opaque);
        float a2 = Color.getAlphaF(shadow);
        float r2 = Color.getRedF(shadow);
        float g2 = Color.getGreenF(shadow);
        float b2 = Color.getBlueF(shadow);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);

        buffer.vertex(x, y, 0).color(r1, g1, b1, a1).endVertex();

        for (int i = 0; i <= segments; i++) {
            double a = i / (double) segments * Math.PI * 2 - Math.PI / 2;

            buffer.vertex(x - Math.cos(a) * radius, y + Math.sin(a) * radius, 0).color(r2, g2, b2, a2).endVertex();
        }

        tesselator.end();

        RenderSystem.disableBlend();
    }

    public static void drawDropCircleShadow(int x, int y, int radius, int offset, int segments, int opaque, int shadow) {
        if (offset >= radius) {
            drawDropCircleShadow(x, y, radius, segments, opaque, shadow);

            return;
        }

        float a1 = Color.getAlphaF(opaque);
        float r1 = Color.getRedF(opaque);
        float g1 = Color.getGreenF(opaque);
        float b1 = Color.getBlueF(opaque);
        float a2 = Color.getAlphaF(shadow);
        float r2 = Color.getRedF(shadow);
        float g2 = Color.getGreenF(shadow);
        float b2 = Color.getBlueF(shadow);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        /* Draw opaque base */
        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(x, y, 0).color(r1, g1, b1, a1).endVertex();

        for (int i = 0; i <= segments; i++) {
            double a = i / (double) segments * Math.PI * 2 - Math.PI / 2;

            buffer.vertex(x - Math.cos(a) * offset, y + Math.sin(a) * offset, 0).color(r1, g1, b1, a1).endVertex();
        }

        tesselator.end();

        /* Draw outer shadow */
        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i < segments; i++) {
            double alpha1 = i / (double) segments * Math.PI * 2 - Math.PI / 2;
            double alpha2 = (i + 1) / (double) segments * Math.PI * 2 - Math.PI / 2;

            buffer.vertex(x - Math.cos(alpha2) * offset, y + Math.sin(alpha2) * offset, 0).color(r1, g1, b1, a1).endVertex();
            buffer.vertex(x - Math.cos(alpha1) * offset, y + Math.sin(alpha1) * offset, 0).color(r1, g1, b1, a1).endVertex();
            buffer.vertex(x - Math.cos(alpha1) * radius, y + Math.sin(alpha1) * radius, 0).color(r2, g2, b2, a2).endVertex();
            buffer.vertex(x - Math.cos(alpha2) * radius, y + Math.sin(alpha2) * radius, 0).color(r2, g2, b2, a2).endVertex();
        }

        tesselator.end();

        RenderSystem.disableBlend();
    }

    @OnlyIn(Dist.CLIENT)
    public static void drawBorder(float x, float y, float width, float height, int color, float border) {
        drawRect(x - border, y - border, width + 2 * border, border, color);
        drawRect(x - border, y + height, width + 2 * border, border, color);
        drawRect(x - border, y, border, height, color);
        drawRect(x + width, y, border, height, color);
    }

    @OnlyIn(Dist.CLIENT)
    public static void drawText(GuiGraphics graphics, String text, float x, float y, float scale, int color, boolean shadow) {
        Font font = Minecraft.getInstance().font;
        RenderSystem.disableBlend();
        graphics.pose().pushPose();
        graphics.pose().scale(scale, scale, 0f);
        float sf = 1 / scale;
        graphics.drawString(font, text,x * sf, y * sf, color, shadow);
        graphics.pose().popPose();
        RenderSystem.enableBlend();
    }

    public static void drawTooltipBackground(GuiGraphics graphics, ItemStack stack, List<Component> lines, int x, int y, int textWidth, int height) {
        List<ClientTooltipComponent> clientComponents = ForgeHooksClient.gatherTooltipComponents(stack, lines,
                Optional.empty(), x, textWidth, height, TextRenderer.getFont());

        // TODO theme color
        int backgroundTop = 0xF0100010;
        int backgroundBottom = backgroundTop;
        int borderColorStart = 0x505000FF;
        int borderColorEnd = (borderColorStart & 0xFEFEFE) >> 1 | borderColorStart & 0xFF000000;
        RenderTooltipEvent.Color colorEvent = new RenderTooltipEvent.Color(stack, graphics, x, y,
                TextRenderer.getFont(),backgroundTop, borderColorStart, borderColorEnd, clientComponents);
        MinecraftForge.EVENT_BUS.post(colorEvent);
        backgroundTop = colorEvent.getBackgroundStart();
        backgroundBottom = colorEvent.getBackgroundStart();
        borderColorStart = colorEvent.getBorderStart();
        borderColorEnd = colorEvent.getBorderEnd();

        // top background border
        drawVerticalGradientRect(x - 3, y - 4, textWidth + 6, 1, backgroundTop, backgroundTop);
        // bottom background border
        drawVerticalGradientRect(x - 3, y + height + 3, textWidth + 6, 1, backgroundBottom, backgroundBottom);
        // center background
        drawVerticalGradientRect(x - 3, y - 3, textWidth + 6, height + 6, backgroundTop, backgroundBottom);
        // left background border
        drawVerticalGradientRect(x - 4, y - 3, 1, height + 6, backgroundTop, backgroundBottom);
        // right background border
        drawVerticalGradientRect(x + textWidth + 3, y - 3, 1, height + 6, backgroundTop, backgroundBottom);

        // left accent border
        drawVerticalGradientRect(x - 3, y - 2, 1, height + 4, borderColorStart, borderColorEnd);
        // right accent border
        drawVerticalGradientRect(x + textWidth + 2, y - 2, 1, height + 4, borderColorStart, borderColorEnd);
        // top accent border
        drawVerticalGradientRect(x - 3, y - 3, textWidth + 6, 1, borderColorStart, borderColorStart);
        // bottom accent border
        drawVerticalGradientRect(x - 3, y + height + 2, textWidth + 6, 1, borderColorEnd, borderColorEnd);
    }
}
