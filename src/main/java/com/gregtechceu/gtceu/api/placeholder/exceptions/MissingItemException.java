package com.gregtechceu.gtceu.api.placeholder.exceptions;

import com.gregtechceu.gtceu.utils.GTUtil;

public class MissingItemException extends PlaceholderException {

    public MissingItemException(String item, int slot) {
        super(GTUtil.translatable("gtceu.computer_monitor_cover.error.missing_item", item, slot).getString());
    }
}
