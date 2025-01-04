package com.gregtechceu.gtceu.api.cover.filter;

import com.gregtechceu.gtceu.common.cover.data.FilterMode;
import com.gregtechceu.gtceu.common.cover.data.ManualIOMode;

import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

public interface CoverWithItemFilter {

    @NotNull
    FilterHandler<ItemStack, ItemFilter> getFilterHandler();

    FilterMode getFilterMode();

    ManualIOMode getManualIOMode();
}
