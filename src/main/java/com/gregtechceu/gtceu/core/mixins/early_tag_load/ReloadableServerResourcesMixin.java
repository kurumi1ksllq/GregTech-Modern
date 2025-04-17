package com.gregtechceu.gtceu.core.mixins.early_tag_load;

import com.gregtechceu.gtceu.core.ITagManagerExtension;

import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.tags.TagManager;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ReloadableServerResources.class)
public class ReloadableServerResourcesMixin {

    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "net/minecraft/tags/TagManager"))
    public TagManager gtceu$setTagManagerResources(RegistryAccess registryAccess, Operation<TagManager> original) {
        TagManager manager = original.call(registryAccess);
        ((ITagManagerExtension) manager).gtceu$setResources((ReloadableServerResources) (Object) this);
        return manager;
    }
}
