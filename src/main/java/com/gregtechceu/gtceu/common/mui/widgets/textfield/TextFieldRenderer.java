package com.gregtechceu.gtceu.common.mui.widgets.textfield;

import com.gregtechceu.gtceu.api.mui.drawable.text.TextRenderer;
import com.gregtechceu.gtceu.api.mui.utils.Color;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Point;
import com.gregtechceu.gtceu.api.mui.widget.sizer.PointF;

import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.commons.lang3.mutable.MutableObject;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

public class TextFieldRenderer extends TextRenderer {

    protected final TextFieldHandler handler;
    protected int markedColor = 0x2F72A8;
    protected int cursorColor = 0xFFFFFFFF;
    protected boolean renderCursor = false;

    public TextFieldRenderer(TextFieldHandler handler) {
        this.handler = handler;
    }

    public void toggleCursor() {
        this.renderCursor = !this.renderCursor;
    }

    public void setCursor(boolean active) {
        this.renderCursor = active;
    }

    public void setMarkedColor(int markedColor) {
        this.markedColor = markedColor;
    }

    public void setCursorColor(int cursorColor) {
        this.cursorColor = cursorColor;
    }

    @Override
    protected void drawMeasuredLines(PoseStack poseStack, MultiBufferSource.BufferSource buffers,
                                     List<Line> measuredLines) {
        drawMarked(poseStack.last().pose(), measuredLines);
        super.drawMeasuredLines(poseStack, buffers, measuredLines);
        // draw cursor
        if (this.renderCursor) {
            Point main = this.handler.getMainCursor();
            PointF start = getPosOf(measuredLines, main);
            if (this.handler.getText().get(main.y).isEmpty()) {
                start.x += 0.7f;
            }
            drawCursor(poseStack, start.x, start.y);
        }
    }

    @Override
    public List<FormattedCharSequence> wrapLine(Component line) {
        return Collections.singletonList(line.getVisualOrderText());
    }

    protected void drawMarked(Matrix4f pose, List<Line> measuredLines) {
        if (!this.simulate && this.handler.hasTextMarked()) {
            PointF start = getPosOf(measuredLines, this.handler.getStartCursor());
            // render Marked
            PointF end = getPosOf(measuredLines, this.handler.getEndCursor());

            if (start.y == end.y) {
                drawMarked(pose, start.y, start.x, end.x);
            } else {
                int min = this.handler.getStartCursor().y;
                int max = this.handler.getEndCursor().y;
                Line line = measuredLines.get(min);
                int startX = getStartX(line.width());
                drawMarked(pose, start.y, start.x, startX + line.width());
                start.y += getFontHeight();
                if (max - min > 1) {
                    for (int i = min + 1; i < max; i++) {
                        line = measuredLines.get(i);
                        startX = getStartX(line.width());
                        drawMarked(pose, start.y, startX, startX + line.width());
                        start.y += getFontHeight();
                    }
                }
                line = measuredLines.get(max);
                startX = getStartX(line.width());
                drawMarked(pose, start.y, startX, end.x);
            }
        }
    }

    public Point getCursorPos(List<String> lines, int x, int y) {
        if (lines.isEmpty()) {
            return new Point();
        }
        List<Line> measuredLines = measureStringLines(lines);
        y -= getStartY(measuredLines.size());
        int index = (int) (y / (getFontHeight()));
        if (index < 0) return new Point();
        if (index >= measuredLines.size())
            return new Point(getFont().width(measuredLines.get(measuredLines.size() - 1).text()),
                    measuredLines.size() - 1);
        Line line = measuredLines.get(index);
        x -= getStartX(line.width());
        if (line.width() <= 0) return new Point(0, index);
        if (line.width() < x) return new Point(getFont().width(line.text()), index);
        float currentX = 0;
        for (int i = 0; i < getFont().width(line.text()); i++) {
            final int finalI = i;
            MutableInt total = new MutableInt();
            MutableInt last = new MutableInt();
            MutableInt c = new MutableInt();
            MutableObject<Style> s = new MutableObject<>();
            line.text().accept((pos, style, codePoint) -> {
                if (total.addAndGet(pos - last.getValue()) >= finalI) {
                    c.setValue(codePoint);
                    s.setValue(style);
                    return false;
                }
                last.setValue(pos);
                return true;
            });
            float charWidth = getFont().getSplitter()
                    .stringWidth(FormattedCharSequence.codepoint(c.getValue(), s.getValue())) * this.scale;
            currentX += charWidth;
            if (currentX >= x) {
                // dist with current letter < dist without current letter -> next letter pos
                if (Math.abs(currentX - x) < Math.abs(currentX - charWidth - x)) i++;
                return new Point(i, index);
            }
        }
        return new Point();
    }

    public PointF getPosOf(List<Line> measuredLines, Point cursorPos) {
        if (measuredLines.isEmpty()) {
            return new PointF(getStartX(0), getStartYOfLines(1));
        }
        Line line = measuredLines.get(cursorPos.y);
        float width = Math.min(getFont().getSplitter().stringWidth(line.text()), cursorPos.x);
        return new PointF(getStartX(line.width()) + width * this.scale,
                getStartYOfLines(measuredLines.size()) + cursorPos.y * getFontHeight());
    }

    @OnlyIn(Dist.CLIENT)
    public void drawMarked(Matrix4f pose, float y0, float x0, float x1) {
        y0 -= 1;
        float y1 = y0 + getFontHeight();
        float red = Color.getRedF(this.markedColor);
        float green = Color.getGreenF(this.markedColor);
        float blue = Color.getBlueF(this.markedColor);
        float alpha = Color.getAlphaF(this.markedColor);
        if (alpha == 0)
            alpha = 1f;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferbuilder.vertex(pose, x0, y1, 0.0f).endVertex();
        bufferbuilder.vertex(pose, x1, y1, 0.0f).endVertex();
        bufferbuilder.vertex(pose, x1, y0, 0.0f).endVertex();
        bufferbuilder.vertex(pose, x0, y0, 0.0f).endVertex();
        tesselator.end();
        RenderSystem.disableColorLogicOp();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawCursor(PoseStack poseStack, float x0, float y0) {
        x0 = (x0 - 0.8f) / this.scale;
        y0 = (y0 - 1) / this.scale;
        float x1 = x0 + 0.6f;
        float y1 = y0 + 9;
        float red = Color.getRedF(this.cursorColor);
        float green = Color.getGreenF(this.cursorColor);
        float blue = Color.getBlueF(this.cursorColor);
        float alpha = Color.getAlphaF(this.cursorColor);
        if (alpha == 0)
            alpha = 1f;
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();

        RenderSystem.disableBlend();
        poseStack.pushPose();
        poseStack.scale(this.scale, this.scale, 0);
        RenderSystem.setShaderColor(red, green, blue, alpha);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION);
        bufferbuilder.vertex(x0, y1, 0.0D).endVertex();
        bufferbuilder.vertex(x1, y1, 0.0D).endVertex();
        bufferbuilder.vertex(x1, y0, 0.0D).endVertex();
        bufferbuilder.vertex(x0, y0, 0.0D).endVertex();
        tesselator.end();
        RenderSystem.setShaderColor(1, 1, 1, 1);

        poseStack.popPose();
        RenderSystem.enableBlend();
    }
}
