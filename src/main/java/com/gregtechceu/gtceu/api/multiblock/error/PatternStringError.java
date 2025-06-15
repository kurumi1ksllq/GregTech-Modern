package com.gregtechceu.gtceu.api.multiblock.error;

import net.minecraft.network.chat.Component;

import java.util.Collections;
import java.util.List;

public class PatternStringError extends PatternError {

    public final String translateKey;

    public PatternStringError(String translateKey) {
        super(null, Collections.emptyList());
        this.translateKey = translateKey;
    }

    @Override
    public List<Component> getErrorInfo() {
        return List.of(Component.translatable(translateKey));
    }
}
