package com.gregtechceu.gtceu.api.placeholder.exceptions;

import com.gregtechceu.gtceu.utils.GTUtil;

public class NoMENetworkException extends PlaceholderException {

    public NoMENetworkException() {
        super(GTUtil.translatable("gtceu.computer_monitor_cover.error.no_ae").getString());
    }
}
