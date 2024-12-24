package com.gregtechceu.gtceu.core.mixins.ui.mixin;

import com.gregtechceu.gtceu.api.ui.util.pond.UIAbstractContainerMenuExtension;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {

    @SuppressWarnings("ConstantConditions")
    @Inject(method = "initMenu", at = @At("HEAD"))
    private void attachScreenHandler(AbstractContainerMenu menu, CallbackInfo ci) {
        ((UIAbstractContainerMenuExtension) menu).gtceu$attachToPlayer((ServerPlayer) (Object) this);
    }
}
