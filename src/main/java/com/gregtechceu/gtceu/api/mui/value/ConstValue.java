package com.gregtechceu.gtceu.api.mui.value;

import com.gregtechceu.gtceu.api.mui.base.value.IValue;

public class ConstValue<T> implements IValue<T> {

    protected T value;

    public ConstValue(T value) {
        this.value = value;
    }

    @Override
    public T getValue() {
        return this.value;
    }

    @Override
    public void setValue(T value) {
        this.value = value;
    }
}
