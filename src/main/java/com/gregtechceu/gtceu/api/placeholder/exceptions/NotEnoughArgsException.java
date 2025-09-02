package com.gregtechceu.gtceu.api.placeholder.exceptions;

import com.gregtechceu.gtceu.utils.GTUtil;

public class NotEnoughArgsException extends PlaceholderException {

    public NotEnoughArgsException(int expected, int got) {
        super(GTUtil.translatable("gtceu.computer_monitor_cover.error.not_enough_args", expected, got).getString());
    }
}
