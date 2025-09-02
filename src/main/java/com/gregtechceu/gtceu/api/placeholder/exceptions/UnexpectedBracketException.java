package com.gregtechceu.gtceu.api.placeholder.exceptions;

import com.gregtechceu.gtceu.utils.GTUtil;

public class UnexpectedBracketException extends RuntimeException {

    public UnexpectedBracketException() {
        super(GTUtil.translatable("gtceu.computer_monitor_cover.error.unexpected_bracket").getString());
    }
}
