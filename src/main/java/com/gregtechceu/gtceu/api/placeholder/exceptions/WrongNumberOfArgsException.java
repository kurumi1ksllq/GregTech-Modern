package com.gregtechceu.gtceu.api.placeholder.exceptions;

import com.gregtechceu.gtceu.utils.GTUtil;

public class WrongNumberOfArgsException extends PlaceholderException {

    public WrongNumberOfArgsException(int expected, int got) {
        super(GTUtil.translatable("gtceu.computer_monitor_cover.error.wrong_number_of_args", expected, got)
                .getString());
    }
}
