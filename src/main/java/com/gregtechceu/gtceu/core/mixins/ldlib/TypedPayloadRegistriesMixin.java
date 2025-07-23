package com.gregtechceu.gtceu.core.mixins.ldlib;

import com.gregtechceu.gtceu.syncdata.map.MapAccessor;

import com.lowdragmc.lowdraglib.syncdata.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.TypedPayloadRegistries;
import com.lowdragmc.lowdraglib.utils.ReflectionUtils;

import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;

@Mixin(value = TypedPayloadRegistries.class, remap = false)
public abstract class TypedPayloadRegistriesMixin {

    @Shadow
    public static IAccessor findByType(Type clazz) {
        throw new AssertionError();
    }

    @Inject(method = "findByType",
            at = @At(value = "INVOKE",
                     target = "Lcom/lowdragmc/lowdraglib/syncdata/TypedPayloadRegistries;findByClass(Ljava/lang/Class;)Lcom/lowdragmc/lowdraglib/syncdata/IAccessor;"),
            cancellable = true)
    private static void gtceu$tryMapAccessor(Type clazz, CallbackInfoReturnable<IAccessor> cir,
                                             @Local(ordinal = 0) Class<?> rawType) {
        if (Map.class.isAssignableFrom(rawType)) {
            Type[] typeArgs = ((ParameterizedType) clazz).getActualTypeArguments();
            Type keyType = typeArgs[0];
            var keyAccessor = findByType(keyType);
            var rawKeyType = ReflectionUtils.getRawType(keyType, Object.class);

            Type valType = typeArgs[1];
            var valAccessor = findByType(valType);
            var rawValType = ReflectionUtils.getRawType(valType, Object.class);

            cir.setReturnValue(MapAccessor.makeAccessor(keyAccessor, rawKeyType, valAccessor, rawValType));
        }
    }
}
