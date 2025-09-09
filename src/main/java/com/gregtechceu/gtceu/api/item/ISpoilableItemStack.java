package com.gregtechceu.gtceu.api.item;

import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Unique;

public interface ISpoilableItemStack {

    long gtceu$getCreationTick(@Nullable Level level);

    void gtceu$setCreationTick(@Nullable Level level, long value);

    long gtceu$getRemainingTicks(@Nullable Level level);

    void gtceu$setFreezeSpoiling(boolean freezeUpdates);

    void gtceu$updateFreshness(@Nullable Level level, boolean createTag);

    @Unique
    void gtceu$setRemainingTicks(@javax.annotation.Nullable Level level, long value);
}
