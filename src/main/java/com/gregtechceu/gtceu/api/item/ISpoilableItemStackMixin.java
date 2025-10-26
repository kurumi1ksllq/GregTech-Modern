package com.gregtechceu.gtceu.api.item;

import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

public interface ISpoilableItemStackMixin {
    void gtceu$updateFreshness(@Nullable Level level, boolean createTag);
}
