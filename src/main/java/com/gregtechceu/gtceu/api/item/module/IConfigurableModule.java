package com.gregtechceu.gtceu.api.item.module;

import com.lowdragmc.lowdraglib.gui.widget.Widget;

import org.jetbrains.annotations.NotNull;

public interface IConfigurableModule {

    @NotNull
    Widget createConfigUI(@NotNull AppliedItemModule module);
}
