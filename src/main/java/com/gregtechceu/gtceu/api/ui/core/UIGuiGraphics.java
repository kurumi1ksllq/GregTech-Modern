package com.gregtechceu.gtceu.api.ui.core;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.ui.event.WindowEvent;
import com.gregtechceu.gtceu.api.ui.texture.NinePatchTexture;
import com.gregtechceu.gtceu.core.mixins.ui.accessor.GuiGraphicsAccessor;

import com.gregtechceu.gtceu.utils.GTMath;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.loading.FMLLoader;

import com.google.common.base.Preconditions;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.List;

@Accessors(fluent = true)
public class UIGuiGraphics extends GuiGraphics {

    public static final ResourceLocation PANEL_NINE_PATCH_TEXTURE = GTCEu.id("background_steel");
    public static final ResourceLocation DARK_PANEL_NINE_PATCH_TEXTURE = GTCEu.id("background_bronze");

    private UIGuiGraphics(Minecraft mc, MultiBufferSource.BufferSource bufferSource) {
        super(mc, bufferSource);
        utilityScreen();
    }

    @SuppressWarnings("DataFlowIssue")
    public static UIGuiGraphics of(GuiGraphics g) {
        var graphics = new UIGuiGraphics(Minecraft.getInstance(), g.bufferSource());
        ((GuiGraphicsAccessor) graphics)
                .gtceu$setScissorStack(((GuiGraphicsAccessor) g).gtceu$getScissorStack());
        ((GuiGraphicsAccessor) graphics).gtceu$setPose(((GuiGraphicsAccessor) g).gtceu$getPose());

        return graphics;
    }

    @SuppressWarnings("DataFlowIssue")
    public static UIGuiGraphics of(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource) {
        var graphics = new UIGuiGraphics(Minecraft.getInstance(), bufferSource);
        ((GuiGraphicsAccessor) graphics).gtceu$setPose(poseStack);
        return graphics;
    }

    public static UtilityScreen utilityScreen() {
        return UtilityScreen.get();
    }

    public void drawFluid(FluidStack stack, int capacity, float x, float y, float width, float height) {
        var sprite = getStillTexture(stack);
        if (sprite == null) {
            sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                    .apply(MissingTextureAtlasSprite.getLocation());
            if (!FMLLoader.isProduction()) {
                GTCEu.LOGGER.error("Missing fluid texture for fluid: {}", stack.getDisplayName().getString());
            }
        }
        Color fluidColor = Color.ofRgb(IClientFluidTypeExtensions.of(stack.getFluid()).getTintColor(stack));
        float scaledAmount = stack.getAmount() * height / capacity;
        if (stack.getAmount() > 0 && scaledAmount < 1) {
            scaledAmount = 1;
        }
        if (scaledAmount > height || scaledAmount == capacity) {
            scaledAmount = height;
        }

        RenderSystem.disableBlend();
        RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);

        final float xCount = width / 16;
        final float xRemainder = width - xCount * 16;
        final float yCount = scaledAmount / 16;
        final float yRemainder = scaledAmount - yCount * 16;

