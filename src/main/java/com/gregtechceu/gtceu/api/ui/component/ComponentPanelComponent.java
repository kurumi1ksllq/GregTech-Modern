package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.base.BaseUIComponent;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIComponent;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.util.ClickData;

import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.lowdragmc.lowdraglib.LDLib.isRemote;

@Accessors(fluent = true, chain = true)
public class ComponentPanelComponent extends BaseUIComponent {

    protected int maxWidthLimit;
    @Setter
    @Nullable
    protected Consumer<List<Component>> textSupplier;
    @Setter
    protected BiConsumer<String, ClickData> clickHandler;
    protected List<Component> lastText = new ArrayList<>();
    @Getter
    protected List<FormattedCharSequence> cacheLines = Collections.emptyList();
    protected boolean isCenter = false;
    protected int space = 2;

    protected ComponentPanelComponent(@NotNull Consumer<List<Component>> textSupplier) {
        this.textSupplier = textSupplier;
        this.textSupplier.accept(lastText);
    }

    protected ComponentPanelComponent(List<Component> text) {
        this.lastText.addAll(text);
    }

    public static Component withButton(Component textComponent, String componentData) {
        var style = textComponent.getStyle();
        style = style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "@!" + componentData));
        style = style.withColor(ChatFormatting.YELLOW);
        return textComponent.copy().withStyle(style);
    }

    public static Component withButton(Component textComponent, String componentData, int color) {
        var style = textComponent.getStyle();
        style = style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "@!" + componentData));
        style = style.withColor(color);
        return textComponent.copy().withStyle(style);
    }

    public static Component withHoverTextTranslate(Component textComponent, Component hover) {
        var style = textComponent.getStyle();
        style = style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover));
        return textComponent.copy().withStyle(style);
    }

    public ComponentPanelComponent maxWidthLimit(int maxWidthLimit) {
        this.maxWidthLimit = maxWidthLimit;
        if (isRemote()) {
            formatDisplayText();
            updateComponentTextSize();
        }
        return this;
    }

    public ComponentPanelComponent center(boolean center) {
        isCenter = center;
        if (isRemote()) {
            formatDisplayText();
            updateComponentTextSize();
        }
        return this;
    }

    public ComponentPanelComponent setSpace(int space) {
        this.space = space;
        if (isRemote()) {
            formatDisplayText();
            updateComponentTextSize();
        }
        return this;
    }

    @Override
    public void init() {
        super.init();
        if (textSupplier != null) {
            lastText.clear();
            textSupplier.accept(lastText);
        }
        formatDisplayText();
        updateComponentTextSize();
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        if (textSupplier != null) {
            List<Component> textBuffer = new ArrayList<>();
            textSupplier.accept(textBuffer);
            if (!lastText.equals(textBuffer)) {
                this.lastText = textBuffer;
                formatDisplayText();
                updateComponentTextSize();
            }
        }

        var style = getStyleUnderMouse(mouseX, mouseY);
        if (style != null) {
            if (style.getHoverEvent() != null) {
                var hoverEvent = style.getHoverEvent();
                var hoverTips = hoverEvent.getValue(HoverEvent.Action.SHOW_TEXT);
                if (hoverTips != null) {
                    tooltip(List.of(hoverTips));
                } else {
                    tooltip(List.<ClientTooltipComponent>of());
                }
            } else {
                tooltip(List.<ClientTooltipComponent>of());
            }
        } else {
            tooltip(List.<ClientTooltipComponent>of());
        }

        if (textSupplier != null) {
            List<Component> textBuffer = new ArrayList<>();
            textSupplier.accept(textBuffer);
            if (!lastText.equals(textBuffer)) {
                this.lastText = textBuffer;
                /*
                sendMessage(1, buffer -> {
                    buffer.writeVarInt(lastText.size());
                    for (Component textComponent : lastText) {
                        buffer.writeComponent(textComponent);
                    }
                });
                */
            }
        }
    }

    /*
    @Override
    public void receiveMessage(int id, FriendlyByteBuf buf) {
        if (id == 1) {
            this.lastText.clear();
            int count = buf.readVarInt();
            for (int i = 0; i < count; i++) {
                this.lastText.add(buf.readComponent());
            }
            formatDisplayText();
            updateComponentTextSize();
        } else if (id == 2) {
            ClickData clickData = ClickData.readFromBuf(buf);
            String componentData = buf.readUtf();
            if (clickHandler != null) {
                clickHandler.accept(componentData, clickData);
            }
        } else {
            super.receiveMessage(id, buf);
        }
    }
    */

    public void updateComponentTextSize() {
        var font = Minecraft.getInstance().font;
        int totalHeight = cacheLines.size() * (font.lineHeight + space);
        if (totalHeight > 0) {
            totalHeight -= space;
        }
        if (isCenter) {
            this.width(maxWidthLimit);
            this.height(totalHeight);
        } else {
            int maxStringWidth = 0;
            for (var line : cacheLines) {
                maxStringWidth = Math.max(font.width(line), maxStringWidth);
            }
            this.width(maxWidthLimit == 0 ? maxStringWidth : Math.min(maxWidthLimit, maxStringWidth));
            this.height(totalHeight);
        }
    }

    public void formatDisplayText() {
        var font = Minecraft.getInstance().font;
        int maxTextWidthResult = maxWidthLimit == 0 ? Integer.MAX_VALUE : maxWidthLimit;
        this.cacheLines = lastText.stream()
                .flatMap(component -> ComponentRenderUtils.wrapComponents(component, maxTextWidthResult, font).stream())
                .toList();
    }

    @Nullable
    protected Style getStyleUnderMouse(double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;

        var selectedLine = (mouseY - y()) / (font.lineHeight + space);
        if (isCenter) {
            if (selectedLine >= 0 && selectedLine < cacheLines.size()) {
                var cacheLine = cacheLines.get((int) selectedLine);
                var lineWidth = font.width(cacheLine);
                var offsetX = x() + (width() - lineWidth) / 2f;
                if (mouseX >= offsetX) {
                    var mouseOffset = (int) (mouseX - x());
                    return font.getSplitter().componentStyleAtWidth(cacheLine, mouseOffset);
                }
            }
        } else {
            if (mouseX >= x() && selectedLine >= 0 && selectedLine < cacheLines.size()) {
                var cacheLine = cacheLines.get((int) selectedLine);
                var mouseOffset = (int) (mouseX - x());
                return font.getSplitter().componentStyleAtWidth(cacheLine, mouseOffset);
            }
        }
        return null;
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        var style = getStyleUnderMouse(mouseX, mouseY);
        if (style != null) {
            if (style.getClickEvent() != null) {
                ClickEvent clickEvent = style.getClickEvent();
                String componentText = clickEvent.getValue();
                if (clickEvent.getAction() == ClickEvent.Action.OPEN_URL) {
                    if (componentText.startsWith("@!")) {
                        String rawText = componentText.substring(2);
                        if (clickHandler != null) {
                            ClickData clickData = new ClickData(button);
                            clickHandler.accept(rawText, clickData);
                        }
                        //sendMessage(2, buf -> {
                        //    buf.writeUtf(rawText);
                        //});
                    } else if (componentText.startsWith("@#")) {
                        String rawText = componentText.substring(2);
                        Util.getPlatform().openUri(rawText);
                    }
                    UIComponent.playButtonClickSound();
                    return true;
                }
            }
        }
        return super.onMouseDown(mouseX, mouseY, button);
    }

    @Override
    public int determineHorizontalContentSize(Sizing sizing) {
        return 13;
    }

    @Override
    public int determineVerticalContentSize(Sizing sizing) {
        return 13;
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        var fontRenderer = Minecraft.getInstance().font;
        for (int i = 0; i < cacheLines.size(); i++) {
            var cacheLine = cacheLines.get(i);
            if (isCenter) {
                var lineWidth = fontRenderer.width(cacheLine);
                graphics.drawString(fontRenderer, cacheLine, x() + (width() - lineWidth) / 2,
                        y() + i * (fontRenderer.lineHeight + space), -1);
            } else {
                graphics.drawString(fontRenderer, cacheLines.get(i), x(), y() + i * (fontRenderer.lineHeight + 2), -1);
            }
        }
    }
}
