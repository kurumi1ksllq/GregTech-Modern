package com.gregtechceu.gtceu.api.item;

import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

public interface ISpoilableItemStack {

    long gtceu$getCreationTick(@Nullable Level level);

    void gtceu$updateFreshness(@Nullable Level level);
}
