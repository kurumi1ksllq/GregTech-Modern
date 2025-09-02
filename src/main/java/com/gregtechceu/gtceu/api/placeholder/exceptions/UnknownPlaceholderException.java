package com.gregtechceu.gtceu.api.placeholder.exceptions;

import com.gregtechceu.gtceu.utils.GTUtil;

public class UnknownPlaceholderException extends PlaceholderException {

    public UnknownPlaceholderException(String name) {
        super(GTUtil.translatable("gtceu.computer_monitor_cover.error.no_placeholder", name).getString());
    }
}
