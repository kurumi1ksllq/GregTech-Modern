package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.base.BaseUIComponent;
import com.gregtechceu.gtceu.api.ui.core.*;
import com.gregtechceu.gtceu.api.ui.parsing.UIModel;
import com.gregtechceu.gtceu.api.ui.parsing.UIParsing;
import com.gregtechceu.gtceu.api.ui.texture.TextTexture;
import com.gregtechceu.gtceu.api.ui.util.Observable;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Accessors(fluent = true, chain = true)
public class LabelComponent extends BaseUIComponent {

    protected final Font font = Minecraft.getInstance().font;

    protected Supplier<Component> textSupplier;
    @Getter
    protected Component text;
    protected List<FormattedCharSequence> wrappedText;

    @Getter
    @Setter
    protected VerticalAlignment verticalTextAlignment = VerticalAlignment.TOP;
    @Getter
    @Setter
    protected HorizontalAlignment horizontalTextAlignment = HorizontalAlignment.LEFT;

    @Getter
    protected final AnimatableProperty<Color> color = AnimatableProperty.of(Color.WHITE);
    protected final Observable<Integer> lineHeight = Observable.of(this.font.lineHeight + 1);
    @Getter
    @Setter
    protected boolean shadow;
    @Getter
    protected int maxWidth;

    @Setter
    public float rollSpeed = 1;
    @Setter
    public TextTexture.TextType textType = TextTexture.TextType.NORMAL;

    @Getter
    protected Function<Style, Boolean> textClickHandler = UIGuiGraphics.utilityScreen()::handleComponentClicked;

    protected LabelComponent(Component text) {
        this.text = text;
        this.wrappedText = new ArrayList<>();

        this.shadow = false;
        this.maxWidth = Integer.MAX_VALUE;

        this.lineHeight.observe($ -> this.notifyParentIfMounted());
    }

    protected LabelComponent(Supplier<Component> textSupplier) {
        this.textSupplier = textSupplier;
        this.wrappedText = new ArrayList<>();

        this.shadow = false;
        this.maxWidth = Integer.MAX_VALUE;

        this.lineHeight.observe($ -> this.notifyParentIfMounted());
    }

    public LabelComponent text(Component text) {
        this.text = text;
        this.notifyParentIfMounted();
        return this;
    }

    public LabelComponent textSupplier(Supplier<Component> textSupplier) {
        this.textSupplier = textSupplier;
        this.notifyParentIfMounted();
        return this;
    }

    public LabelComponent maxWidth(int maxWidth) {
        this.maxWidth = maxWidth;
        this.notifyParentIfMounted();
        return this;
    }

    public LabelComponent color(Color color) {
        this.color.set(color);
        return this;
    }

    public LabelComponent lineHeight(int lineHeight) {
        this.lineHeight.set(lineHeight);
        return this;
    }

    public int lineHeight() {
        return this.lineHeight.get();
    }

    public LabelComponent textClickHandler(Function<Style, Boolean> textClickHandler) {
        this.textClickHandler = textClickHandler;
        return this;
    }

    @Override
    public int determineHorizontalContentSize(Sizing sizing) {
        int widestText = 0;
        for (var line : this.wrappedText) {
            int width = this.font.width(line);
            if (width > widestText) widestText = width;
        }

        if (widestText > this.maxWidth && this.wrapLines()) {
            return this.determineHorizontalContentSize(sizing);
        } else {
            return widestText;
        }
    }

    @Override
    public int determineVerticalContentSize(Sizing sizing) {
        this.wrapLines();
        return (this.wrappedText.size() * (this.lineHeight() + 2)) - 2;
    }

    @Override
    public LabelComponent inflate(Size space) {
        this.wrapLines();
        super.inflate(space);
        return this;
    }

