package com.gregtechceu.gtceu.api.placeholder.exceptions;

import com.gregtechceu.gtceu.utils.GTUtil;

public class InvalidArgsException extends PlaceholderException {

    public InvalidArgsException() {
        super(GTUtil.translatable("gtceu.computer_monitor_cover.error.invalid_args").getString());
    }
}
