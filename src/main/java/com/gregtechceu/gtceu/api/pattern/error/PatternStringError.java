package com.gregtechceu.gtceu.api.pattern.error;

import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.network.chat.Component;

public class PatternStringError extends PatternError {

    public final String translateKey;

    public PatternStringError(String translateKey) {
        this.translateKey = translateKey;
    }

    @Override
    public Component getErrorInfo() {
        return GTUtil.translatable(translateKey);
    }
}
