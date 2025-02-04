package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.core.Sizing;

import net.minecraft.util.Mth;

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
public class IntInputComponent extends NumberInputComponent<Integer> {

    public IntInputComponent(Supplier<Integer> valueSupplier, Consumer<Integer> onChanged) {
        super(valueSupplier, onChanged);
    }

    public IntInputComponent(Sizing horizontalSizing, Sizing verticalSizing, Supplier<Integer> valueSupplier,
                             Consumer<Integer> onChanged) {
        super(horizontalSizing, verticalSizing, valueSupplier, onChanged);
    }

    @Override
    protected Integer defaultMin() {
        return 0;
    }

    @Override
    protected Integer defaultMax() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected String toText(Integer value) {
        return String.valueOf(value);
    }

    @Override
    protected Integer fromText(String value) {
        if (value.isEmpty())
            return 0;
        return Integer.parseInt(value);
    }

    @Override
    protected NumberInputComponent.ChangeValues<Integer> getChangeValues() {
        return new NumberInputComponent.ChangeValues<>(1, 8, 64, 512);
    }

    @Override
    protected Integer getOne(boolean positive) {
        return positive ? 1 : -1;
    }

    @Override
    protected Integer add(Integer a, Integer b) {
        return a + b;
    }

    @Override
    protected Integer multiply(Integer a, Integer b) {
        return a * b;
    }

    @Override
    protected Integer clamp(Integer value, Integer min, Integer max) {
        return Mth.clamp(value, min, max);
    }

    @Override
    protected void setTextFieldRange(TextBoxComponent textField, Integer min, Integer max) {
        textField.numbersOnly(min, max);
    }
}
