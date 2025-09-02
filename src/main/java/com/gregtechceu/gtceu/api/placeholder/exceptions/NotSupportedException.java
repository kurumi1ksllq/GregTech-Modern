package com.gregtechceu.gtceu.api.placeholder.exceptions;

import com.gregtechceu.gtceu.utils.GTUtil;

public class NotSupportedException extends PlaceholderException {

    public NotSupportedException() {
        super(GTUtil.translatable("gtceu.computer_monitor_cover.error.not_supported").getString());
    }
}
