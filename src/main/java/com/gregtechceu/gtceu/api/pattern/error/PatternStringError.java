package com.gregtechceu.gtceu.api.pattern.error;

import net.minecraft.network.chat.Component;

import java.util.Collections;

public class PatternStringError extends PatternError {

    public final String translateKey;

    public PatternStringError(String translateKey) {
        super(null, Collections.emptyList());
        this.translateKey = translateKey;
    }

    @Override
    public Component getErrorInfo() {
        return Component.translatable(translateKey);
    }
}
