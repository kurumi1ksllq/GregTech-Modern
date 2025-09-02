package com.gregtechceu.gtceu.api.placeholder.exceptions;

import com.gregtechceu.gtceu.utils.GTUtil;

public class UnclosedBracketException extends PlaceholderException {

    public UnclosedBracketException() {
        super(GTUtil.translatable("gtceu.computer_monitor_cover.error.unclosed_bracket").getString());
    }
}
