package com.gregtechceu.gtceu.api.mui.drawable.text;

import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.Supplier;

public class DynamicKey extends BaseKey {

    private final Supplier<Component> supplier;

    public DynamicKey(Supplier<Component> supplier) {
        Objects.requireNonNull(supplier.get(), "IKey returns a null string!");
        this.supplier = supplier;
    }

    @Override
    public Component get() {
        return this.supplier.get();
    }
}