    private boolean wrapLines() {
        if (textType == TextTexture.TextType.NORMAL) {
            if (this.textSupplier != null) {
                this.wrappedText = this.font.split(this.textSupplier.get(),
                        this.horizontalSizing.get().isContent() ? this.maxWidth : this.width);
            } else {
                this.wrappedText = this.font.split(this.text,
                        this.horizontalSizing.get().isContent() ? this.maxWidth : this.width);
            }
            return true;
        } else {
            if (textSupplier != null) {
                this.wrappedText = Collections
                        .singletonList(Language.getInstance().getVisualOrder(this.textSupplier.get()));
            } else {
                this.wrappedText = Collections.singletonList(Language.getInstance().getVisualOrder(this.text));
            }
        }
        return false;
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        this.color.update(delta);
        if (textType == TextTexture.TextType.NORMAL) {
            this.wrapLines();
        }
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        var pose = graphics.pose();

        pose.pushPose();
        pose.translate(0, 0, 400);
        // pose.translate(0, 1 / Minecraft.getInstance().getWindow().getGuiScale(), 0);

        int x = this.x;
        int y = this.y;

        if (this.horizontalSizing.get().isContent()) {
            x += this.horizontalSizing.get().value;
        }
        if (this.verticalSizing.get().isContent()) {
            y += this.verticalSizing.get().value;
        }

        RenderSystem.enableDepthTest();

        switch (this.verticalTextAlignment) {
            case CENTER -> y += (this.height - ((this.wrappedText.size() * (this.lineHeight() + 2)) - 2)) / 2;
            case BOTTOM -> y += this.height - ((this.wrappedText.size() * (this.lineHeight() + 2)) - 2);
        }

        final int lambdaX = x;
        final int lambdaY = y;

        graphics.drawManaged(() -> {
            int renderX = lambdaX;

            int lineHeight = this.lineHeight();

            if (textType == TextTexture.TextType.NORMAL) {
                lineHeight *= this.wrappedText.size();
                for (int i = 0; i < this.wrappedText.size(); i++) {
                    int renderY = lambdaY + i * (lineHeight + 2);
                    renderY += lineHeight - lineHeight;

                    var line = this.wrappedText.get(i);
                    switch (this.horizontalTextAlignment) {
                        case CENTER -> renderX += (this.width - this.font.width(line)) / 2;
                        case RIGHT -> renderX += this.width - this.font.width(line);
                    }

                    int lineWidth = font.width(line);
                    int _x = renderX + (width - lineWidth) / 2;
                    graphics.drawString(font, line, _x, renderY, color.get().argb(), shadow);
                }
            } else if (textType == TextTexture.TextType.LEFT) {
                lineHeight *= this.wrappedText.size();
                for (int i = 0; i < this.wrappedText.size(); i++) {
                    int renderY = lambdaY + i * (lineHeight + 2);
                    renderY += lineHeight - lineHeight;

                    var line = this.wrappedText.get(i);
                    switch (this.horizontalTextAlignment) {
                        case CENTER -> renderX += (this.width - this.font.width(line)) / 2;
                        case RIGHT -> renderX += this.width - this.font.width(line);
                    }

                    graphics.drawString(font, line, renderX, renderY, color.get().argb(), shadow);
                }
            } else if (textType == TextTexture.TextType.RIGHT) {
                lineHeight *= this.wrappedText.size();
                for (int i = 0; i < this.wrappedText.size(); i++) {
                    int renderY = lambdaY + i * (lineHeight + 2);
                    renderY += lineHeight - lineHeight;

                    var line = this.wrappedText.get(i);
                    switch (this.horizontalTextAlignment) {
                        case CENTER -> renderX += (this.width - this.font.width(line)) / 2;
                        case RIGHT -> renderX += this.width - this.font.width(line);
                    }

                    int lineWidth = font.width(line);
                    graphics.drawString(font, line, (renderX + width - lineWidth), renderY, color.get().argb(), shadow);
                }
            } else if (textType == TextTexture.TextType.HIDE) {
                int renderY = lambdaY * (lineHeight + 2);
                renderY += lineHeight - lineHeight;
                FormattedCharSequence line = this.wrappedText.size() > 1 ?
                        FormattedCharSequence.composite(this.wrappedText.get(0),
                                Component.literal("..").getVisualOrderText()) :
                        this.wrappedText.get(0);

                if (Widget.isMouseOver(renderX, renderY, width, height, mouseX, mouseY) &&
                        this.wrappedText.size() > 1) {
                    drawRollTextLine(graphics, renderX, renderY, width, height, font, lineHeight, line);
                } else {
                    drawTextLine(graphics, renderX, renderY, width, height, font, lineHeight, line);
                }
            } else if (textType == TextTexture.TextType.ROLL || textType == TextTexture.TextType.ROLL_ALWAYS) {
                int renderY = lambdaY * (lineHeight + 2);
                renderY += lineHeight - lineHeight;

                var line = this.wrappedText.get(0);
                switch (this.horizontalTextAlignment) {
                    case CENTER -> renderX += (this.width - this.font.width(line)) / 2;
                    case RIGHT -> renderX += this.width - this.font.width(line);
                }

                if (this.wrappedText.size() > 1 && (textType == TextTexture.TextType.ROLL_ALWAYS ||
                        Widget.isMouseOver(renderX, renderY, width, height, mouseX, mouseY))) {
                    drawRollTextLine(graphics, renderX, renderY, width, height, font, lineHeight, line);
                } else {
                    drawTextLine(graphics, renderX, renderY, width, height, font, lineHeight, line);
                }
            } else if (textType == TextTexture.TextType.LEFT_HIDE) {
                int renderY = lambdaY * (lineHeight + 2);
                renderY += lineHeight - lineHeight;
                FormattedCharSequence line = this.wrappedText.size() > 1 ?
                        FormattedCharSequence.composite(this.wrappedText.get(0),
                                Component.literal("..").getVisualOrderText()) :
                        this.wrappedText.get(0);
                switch (this.horizontalTextAlignment) {
                    case CENTER -> renderX += (this.width - this.font.width(line)) / 2;
                    case RIGHT -> renderX += this.width - this.font.width(line);
                }

                if (Widget.isMouseOver(renderX, renderY, width, height, mouseX, mouseY) &&
                        this.wrappedText.size() > 1) {
                    drawRollTextLine(graphics, renderX, renderY, width, height, font, lineHeight, line);
                } else {
                    graphics.drawString(font, line, renderX, renderY, color.get().argb(), shadow);
                }
            } else
                if (textType == TextTexture.TextType.LEFT_ROLL || textType == TextTexture.TextType.LEFT_ROLL_ALWAYS) {
                    int renderY = lambdaY * (lineHeight + 2);
                    renderY += lineHeight - lineHeight;

                    var line = this.wrappedText.get(0);
                    switch (this.horizontalTextAlignment) {
                        case CENTER -> renderX += (this.width - this.font.width(line)) / 2;
                        case RIGHT -> renderX += this.width - this.font.width(line);
                    }

                    if (this.wrappedText.size() > 1 && (textType == TextTexture.TextType.LEFT_ROLL_ALWAYS ||
                            Widget.isMouseOver(renderX, renderY, width, height, mouseX, mouseY))) {
                        drawRollTextLine(graphics, renderX, renderY, width, height, font, lineHeight, line);
                    } else {
                        graphics.drawString(font, line, renderX, renderY, color.get().argb(), shadow);
                    }
                }
        });

        pose.popPose();
    }

