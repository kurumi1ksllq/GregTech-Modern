package com.gregtechceu.gtceu.core.mixins.early_tag_load;

import com.gregtechceu.gtceu.core.ITagLoaderExtension;
import com.gregtechceu.gtceu.core.ITagManagerExtension;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagManager;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Mixin(TagManager.class)
public class TagManagerMixin implements ITagManagerExtension {

    @Shadow
    @Final
    private RegistryAccess registryAccess;

    @Unique
    private ReloadableServerResources gtceu$resources;

    @Inject(method = "createLoader",
            at = @At(
                     value = "INVOKE",
                     target = "Lnet/minecraft/tags/TagLoader;<init>(Ljava/util/function/Function;Ljava/lang/String;)V",
                     shift = At.Shift.BY,
                     by = 2))
    private <T> void gtceu$saveRegistryToTagLoader(ResourceManager rm, Executor executor,
                                                   RegistryAccess.RegistryEntry<T> reg,
                                                   CallbackInfoReturnable<CompletableFuture<TagManager.LoadResult<T>>> cir,
                                                   @Local Registry<T> registry,
                                                   @Local TagLoader<Holder<T>> loader) {
        ((ITagLoaderExtension) loader).gtceu$setRegistry(registry);
    }

    @Inject(method = "lambda$reload$2", at = @At("RETURN"))
    private void gtceu$earlyRunRegistryTagUpdate(CallbackInfo ci) {
        gtceu$resources.updateRegistryTags(this.registryAccess);
    }

    @Override
    public void gtceu$setResources(ReloadableServerResources resources) {
        this.gtceu$resources = resources;
    }
}
