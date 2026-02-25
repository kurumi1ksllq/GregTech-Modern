package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.item.IMergeableNBTSerializable;

import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.CapabilityDispatcher;
import net.minecraftforge.common.util.INBTSerializable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = CapabilityDispatcher.class, remap = false)
public abstract class CapabilityDispatcherMixin {

    @Unique
    private static void gtceu$beforeComparison(@NotNull Object first, @Nullable Object second) {
        Map<String, INBTSerializable<Tag>> map = new HashMap<>();
        CapabilityDispatcherAccessor accessor = (CapabilityDispatcherAccessor) first;
        CapabilityDispatcherAccessor otherAccessor = (CapabilityDispatcherAccessor) second;
        if (otherAccessor != null) {
            for (int i = 0; i < otherAccessor.getWriters().length; i++) {
                map.put(otherAccessor.getNames()[i], otherAccessor.getWriters()[i]);
            }
        }
        for (int i = 0; i < accessor.getWriters().length; i++) {
            INBTSerializable<Tag> writer = accessor.getWriters()[i];
            String name = accessor.getNames()[i];
            if (writer instanceof IMergeableNBTSerializable mergeable) {
                mergeable.prepareForComparisonWith(map.get(name));
            }
        }
    }

    @Inject(at = @At(value = "INVOKE",
                     target = "Lnet/minecraftforge/common/capabilities/CapabilityDispatcher;serializeNBT()Lnet/minecraft/nbt/CompoundTag;",
                     ordinal = 0),
            method = "areCompatible")
    private void gtceu$areCompatible(CapabilityDispatcher other, CallbackInfoReturnable<Boolean> cir) {
        gtceu$beforeComparison(this, other);
        if (other != null) gtceu$beforeComparison(other, this);
    }
}
