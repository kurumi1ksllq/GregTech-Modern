package com.gregtechceu.gtceu.api.placeholder.exceptions;

import com.gregtechceu.gtceu.utils.GTUtil;

public class InvalidNumberException extends PlaceholderException {

    public InvalidNumberException(String number) {
        super(GTUtil.translatable("gtceu.computer_monitor_cover.error.invalid_number", number).getString());
    }
}
