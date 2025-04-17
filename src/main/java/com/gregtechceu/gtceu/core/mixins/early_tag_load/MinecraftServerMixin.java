package com.gregtechceu.gtceu.core.mixins.early_tag_load;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ReloadableServerResources;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @WrapOperation(method = "lambda$reloadResources$26",
                   at = @At(value = "INVOKE",
                            target = "Lnet/minecraft/server/ReloadableServerResources;updateRegistryTags(Lnet/minecraft/core/RegistryAccess;)V"))
    private void gtceu$cancelTagUpdateServer(ReloadableServerResources instance,
                                             RegistryAccess registryAccess, Operation<Void> original) {
        // no-op
    }
}