    private void drawRollTextLine(UIGuiGraphics graphics, float x, float y, int width, int height,
                                  Font fontRenderer, int textH, FormattedCharSequence line) {
        float renderY = y + (height - textH) / 2f;
        int textW = fontRenderer.width(line);
        int totalW = width + textW + 10;
        float from = x + width;
        var trans = graphics.pose().last().pose();
        var realPos = new Matrix4f(trans).transform(new Vector4f(x, y, 0, 1));
        var realPos2 = new Matrix4f(trans).transform(new Vector4f(x + width, y + height, 0, 1));
        graphics.enableScissor((int) realPos.x, (int) realPos.y, (int) realPos2.x, (int) realPos2.y);
        var t = rollSpeed > 0 ?
                ((((rollSpeed * Math.abs((int) (System.currentTimeMillis() % 1000000)) / 10) % (totalW))) / (totalW)) :
                0.5;
        graphics.drawString(fontRenderer, line, (int) (from - t * totalW), (int) renderY, color.get().argb(), shadow);
        graphics.disableScissor();
    }

    private void drawTextLine(GuiGraphics graphics, float x, float y, int width, int height, Font fontRenderer,
                              int textH, FormattedCharSequence line) {
        int textW = fontRenderer.width(line);
        float _x = x + (width - textW) / 2f;
        float _y = y + (height - textH) / 2f;
        graphics.drawString(fontRenderer, line, (int) _x, (int) _y, color.get().argb(), shadow);
    }

    @Override
    public void drawTooltip(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        super.drawTooltip(graphics, mouseX, mouseY, partialTicks, delta);

        if (!this.isInBoundingBox(mouseX, mouseY)) return;
        graphics.renderComponentHoverEffect(this.font, this.styleAt(mouseX - this.x, mouseY - this.y), mouseX,
                mouseY);
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        return this.textClickHandler.apply(this.styleAt((int) mouseX, (int) mouseY)) |
                super.onMouseDown(mouseX, mouseY, button);
    }

    protected Style styleAt(int mouseX, int mouseY) {
        return this.font.getSplitter().componentStyleAtWidth(
                this.wrappedText.get(Math.min(mouseY / (this.lineHeight() + 2), this.wrappedText.size() - 1)), mouseX);
    }

    @Override
    public void parseProperties(UIModel model, Element element, Map<String, Element> children) {
        super.parseProperties(model, element, children);
        UIParsing.apply(children, "text", UIParsing::parseComponent, this::text);
        UIParsing.apply(children, "max-width", UIParsing::parseUnsignedInt, this::maxWidth);
        UIParsing.apply(children, "color", Color::parse, this::color);
        UIParsing.apply(children, "shadow", UIParsing::parseBool, this::shadow);
        UIParsing.apply(children, "line-height", UIParsing::parseUnsignedInt, this::lineHeight);

        UIParsing.apply(children, "vertical-text-alignment", VerticalAlignment::parse, this::verticalTextAlignment);
        UIParsing.apply(children, "horizontal-text-alignment", HorizontalAlignment::parse,
                this::horizontalTextAlignment);

        UIParsing.apply(children, "roll-speed", UIParsing::parseFloat, this::rollSpeed);
        UIParsing.apply(children, "text-type", UIParsing.parseEnum(TextTexture.TextType.class), this::textType);
    }
}