        final float yStart = y + height;
        for (int xTile = 0; xTile <= xCount; xTile++) {
            for (int yTile = 0; yTile <= yCount; yTile++) {
                float w = xTile == xCount ? xRemainder : 16;
                float h = yTile == yCount ? yRemainder : 16;
                float xCoord = x + xTile * 16;
                float yCoord = yStart - (yTile + 1) * 16;
                if (width > 0 && height > 0) {
                    float maskT = 16 - h;
                    float maskR = 16 - w;
                    drawFluidTexture(xCoord, yCoord, sprite, maskT, maskR, 0, fluidColor);
                }
            }
        }
        RenderSystem.enableBlend();
    }

    public void drawFluidTexture(float xCoord, float yCoord, TextureAtlasSprite sprite, float maskTop, float maskRight,
                                 int zLevel, Color fluidColor) {
        float uMin = sprite.getU0();
        float uMax = sprite.getU1();
        float vMin = sprite.getV0();
        float vMax = sprite.getV1();
        uMax = uMax - maskRight / 16f * (uMax - uMin);
        vMax = vMax - maskTop / 16f * (vMax - vMin);

        var builder = Tesselator.getInstance().getBuilder();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        var pose = this.pose().last().pose();
        builder.vertex(pose, xCoord, yCoord + 16, zLevel).uv(uMin, vMax).color(fluidColor.argb()).endVertex();
        builder.vertex(pose, xCoord + 16 - maskRight, yCoord + 16, zLevel).uv(uMax, vMax).color(fluidColor.argb())
                .endVertex();
        builder.vertex(pose, xCoord + 16 - maskRight, yCoord + maskTop, zLevel).uv(uMax, vMin).color(fluidColor.argb())
                .endVertex();
        builder.vertex(pose, xCoord, yCoord + maskTop, zLevel).uv(uMin, vMin).color(fluidColor.argb()).endVertex();

        BufferUploader.drawWithShader(builder.end());
    }

    @Nullable
    public TextureAtlasSprite getStillTexture(FluidStack stack) {
        ResourceLocation blocksTexture = InventoryMenu.BLOCK_ATLAS;
        ResourceLocation still = IClientFluidTypeExtensions.of(stack.getFluid()).getStillTexture(stack);
        return still == null ? null : Minecraft.getInstance().getTextureAtlas(blocksTexture).apply(still);
    }

    @Nullable
    public TextureAtlasSprite getFlowingTexture(FluidStack stack) {
        ResourceLocation blocksTexture = InventoryMenu.BLOCK_ATLAS;
        ResourceLocation still = IClientFluidTypeExtensions.of(stack.getFluid()).getFlowingTexture(stack);
        return still == null ? null : Minecraft.getInstance().getTextureAtlas(blocksTexture).apply(still);
    }

    /**
     * Draw the outline of a rectangle
     *
     * @param x      The x-coordinate of top-left corner of the rectangle
     * @param y      The y-coordinate of top-left corner of the rectangle
     * @param width  The width of the rectangle
     * @param height The height of the rectangle
     * @param color  The color of the rectangle
     */
    public void drawRectOutline(int x, int y, int width, int height, int color) {
        this.fill(x, y, x + width, y + 1, color);
        this.fill(x, y + height - 1, x + width, y + height, color);

        this.fill(x, y + 1, x + 1, y + height - 1, color);
        this.fill(x + width - 1, y + 1, x + width, y + height - 1, color);
    }

    /**
     * Draw a filled rectangle with a gradient
     *
     * @param x                The x-coordinate of top-left corner of the rectangle
     * @param y                The y-coordinate of top-left corner of the rectangle
     * @param width            The width of the rectangle
     * @param height           The height of the rectangle
     * @param topLeftColor     The color at the rectangle's top left corner
     * @param topRightColor    The color at the rectangle's top right corner
     * @param bottomRightColor The color at the rectangle's bottom right corner
     * @param bottomLeftColor  The color at the rectangle's bottom left corner
     */
    public void drawGradientRect(int x, int y, int width, int height, int topLeftColor, int topRightColor,
                                 int bottomRightColor, int bottomLeftColor) {
        var buffer = Tesselator.getInstance().getBuilder();
        var matrix = this.pose().last().pose();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x + width, y, 0).color(topRightColor).endVertex();
        buffer.vertex(matrix, x, y, 0).color(topLeftColor).endVertex();
        buffer.vertex(matrix, x, y + height, 0).color(bottomLeftColor).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).color(bottomRightColor).endVertex();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        Tesselator.getInstance().end();

        RenderSystem.disableBlend();
    }

    /**
     * Draw a panel that looks like the background of a vanilla
     * inventory screen
     *
     * @param x      The x-coordinate of top-left corner of the panel
     * @param y      The y-coordinate of top-left corner of the panel
     * @param width  The width of the panel
     * @param height The height of the panel
     * @param dark   Whether to use the dark version of the panel texture
     */
    public void drawPanel(int x, int y, int width, int height, boolean dark) {
        NinePatchTexture.draw(dark ? DARK_PANEL_NINE_PATCH_TEXTURE : PANEL_NINE_PATCH_TEXTURE, this, x, y, width,
                height);
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given coordinates with a
     * blit offset and texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x             the x-coordinate of the blit position.
     * @param y             the y-coordinate of the blit position.
     * @param blitOffset    the z-level offset for rendering order.
     * @param uOffset       the horizontal texture coordinate offset.
     * @param vOffset       the vertical texture coordinate offset.
     * @param uWidth        the width of the blitted portion in texture coordinates.
     * @param vHeight       the height of the blitted portion in texture coordinates.
     * @param textureWidth  the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public void blit(ResourceLocation atlasLocation, float x, float y, float blitOffset, float uOffset, float vOffset,
                     float uWidth, float vHeight, float textureWidth, float textureHeight) {
        this.blit(atlasLocation, x, x + uWidth, y, y + vHeight, blitOffset, uWidth, vHeight, uOffset, vOffset,
                textureWidth, textureHeight);
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given position and
     * dimensions with texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x             the x-coordinate of the top-left corner of the blit position.
     * @param y             the y-coordinate of the top-left corner of the blit position.
     * @param uOffset       the horizontal texture coordinate offset.
     * @param vOffset       the vertical texture coordinate offset.
     * @param width         the width of the blitted portion.
     * @param height        the height of the blitted portion.
     * @param textureWidth  the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public void blit(ResourceLocation atlasLocation, float x, float y, float uOffset, float vOffset, float width,
                     float height, float textureWidth, float textureHeight) {
        this.blit(atlasLocation, x, y, width, height, uOffset, vOffset, width, height, textureWidth, textureHeight);
    }

    /**
     * Blits a portion of the texture specified by the atlas location onto the screen at the given position and
     * dimensions with texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x             the x-coordinate of the top-left corner of the blit position.
     * @param y             the y-coordinate of the top-left corner of the blit position.
     * @param width         the width of the blitted portion.
     * @param height        the height of the blitted portion.
     * @param uOffset       the horizontal texture coordinate offset.
     * @param vOffset       the vertical texture coordinate offset.
     * @param uWidth        the width of the blitted portion in texture coordinates.
     * @param vHeight       the height of the blitted portion in texture coordinates.
     * @param textureWidth  the width of the texture.
     * @param textureHeight the height of the texture.
     */
    public void blit(ResourceLocation atlasLocation, float x, float y, float width, float height, float uOffset,
                     float vOffset, float uWidth, float vHeight, float textureWidth, float textureHeight) {
        this.blit(atlasLocation, x, x + width, y, y + height, 0, uWidth, vHeight, uOffset, vOffset, textureWidth,
                textureHeight);
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates and texture coordinates.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x1            the x-coordinate of the first corner of the blit position.
     * @param x2            the x-coordinate of the second corner of the blit position.
     * @param y1            the y-coordinate of the first corner of the blit position.
     * @param y2            the y-coordinate of the second corner of the blit position.
     * @param blitOffset    the z-level offset for rendering order.
     * @param uWidth        the width of the blitted portion in texture coordinates.
     * @param vHeight       the height of the blitted portion in texture coordinates.
     * @param uOffset       the horizontal texture coordinate offset.
     * @param vOffset       the vertical texture coordinate offset.
     * @param textureWidth  the width of the texture.
     * @param textureHeight the height of the texture.
     */
    void blit(ResourceLocation atlasLocation, float x1, float x2, float y1, float y2, float blitOffset, float uWidth,
              float vHeight, float uOffset, float vOffset, float textureWidth, float textureHeight) {
        this.innerBlit(atlasLocation, x1, x2, y1, y2, blitOffset, uOffset / textureWidth,
                (uOffset + uWidth) / textureWidth, vOffset / textureHeight,
                (vOffset + vHeight) / textureHeight);
    }

    /**
     * Performs the inner blit operation for rendering a texture with the specified coordinates and texture coordinates
     * without color tinting.
     *
     * @param atlasLocation the location of the texture atlas.
     * @param x1            the x-coordinate of the first corner of the blit position.
     * @param x2            the x-coordinate of the second corner of the blit position.
     * @param y1            the y-coordinate of the first corner of the blit position.
     * @param y2            the y-coordinate of the second corner of the blit position.
     * @param blitOffset    the z-level offset for rendering order.
     * @param minU          the minimum horizontal texture coordinate.
     * @param maxU          the maximum horizontal texture coordinate.
     * @param minV          the minimum vertical texture coordinate.
     * @param maxV          the maximum vertical texture coordinate.
     */
    void innerBlit(ResourceLocation atlasLocation, float x1, float x2, float y1, float y2, float blitOffset, float minU,
                   float maxU, float minV, float maxV) {
        RenderSystem.setShaderTexture(0, atlasLocation);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix4f = this.pose().last().pose();
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bufferbuilder.vertex(matrix4f, x1, y1, blitOffset).uv(minU, minV).endVertex();
        bufferbuilder.vertex(matrix4f, x1, y2, blitOffset).uv(minU, maxV).endVertex();
        bufferbuilder.vertex(matrix4f, x2, y2, blitOffset).uv(maxU, maxV).endVertex();
        bufferbuilder.vertex(matrix4f, x2, y1, blitOffset).uv(maxU, minV).endVertex();
        BufferUploader.drawWithShader(bufferbuilder.end());
    }

    public void drawSpectrum(int x, int y, int width, int height, boolean vertical) {
        var buffer = Tesselator.getInstance().getBuilder();
        var matrix = this.pose().last().pose();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, x, y, 0).color(1f, 1f, 1f, 1f).endVertex();
        buffer.vertex(matrix, x, y + height, 0).color(vertical ? 0f : 1f, 1f, 1f, 1f).endVertex();
        buffer.vertex(matrix, x + width, y + height, 0).color(0f, 1f, 1f, 1f).endVertex();
        buffer.vertex(matrix, x + width, y, 0).color(vertical ? 1f : 0f, 1f, 1f, 1f).endVertex();

        Tesselator.getInstance().end();
    }

    public void drawText(Component text, float x, float y, float scale, int color) {
        drawText(text, x, y, scale, color, TextAnchor.TOP_LEFT);
    }

    public void drawText(Component text, float x, float y, float scale, int color, boolean dropShadow) {
        drawText(text, x, y, scale, color, TextAnchor.TOP_LEFT, dropShadow);
    }

    public void drawText(Component text, float x, float y, float scale, int color, TextAnchor anchorPoint) {
        drawText(text, x, y, scale, color, anchorPoint, false);
    }

    public void drawText(Component text, float x, float y, float scale, int color, TextAnchor anchorPoint,
                         boolean dropShadow) {
        final var textRenderer = Minecraft.getInstance().font;

        this.pose().pushPose();
        this.pose().scale(scale, scale, 1);

        switch (anchorPoint) {
            case TOP_RIGHT -> x -= textRenderer.width(text) * scale;
            case BOTTOM_LEFT -> y -= textRenderer.lineHeight * scale;
            case BOTTOM_RIGHT -> {
                x -= textRenderer.width(text) * scale;
                y -= textRenderer.lineHeight * scale;
            }
        }

        this.drawString(textRenderer, text, (int) (x * (1 / scale)), (int) (y * (1 / scale)), color, dropShadow);
        this.pose().popPose();
    }

    public enum TextAnchor {
        TOP_RIGHT,
        BOTTOM_RIGHT,
        TOP_LEFT,
        BOTTOM_LEFT
    }

    public void drawLine(int x1, int y1, int x2, int y2, double thiccness, Color color) {
        var offset = new Vector2d(x2 - x1, y2 - y1).perpendicular().normalize().mul(thiccness * .5d);

        var buffer = Tesselator.getInstance().getBuilder();
        var matrix = this.pose().last().pose();
        int vColor = color.argb();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        buffer.vertex(matrix, (float) (x1 + offset.x), (float) (y1 + offset.y), 0).color(vColor).endVertex();
        buffer.vertex(matrix, (float) (x1 - offset.x), (float) (y1 - offset.y), 0).color(vColor).endVertex();
        buffer.vertex(matrix, (float) (x2 - offset.x), (float) (y2 - offset.y), 0).color(vColor).endVertex();
        buffer.vertex(matrix, (float) (x2 + offset.x), (float) (y2 + offset.y), 0).color(vColor).endVertex();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator.getInstance().end();
    }

    public void drawSolidRect(float x, float y, float width, float height, int color) {
        this.fill(x, y, x + width, y + height, color);
        RenderSystem.enableBlend();
    }

    public void drawCircle(int centerX, int centerY, int segments, double radius, Color color) {
        drawCircle(centerX, centerY, 0, 360, segments, radius, color);
    }

    public void drawCircle(int centerX, int centerY, double angleFrom, double angleTo, int segments, double radius,
                           Color color) {
        Preconditions.checkArgument(angleFrom < angleTo, "angleFrom must be less than angleTo");

        var buffer = Tesselator.getInstance().getBuilder();
        var matrix = this.pose().last().pose();

        double angleStep = Math.toRadians(angleTo - angleFrom) / segments;
        int vColor = color.argb();

        buffer.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        buffer.vertex(matrix, centerX, centerY, 0).color(vColor).endVertex();

        for (int i = segments; i >= 0; i--) {
            double theta = Math.toRadians(angleFrom) + i * angleStep;
            buffer.vertex(matrix, (float) (centerX - Math.cos(theta) * radius),
                            (float) (centerY - Math.sin(theta) * radius), 0)
                    .color(vColor).endVertex();
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator.getInstance().end();
    }

    public void drawRing(int centerX, int centerY, int segments, double innerRadius, double outerRadius,
                         Color innerColor, Color outerColor) {
        drawRing(centerX, centerY, 0d, 360d, segments, innerRadius, outerRadius, innerColor, outerColor);
    }

    public void drawRing(int centerX, int centerY, double angleFrom, double angleTo, int segments, double innerRadius,
                         double outerRadius, Color innerColor, Color outerColor) {
        Preconditions.checkArgument(angleFrom < angleTo, "angleFrom must be less than angleTo");
        Preconditions.checkArgument(innerRadius < outerRadius, "innerRadius must be less than outerRadius");

        var buffer = Tesselator.getInstance().getBuilder();
        var matrix = this.pose().last().pose();

        double angleStep = Math.toRadians(angleTo - angleFrom) / segments;
        int inColor = innerColor.argb();
        int outColor = outerColor.argb();

        buffer.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

        for (int i = 0; i <= segments; i++) {
            double theta = Math.toRadians(angleFrom) + i * angleStep;

            buffer.vertex(matrix, (float) (centerX - Math.cos(theta) * outerRadius),
                            (float) (centerY - Math.sin(theta) * outerRadius), 0)
                    .color(outColor).endVertex();
            buffer.vertex(matrix, (float) (centerX - Math.cos(theta) * innerRadius),
                            (float) (centerY - Math.sin(theta) * innerRadius), 0)
                    .color(inColor).endVertex();
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator.getInstance().end();
    }

    public void drawTooltip(Font textRenderer, int x, int y, List<ClientTooltipComponent> components) {
        ((GuiGraphicsAccessor) this).gtceu$renderTooltipFromComponents(textRenderer, components, x, y,
                DefaultTooltipPositioner.INSTANCE);
    }

    // --- debug rendering ---

    /**
     * Draw the area around the given rectangle which
     * the given insets describe
     *
     * @param x      The x-coordinate of top-left corner of the rectangle
     * @param y      The y-coordinate of top-left corner of the rectangle
     * @param width  The width of the rectangle
     * @param height The height of the rectangle
     * @param insets The insets to draw around the rectangle
     * @param color  The color to draw the inset area with
     */
    public void drawInsets(int x, int y, int width, int height, Insets insets, int color) {
        this.fill(x - insets.left(), y - insets.top(), x + width + insets.right(), y, color);
        this.fill(x - insets.left(), y + height, x + width + insets.right(), y + height + insets.bottom(), color);

        this.fill(x - insets.left(), y, x, y + height, color);
        this.fill(x + width, y, x + width + insets.right(), y + height, color);
    }

    /**
     * Fills a rectangle with the specified color using the given coordinates as the boundaries.
     *
     * @param minX  the minimum x-coordinate of the rectangle.
     * @param minY  the minimum y-coordinate of the rectangle.
     * @param maxX  the maximum x-coordinate of the rectangle.
     * @param maxY  the maximum y-coordinate of the rectangle.
     * @param color the color to fill the rectangle with.
     */
    public void fill(float minX, float minY, float maxX, float maxY, int color) {
        this.fill(minX, minY, maxX, maxY, 0, color);
    }

    /**
     * Fills a rectangle with the specified color and z-level using the given coordinates as the boundaries.
     *
     * @param minX  the minimum x-coordinate of the rectangle.
     * @param minY  the minimum y-coordinate of the rectangle.
     * @param maxX  the maximum x-coordinate of the rectangle.
     * @param maxY  the maximum y-coordinate of the rectangle.
     * @param z     the z-level of the rectangle.
     * @param color the color to fill the rectangle with.
     */
    public void fill(float minX, float minY, float maxX, float maxY, int z, int color) {
        this.fill(RenderType.gui(), minX, minY, maxX, maxY, z, color);
    }

    /**
     * Fills a rectangle with the specified color using the given render type and coordinates as the boundaries.
     *
     * @param renderType the render type to use.
     * @param minX       the minimum x-coordinate of the rectangle.
     * @param minY       the minimum y-coordinate of the rectangle.
     * @param maxX       the maximum x-coordinate of the rectangle.
     * @param maxY       the maximum y-coordinate of the rectangle.
     * @param color      the color to fill the rectangle with.
     */
    public void fill(RenderType renderType, float minX, float minY, float maxX, float maxY, int color) {
        this.fill(renderType, minX, minY, maxX, maxY, 0, color);
    }

    /**
     * Fills a rectangle with the specified color and z-level using the given render type and coordinates as the
     * boundaries.
     *
     * @param renderType the render type to use.
     * @param minX       the minimum x-coordinate of the rectangle.
     * @param minY       the minimum y-coordinate of the rectangle.
     * @param maxX       the maximum x-coordinate of the rectangle.
     * @param maxY       the maximum y-coordinate of the rectangle.
     * @param z          the z-level of the rectangle.
     * @param color      the color to fill the rectangle with.
     */
    public void fill(RenderType renderType, float minX, float minY, float maxX, float maxY, int z, int color) {
        Matrix4f matrix4f = this.pose().last().pose();
        if (minX < maxX) {
            float i = minX;
            minX = maxX;
            maxX = i;
        }

        if (minY < maxY) {
            float j = minY;
            minY = maxY;
            maxY = j;
        }

        float f3 = (float) FastColor.ARGB32.alpha(color) / 255.0F;
        float f = (float) FastColor.ARGB32.red(color) / 255.0F;
        float f1 = (float) FastColor.ARGB32.green(color) / 255.0F;
        float f2 = (float) FastColor.ARGB32.blue(color) / 255.0F;
        VertexConsumer vertexconsumer = this.bufferSource().getBuffer(renderType);
        vertexconsumer.vertex(matrix4f, minX, minY, z).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, minX, maxY, z).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, maxX, maxY, z).color(f, f1, f2, f3).endVertex();
        vertexconsumer.vertex(matrix4f, maxX, minY, z).color(f, f1, f2, f3).endVertex();
        ((GuiGraphicsAccessor) this).callFlushIfUnmanaged();
    }

    /**
     * Draw the element inspector for the given tree, detailing the position,
     * bounding box, margins and padding of each component
     *
     * @param root        The root component of the hierarchy to draw
     * @param mouseX      The x-coordinate of the mouse pointer
     * @param mouseY      The y-coordinate of the mouse pointer
     * @param onlyHovered Whether to only draw the inspector for the hovered widget
     */
    public void drawInspector(ParentUIComponent root, double mouseX, double mouseY, boolean onlyHovered) {
        RenderSystem.disableDepthTest();
        var client = Minecraft.getInstance();
        var font = client.font;

        var children = new ArrayList<UIComponent>();
        if (!onlyHovered) {
            root.collectDescendants(children);
        } else if (root.childAt((int) mouseX, (int) mouseY) != null) {
            children.add(root.childAt((int) mouseX, (int) mouseY));
        }

        for (var child : children) {
            if (child instanceof ParentUIComponent parentComponent) {
                this.drawInsets(parentComponent.x(), parentComponent.y(), parentComponent.width(),
                        parentComponent.height(), parentComponent.padding().get().inverted(), 0xA70CECDD);
            }

            final var margins = child.margins().get();
            this.drawInsets(child.x(), child.y(), child.width(), child.height(), margins, 0xA7FFF338);
            drawRectOutline(child.x(), child.y(), child.width(), child.height(), 0xFF3AB0FF);

            if (onlyHovered) {

                int inspectorX = child.x() + 1;
                int inspectorY = child.y() + child.height() + child.margins().get().bottom() + 1;
                int inspectorHeight = font.lineHeight * 5 + 4;

                if (inspectorY > client.getWindow().getGuiScaledHeight() - inspectorHeight) {
                    inspectorY -= child.fullSize().height() + inspectorHeight + 1;
                    if (inspectorY < 0) inspectorY = 1;
                    if (child instanceof ParentUIComponent parentComponent) {
                        inspectorX += parentComponent.padding().get().left();
                        inspectorY += parentComponent.padding().get().top();
                    }
                }

                final var nameText = Component.literal(
                        child.getClass().getSimpleName() + (child.id() != null ? " '" + child.id() + "'" : ""));
                final var descriptor = Component.literal(child.x() + "," + child.y() + " (" + child.width() + "," +
                        child.height() + ")" + " <" + margins.top() + "," + margins.bottom() + "," + margins.left() +
                        "," + margins.right() + "> ");
                if (child instanceof ParentUIComponent parentComponent) {
                    var padding = parentComponent.padding().get();
                    descriptor.append(" >" + padding.top() + "," + padding.bottom() + "," + padding.left() + "," +
                            padding.right() + "<");
                }
                final var pos = Component.literal(child.positioning().get().toString());
                final var sizeH = Component.literal("size_h=" + child.horizontalSizing().get().toString());
                final var sizeV = Component.literal("size_v=" + child.verticalSizing().get().toString());

                int width = GTMath.max(font.width(nameText), font.width(descriptor),
                        font.width(pos), font.width(sizeH), font.width(sizeV));
                fill(inspectorX, inspectorY, inspectorX + width + 3, inspectorY + inspectorHeight, 0xA7000000);
                drawRectOutline(inspectorX, inspectorY, width + 3, inspectorHeight, 0xA7000000);

                this.drawString(font, nameText, inspectorX + 2, inspectorY + 2, 0xFFFFFF, false);
                this.drawString(font, descriptor, inspectorX + 2, inspectorY + font.lineHeight + 2, 0xFFFFFF, false);
                this.drawString(font, pos, inspectorX + 2, inspectorY + font.lineHeight * 2 + 2,
                        0xFFFFFF, false);
                this.drawString(font, sizeH, inspectorX + 2, inspectorY + font.lineHeight * 3 + 2,
                        0xFFFFFF, false);
                this.drawString(font, sizeV, inspectorX + 2, inspectorY + font.lineHeight * 4 + 2,
                        0xFFFFFF, false);
            }
        }

        RenderSystem.enableDepthTest();
    }

    public static class UtilityScreen extends Screen {

        private static UtilityScreen INSTANCE;

        private UtilityScreen() {
            super(Component.empty());
        }

        public static UtilityScreen get() {
            if (INSTANCE == null) {
                INSTANCE = new UtilityScreen();

                final var client = Minecraft.getInstance();
                INSTANCE.init(client,
                        client.getWindow().getGuiScaledWidth(),
                        client.getWindow().getGuiScaledHeight());
            }

            return INSTANCE;
        }

        public static void onWindowResized(WindowEvent.Resized event) {
            if (INSTANCE == null) return;
            Window window = event.getWindow();
            INSTANCE.init(event.getMinecraft(), window.getGuiScaledWidth(), window.getGuiScaledHeight());
        }

    }

}
