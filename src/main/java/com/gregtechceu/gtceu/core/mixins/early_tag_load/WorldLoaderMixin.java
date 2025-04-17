package com.gregtechceu.gtceu.core.mixins.early_tag_load;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.WorldLoader;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WorldLoader.class)
public class WorldLoaderMixin {

    @WrapOperation(method = "lambda$load$1",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/server/ReloadableServerResources;updateRegistryTags(Lnet/minecraft/core/RegistryAccess;)V"))
    private static void gtceu$cancelTagUpdateWorldLoader(ReloadableServerResources instance,
                                                         RegistryAccess registryAccess, Operation<Void> original) {
        // no-op
    }
}
