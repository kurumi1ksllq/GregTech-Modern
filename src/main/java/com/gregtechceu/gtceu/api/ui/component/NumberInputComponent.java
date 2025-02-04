package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.GuiTextures;
import com.gregtechceu.gtceu.api.ui.container.FlowLayout;
import com.gregtechceu.gtceu.api.ui.core.Positioning;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.texture.UITexture;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;
import com.gregtechceu.gtceu.api.ui.util.ClickData;
import com.gregtechceu.gtceu.utils.GTUtil;

import com.lowdragmc.lowdraglib.LDLib;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A widget containing an integer input field, as well as adjacent buttons for increasing or decreasing the value.
 *
 * <p>
 * The buttons' change amount can be altered with Ctrl, Shift, or both.<br>
 * The input is limited by a minimum and maximum value.
 * </p>
 */
@Accessors(fluent = true, chain = true)
public abstract class NumberInputComponent<T extends Number> extends FlowLayout {

    protected abstract T defaultMin();

    protected abstract T defaultMax();

    protected abstract String toText(T value);

    protected abstract T fromText(String value);

    protected record ChangeValues<T extends Number>(T regular, T shift, T ctrl, T ctrlShift) {}

    protected abstract ChangeValues<T> getChangeValues();

    protected abstract T add(T a, T b);

    protected abstract T multiply(T a, T b);

    protected abstract T clamp(T value, T min, T max);

    protected abstract void setTextFieldRange(TextBoxComponent textField, T min, T max);

    protected abstract T getOne(boolean positive);

    /////////////////////////////////////////////////
    // *********** IMPLEMENTATION ***********//
    /////////////////////////////////////////////////

    private final ChangeValues<T> CHANGE_VALUES = getChangeValues();
    private final T ONE_POSITIVE = getOne(true);
    private final T ONE_NEGATIVE = getOne(false);

    @Getter
    private Supplier<T> valueSupplier;
    @Getter
    private T min = defaultMin();
    @Getter
    private T max = defaultMax();

    private final Consumer<T> onChanged;

    private TextBoxComponent textField;

    public NumberInputComponent(Supplier<T> valueSupplier, Consumer<T> onChanged) {
        this(Sizing.fixed(100), Sizing.fixed(20), valueSupplier, onChanged);
    }

    public NumberInputComponent(Sizing horizontalSizing, Sizing verticalSizing, Supplier<T> valueSupplier,
                                Consumer<T> onChanged) {
        super(horizontalSizing, verticalSizing, Algorithm.HORIZONTAL);
        this.valueSupplier = valueSupplier;
        this.onChanged = onChanged;
        buildUI();
    }

    @Override
    public void init() {
        super.init();
        textField.text(toText(valueSupplier.get()));
    }

    /*
     * @Override
     * public void writeInitialData(FriendlyByteBuf buffer) {
     * super.writeInitialData(buffer);
     * buffer.writeUtf(toText(valueSupplier.get()));
     * }
     *
     * @Override
     * public void readInitialData(FriendlyByteBuf buffer) {
     * super.readInitialData(buffer);
     * textField.setCurrentString(buffer.readUtf());
     * }
     */

    private void buildUI() {
        int buttonWidth = Mth.clamp(this.width() / 5, 15, 40);

        this.child(UIComponents.button(Component.empty(), this::decrease)
                .renderer(ButtonComponent.Renderer
                        .texture(UITextures.group(GuiTextures.VANILLA_BUTTON, getButtonTexture("-", buttonWidth))))
                .sizing(Sizing.fill(20), Sizing.fixed(20))
                .positioning(Positioning.relative(0, 0))
                .tooltip(List.of(Component.translatable("gui.widget.incrementButton.default_tooltip"))));

        this.textField = UIComponents.textBox(Sizing.fixed(60), toText(valueSupplier.get()))
                .textSupplier(() -> toText(valueSupplier.get()));
        this.textField.onChanged().subscribe(stringValue -> this.setValue(clamp(fromText(stringValue), min, max)));
        this.updateTextFieldRange();
        this.child(this.textField);

        this.child(UIComponents.button(Component.empty(), this::increase)
                .renderer(ButtonComponent.Renderer
                        .texture(UITextures.group(GuiTextures.VANILLA_BUTTON, getButtonTexture("+", buttonWidth))))
                .tooltip(List.of(Component.translatable("gui.widget.incrementButton.default_tooltip"))));
    }

    private UITexture getButtonTexture(String prefix, int buttonWidth) {
        var texture = UITextures.text(Component.literal(prefix + "1"));

        if (!GTCEu.isClientThread()) {
            return texture;
        }

        // Dynamic text is only necessary on the remote side:

        int maxTextWidth = buttonWidth - 4;

        texture.textSupplier(() -> {
            T amount = GTUtil.isCtrlDown() ?
                    GTUtil.isShiftDown() ? CHANGE_VALUES.ctrlShift : CHANGE_VALUES.ctrl :
                    GTUtil.isShiftDown() ? CHANGE_VALUES.shift : CHANGE_VALUES.regular;

            String text = prefix + toText(amount);

            texture.scale(maxTextWidth / (float) Math.max(Minecraft.getInstance().font.width(text), maxTextWidth));

            return Component.literal(text);
        });

        return texture;
    }

    private void increase(ClickData cd) {
        this.changeValue(cd, ONE_POSITIVE);
    }

    private void decrease(ClickData cd) {
        this.changeValue(cd, ONE_NEGATIVE);
    }

    private void changeValue(ClickData cd, T multiplier) {
        if (!cd.isClientSide) {
            T amount = cd.isCtrlClick ?
                    cd.isShiftClick ? CHANGE_VALUES.ctrlShift : CHANGE_VALUES.ctrl :
                    cd.isShiftClick ? CHANGE_VALUES.shift : CHANGE_VALUES.regular;

            this.setValue(clamp(add(valueSupplier.get(), multiply(amount, multiplier)), min, max));
        }
    }

    public NumberInputComponent<T> setMin(T min) {
        this.min = min;
        updateTextFieldRange();

        return this;
    }

    public NumberInputComponent<T> setMax(T max) {
        this.max = max;
        updateTextFieldRange();

        return this;
    }

    public NumberInputComponent<T> setValue(T value) {
        onChanged.accept(value);
        return this;
    }

    protected void updateTextFieldRange() {
        setTextFieldRange(textField, min, max);

        this.setValue(clamp(valueSupplier.get(), min, max));
    }
}
