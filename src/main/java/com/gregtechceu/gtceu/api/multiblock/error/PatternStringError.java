package com.gregtechceu.gtceu.api.multiblock.error;

import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

public class PatternStringError extends PatternError {

    public final String translateKey;
    public final Object[] args;

    public PatternStringError(String translateKey, Object... args) {
        super(null, Collections.emptyList());
        this.translateKey = translateKey;
        this.args = args;
    }

    @Override
    public List<Component> getErrorInfo() {
        return List.of(Component.translatable(translateKey, args));
    }
}
