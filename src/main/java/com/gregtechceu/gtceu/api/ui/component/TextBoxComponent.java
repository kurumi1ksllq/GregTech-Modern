package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.core.CursorStyle;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.parsing.UIModel;
import com.gregtechceu.gtceu.api.ui.parsing.UIParsing;
import com.gregtechceu.gtceu.api.ui.util.EventSource;
import com.gregtechceu.gtceu.api.ui.util.EventStream;
import com.gregtechceu.gtceu.api.ui.util.Observable;
import com.gregtechceu.gtceu.core.mixins.ui.accessor.EditBoxAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import lombok.Setter;
import lombok.experimental.Accessors;
import org.lwjgl.glfw.GLFW;
import org.w3c.dom.Element;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

@Accessors(fluent = true, chain = true)
public class TextBoxComponent extends EditBox {

    protected final Observable<Boolean> showsBackground = Observable
            .of(((EditBoxAccessor) this).gtceu$isBordered());

    @Setter
    protected Supplier<String> textSupplier;
    protected final Observable<String> textValue = Observable.of("");
    protected final EventStream<OnChanged> changedEvents = OnChanged.newStream();

    protected float wheelDur;
    protected NumberFormat numberInstance;
    protected Component hover;
    private boolean isDragging;

    @Setter
    private Function<String, String> validator;

    protected TextBoxComponent(Sizing horizontalSizing) {
        super(Minecraft.getInstance().font, 0, 0, 0, 0, Component.empty());

        this.setResponder(textValue::set);
        this.textValue.observe(this.changedEvents.sink()::onChanged);
        this.sizing(horizontalSizing, Sizing.content());

        this.showsBackground.observe(a -> this.widgetWrapper().notifyParentIfMounted());
    }

    @Override
    public void drawFocusHighlight(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        // noop, since EditBox already does this
    }

