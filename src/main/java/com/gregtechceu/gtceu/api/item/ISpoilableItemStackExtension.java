package com.gregtechceu.gtceu.api.item;

import com.gregtechceu.gtceu.api.item.component.SpoilContext;

import org.jetbrains.annotations.NotNull;

public interface ISpoilableItemStackExtension {

    void gtceu$updateFreshness(@NotNull SpoilContext spoilContext, boolean createTag);

    SpoilContext gtceu$getSpoilContext();
}
