package com.gregtechceu.gtceu.core.mixins.ldlib;

import com.gregtechceu.gtceu.syncdata.map.ManagedMapRef;
import com.gregtechceu.gtceu.syncdata.map.MapAccessor;
import com.gregtechceu.gtceu.syncdata.map.ReadonlyMapRef;

import com.lowdragmc.lowdraglib.syncdata.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import com.lowdragmc.lowdraglib.syncdata.managed.ManagedField;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(value = ManagedKey.class, remap = false)
public class ManagedKeyMixin {

    @Shadow
    @Final
    private Field rawField;

    @Shadow
    @Final
    private boolean isLazy;

    @Inject(method = "createRef",
            at = @At(value = "INVOKE",
                     target = "Lcom/lowdragmc/lowdraglib/syncdata/IAccessor;isManaged()Z"),
            cancellable = true)
    private void gtceu$tryMapAccessor(Object instance, CallbackInfoReturnable<IRef> cir,
                                      @Local IAccessor accessor) {
        ManagedKey self = (ManagedKey) (Object) this;

        if (accessor instanceof MapAccessor mapAccessor) {
            if (accessor.isManaged() || mapAccessor.getKeyAccessor().isManaged()) {
                cir.setReturnValue(new ManagedMapRef(ManagedField.of(rawField, instance), isLazy).setKey(self));
                return;
            }
            try {
                rawField.setAccessible(true);
                cir.setReturnValue(new ReadonlyMapRef(isLazy, rawField.get(instance)).setKey(self));
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