    @Override
    public void update(float delta, int mouseX, int mouseY) {
        super.update(delta, mouseX, mouseY);
        if (this.isVisible() && this.isActive() && textSupplier != null && !textSupplier.get().equals(getValue())) {
            text(textSupplier.get());
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean result = super.keyPressed(keyCode, scanCode, modifiers);

        if (keyCode == GLFW.GLFW_KEY_TAB) {
            this.insertText("    ");
            return true;
        } else {
            return result;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseOverElement(mouseX, mouseY)) {
            isDragging = true;
        }
        setFocused(isMouseOverElement(mouseX, mouseY));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double wheelDelta) {
        if (wheelDur > 0 && numberInstance != null && isMouseOverElement(mouseX, mouseY) && isFocused()) {
            try {
                text(numberInstance.format(Float.parseFloat(getValue()) + (wheelDelta > 0 ? 1 : -1) * wheelDur));
            } catch (Exception ignored) {}
            setFocused(true);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (isDragging && numberInstance != null && isFocused()) {
            try {
                text(numberInstance.format(Float.parseFloat(getValue()) + dragX * wheelDur));
            } catch (Exception ignored) {}
            setFocused(true);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void setBordered(boolean drawsBackground) {
        super.setBordered(drawsBackground);
        this.showsBackground.set(drawsBackground);
    }

    public EventSource<OnChanged> onChanged() {
        return changedEvents.source();
    }

    public TextBoxComponent text(String text) {
        this.setValue(text);
        this.moveCursorToStart();
        return this;
    }

    protected CursorStyle gtceu$preferredCursorStyle() {
        return CursorStyle.TEXT;
    }

    public TextBoxComponent setCompoundTagOnly() {
        setFilter(s -> {
            try {
                TagParser.parseTag(s);
                return true;
            } catch (Exception ignored) {}
            return false;
        });
        hover = Component.translatable("ldlib.gui.text_field.compound_tag");
        return this;
    }

    public TextBoxComponent setResourceLocationOnly() {
        setFilter(s -> {
            s = s.toLowerCase();
            s = s.replace(' ', '_');
            return ResourceLocation.isValidResourceLocation(s);
        });
        hover = Component.translatable("ldlib.gui.text_field.resourcelocation");
        return this;
    }

    public TextBoxComponent numbersOnly(long minValue, long maxValue) {
        setFilter(s -> {
            try {
                if (s == null) return false;
                if (s.isEmpty()) return true;
                float value = Long.parseLong(s);
                if (minValue <= value && value <= maxValue) return true;
                if (value < minValue) return false;
                return true;
            } catch (NumberFormatException ignored) {}
            return false;
        });
        if (minValue == Long.MIN_VALUE && maxValue == Long.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.3");
        } else if (minValue == Long.MIN_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.2", maxValue);
        } else if (maxValue == Long.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.1", minValue);
        } else {
            hover = Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue);
        }
        return this; // setWheelDur(1);
    }

    public TextBoxComponent numbersOnly(int minValue, int maxValue) {
        setFilter(s -> {
            try {
                if (s == null) return false;
                if (s.isEmpty()) return true;
                float value = Integer.parseInt(s);
                if (minValue <= value && value <= maxValue) return true;
                if (value < minValue) return false;
                return true;
            } catch (NumberFormatException ignored) {}
            return false;
        });
        if (minValue == Integer.MIN_VALUE && maxValue == Integer.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.3");
        } else if (minValue == Integer.MIN_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.2", maxValue);
        } else if (maxValue == Integer.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.1", minValue);
        } else {
            hover = Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue);
        }
        return this; // setWheelDur(1);
    }

    public TextBoxComponent numbersOnly(float minValue, float maxValue) {
        setFilter(s -> {
            try {
                if (s == null) return false;
                if (s.isEmpty()) return true;
                float value = Float.parseFloat(s);
                if (minValue <= value && value <= maxValue) return true;
                if (value < minValue) return false;
                return true;
            } catch (NumberFormatException ignored) {}
            return false;
        });
        if (minValue == -Float.MAX_VALUE && maxValue == Float.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.3");
        } else if (minValue == -Float.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.2", maxValue);
        } else if (maxValue == Float.MAX_VALUE) {
            hover = Component.translatable("ldlib.gui.text_field.number.1", minValue);
        } else {
            hover = Component.translatable("ldlib.gui.text_field.number.0", minValue, maxValue);
        }
        return this; // setWheelDur(0.1f);
    }

    public TextBoxComponent wheelDur(float wheelDur) {
        this.wheelDur = wheelDur;
        this.numberInstance = NumberFormat.getNumberInstance();
        numberInstance.setMaximumFractionDigits(4);
        return this;
    }

    public TextBoxComponent wheelDur(int digits, float wheelDur) {
        this.wheelDur = wheelDur;
        this.numberInstance = NumberFormat.getNumberInstance();
        numberInstance.setMaximumFractionDigits(digits);
        return this;
    }

    @Override
    public List<ClientTooltipComponent> tooltip() {
        List<ClientTooltipComponent> tooltip = super.tooltip();
        if (tooltip == null) {
            return List.of(ClientTooltipComponent.create(hover.getVisualOrderText()));
        }
        if (hover != null) {
            tooltip = new ArrayList<>(tooltip);
            tooltip.add(ClientTooltipComponent.create(hover.getVisualOrderText()));
        }
        return Collections.unmodifiableList(tooltip);
    }

    @Override
    public void parseProperties(UIModel spec, Element element, Map<String, Element> children) {
        super.parseProperties(spec, element, children);
        UIParsing.apply(children, "show-background", UIParsing::parseBool, this::setBordered);
        UIParsing.apply(children, "max-length", UIParsing::parseUnsignedInt, this::setMaxLength);
        UIParsing.apply(children, "text", e -> e.getTextContent().strip(), this::text);
    }

    public interface OnChanged {

        void onChanged(String value);

        static EventStream<OnChanged> newStream() {
            return new EventStream<>(subscribers -> value -> {
                for (var subscriber : subscribers) {
                    subscriber.onChanged(value);
                }
            });
        }
    }
}
