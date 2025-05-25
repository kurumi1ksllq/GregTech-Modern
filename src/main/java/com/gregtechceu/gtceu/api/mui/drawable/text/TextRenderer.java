package com.gregtechceu.gtceu.api.mui.drawable.text;

import com.gregtechceu.gtceu.api.mui.base.drawable.ITextLine;
import com.gregtechceu.gtceu.api.mui.drawable.Stencil;
import com.gregtechceu.gtceu.api.mui.utils.Alignment;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Area;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;
import com.gregtechceu.gtceu.core.mixins.StringSplitterAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextRenderer {

    public static final TextRenderer SHARED = new TextRenderer();

    protected float maxWidth = -1, maxHeight = -1;
    @Getter
    protected int x = 0, y = 0;
    @Getter
    protected Alignment alignment = Alignment.TopLeft;
    @Getter
    @Setter
    protected float scale = 1f;
    @Getter
    @Setter
    protected boolean shadow = false;
    @Getter
    @Setter
    protected int color = 0;// Theme.INSTANCE.getText();
    @Setter
    protected boolean simulate;
    @Getter
    protected float lastWidth = 0, lastHeight = 0;
    protected float lastX = 0, lastY = 0;
    protected boolean scrollOnOverflow = false;

    public void setAlignment(Alignment alignment, float maxWidth) {
        setAlignment(alignment, maxWidth, -1);
    }

    public void setAlignment(Alignment alignment, float maxWidth, float maxHeight) {
        this.alignment = alignment;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public void setPos(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void draw(GuiGraphics graphics, String text) {
        this.draw(graphics.pose(), graphics.bufferSource(), Component.literal(text));
    }

    public void draw(PoseStack poseStack, MultiBufferSource.BufferSource buffers, String text) {
        this.draw(poseStack, buffers, Component.literal(text));
    }

    public void draw(GuiGraphics graphics, Component text) {
        if (simulate) {
            this.draw(null, null, text);
        }
        this.draw(graphics.pose(), graphics.bufferSource(), text);
    }

    public void draw(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Component text) {
        if (this.maxWidth <= 0 && !text.getString().contains("\n'")) {
            drawSimple(poseStack, buffers, text);
        } else {
            draw(poseStack, buffers, Collections.singletonList(text));
        }
    }

    public void draw(PoseStack poseStack, MultiBufferSource.BufferSource buffers, List<Component> lines) {
        drawMeasuredLines(poseStack, buffers, measureLines(lines));
    }

    protected void drawMeasuredLines(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                     List<Line> measuredLines) {
        float maxW = 0;
        int y0 = getStartYOfLines(measuredLines.size());
        for (Line measuredLine : measuredLines) {
            int x0 = getStartX(measuredLine.width);
            maxW = Math.max(maxW, measuredLine.width);
            draw(poseStack, buffers, measuredLine.text, x0, y0);
            y0 += (int) getFontHeight();
        }
        this.lastWidth = this.maxWidth > 0 ? Math.min(maxW, this.maxWidth) : maxW;
        this.lastHeight = measuredLines.size() * getFontHeight();
        this.lastWidth = Math.max(0, this.lastWidth - this.scale);
        this.lastHeight = Math.max(0, this.lastHeight - this.scale);
    }

    public void drawSimple(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Component text) {
        this.drawSimple(poseStack, buffers, text.getVisualOrderText());
    }

    public void drawSimple(PoseStack poseStack, MultiBufferSource.BufferSource buffers, FormattedCharSequence text) {
        float w = getFont().width(text) * this.scale;
        int y = getStartYOfLines(1), x = getStartX(w);
        draw(poseStack, buffers, text, x, y);
        this.lastWidth = w;
        this.lastHeight = getFontHeight();
        this.lastWidth = Math.max(0, this.lastWidth - this.scale);
        this.lastHeight = Math.max(0, this.lastHeight - this.scale);
    }

    public List<Line> measureStringLines(List<String> lines) {
        return measureLines(FontRenderHelper.asComponents(lines));
    }

    public List<Line> measureLines(List<Component> lines) {
        List<Line> measuredLines = new ArrayList<>();
        for (Component line : lines) {
            for (FormattedCharSequence subLine : wrapLine(line)) {
                measuredLines.add(line(subLine));
            }
        }
        return measuredLines;
    }

    public List<ITextLine> compile(List<Object> rawText) {
        return RichTextCompiler.INSTANCE.compileLines(getFont(), rawText, (int) this.maxWidth, this.scale);
    }

    public List<ITextLine> compileAndDraw(GuiContext context, List<Object> raw) {
        return compileAndDraw(context.getGraphics().pose(), context.getGraphics().bufferSource(), raw);
    }

    public List<ITextLine> compileAndDraw(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                          List<Object> raw) {
        List<ITextLine> lines = compile(raw);
        drawCompiled(poseStack, buffers, lines);
        return lines;
    }

    public void drawCompiled(PoseStack poseStack, MultiBufferSource.BufferSource buffers, List<ITextLine> lines) {
        int height = 0, width = 0;
        for (ITextLine line : lines) {
            height += line.getHeight(getFont());
            width = Math.max(width, line.getWidth());
        }
        if (!this.simulate) {
            poseStack.pushPose();
            poseStack.translate(this.x, this.y, 10);
            poseStack.scale(this.scale, this.scale, 1f);
            poseStack.translate(-this.x, -this.y, 0);
        }
        int y0 = getStartY(height, height);
        this.lastY = y0;
        for (ITextLine line : lines) {
            int x0 = getStartX(width, line.getWidth());
            if (!simulate) line.draw(poseStack, buffers, getFont(), x0, y0, this.color, this.shadow);
            y0 += line.getHeight(getFont());
        }
        if (!this.simulate) {
            poseStack.popPose();
        }
        this.lastWidth = this.maxWidth > 0 ? Math.min(width * this.scale, this.maxWidth) : width * this.scale;
        this.lastHeight = height * this.scale;
        this.lastWidth = Math.max(0, this.lastWidth - this.scale);
        this.lastHeight = Math.max(0, this.lastHeight - this.scale);
    }

    public void drawCut(GuiGraphics graphics, String text) {
        drawCut(graphics.pose(), graphics.bufferSource(), text);
    }

    public void drawCut(PoseStack poseStack, MultiBufferSource.BufferSource buffers, String text) {
        if (text.contains("\n")) {
            throw new IllegalArgumentException("Scrolling text can't wrap!");
        }
        drawCut(poseStack, buffers, line(Component.literal(text).getVisualOrderText()));
    }

    public void drawCut(GuiGraphics graphics, Line line) {
        drawCut(graphics.pose(), graphics.bufferSource(), line);
    }

    public void drawCut(PoseStack poseStack, MultiBufferSource.BufferSource buffers, Line line) {
        if (line.width > this.maxWidth) {
            var cutText = FormattedCharSequence.composite(
                    FontRenderHelper.splitAtMax(line.text(), this.maxWidth - 6),
                    FormattedCharSequence.forward("...", Style.EMPTY));
            drawMeasuredLines(poseStack, buffers, Collections.singletonList(line(cutText)));
        } else {
            drawMeasuredLines(poseStack, buffers, Collections.singletonList(line));
        }
    }

    public void drawScrolling(GuiContext context, Line line, int scroll, Area area) {
        drawScrolling(context.getGraphics().pose(), context.getGraphics().bufferSource(), line, scroll, area, context);
    }

    public void drawScrolling(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                              Line line, int scroll, Area area, GuiContext context) {
        if (line.width() <= this.maxWidth) {
            drawMeasuredLines(poseStack, buffers, Collections.singletonList(line));
            return;
        }
        scroll = scroll % (int) (line.width + 1);
        float max = this.maxWidth + scroll;
        FormattedCharSequence drawString = FontRenderHelper.splitAtMax(line.text(), max);
        Area.SHARED.set(this.x, Integer.MIN_VALUE, this.x + (int) this.maxWidth, Integer.MAX_VALUE);
        Stencil.apply(Area.SHARED, context);
        poseStack.pushPose();
        poseStack.translate(-scroll, 0, 0);
        drawMeasuredLines(poseStack, buffers, Collections.singletonList(line(drawString)));
        poseStack.popPose();
        Stencil.remove();
    }

    public List<FormattedCharSequence> wrapLine(Component line) {
        return this.maxWidth > 0 ? getFont().split(line, (int) (this.maxWidth / this.scale)) :
                Collections.singletonList(line.getVisualOrderText());
    }

    public boolean wouldFit(List<String> text) {
        if (this.maxHeight > 0 && this.maxHeight < text.size() * getFontHeight() - this.scale) {
            return false;
        }
        if (this.maxWidth > 0) {
            for (String line : text) {
                if (this.maxWidth < getFont().width(line)) {
                    return false;
                }
            }
        }
        return true;
    }

    public int getMaxWidth(List<Component> lines) {
        if (lines.isEmpty()) {
            return 0;
        }
        List<Line> measuredLines = measureLines(lines);
        float w = 0;
        for (Line measuredLine : measuredLines) {
            w = Math.max(w, measuredLine.width());
        }
        return (int) Math.ceil(w);
    }

    protected int getStartYOfLines(int lines) {
        return getStartY(lines * getFontHeight() - this.scale);
    }

    protected int getStartY(float height) {
        return getStartY(this.maxHeight, height);
    }

    protected int getStartY(float maxHeight, float height) {
        if (this.alignment.y > 0 && maxHeight > 0 && height != maxHeight) {
            return (int) (this.y + (maxHeight * this.alignment.y) - height * this.alignment.y);
        }
        return this.y;
    }

    protected int getStartX(float lineWidth) {
        return getStartX(this.maxWidth, lineWidth);
    }

    protected int getStartX(float maxWidth, float lineWidth) {
        if (this.alignment.x > 0 && maxWidth > 0) {
            return (int) (this.x + (maxWidth * this.alignment.x) - lineWidth * this.alignment.x);
        }
        return this.x;
    }

    protected void draw(PoseStack poseStack, MultiBufferSource buffers, FormattedCharSequence text, float x, float y) {
        if (this.simulate) return;
        RenderSystem.disableBlend();
        poseStack.pushPose();
        poseStack.scale(this.scale, this.scale, 0f);
        getFont().drawInBatch(text, x / this.scale, y / this.scale, this.color, this.shadow,
                poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, 0xf000f0);
        poseStack.popPose();
        RenderSystem.enableBlend();
    }

    public float getFontHeight() {
        return getFont().lineHeight * this.scale;
    }

    @OnlyIn(Dist.CLIENT)
    public static Font getFont() {
        return Minecraft.getInstance().font;
    }

    @OnlyIn(Dist.CLIENT)
    public static StringSplitter.WidthProvider getWidthProvider() {
        return ((StringSplitterAccessor) getFont().getSplitter()).getWidthProvider();
    }

    public Line line(FormattedCharSequence text) {
        return new Line(text, getFont().width(text) * this.scale);
    }

    public record Line(@Getter FormattedCharSequence text, @Getter float width) {

        public int upperWidth() {
            return (int) (this.width + 1);
        }

        public int lowerWidth() {
            return (int) (this.width + 1);
        }
    }

    public record FormattedChar(int codePoint, Style style) {

        public FormattedCharSequence asSequence() {
            return FormattedCharSequence.codepoint(codePoint, style);
        }
    }
}
