package com.gregtechceu.gtceu.core.mixins.embeddium;

import com.gregtechceu.gtceu.integration.embeddium.GTEmbeddiumCompat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.apache.commons.lang3.ArrayUtils;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = DefaultTerrainRenderPasses.class, remap = false)
public class DefaultTerrainRenderPassesMixin {

    @Shadow
    @Final
    @Mutable
    public static TerrainRenderPass[] ALL;

    /**
     * Very evil mixin to not have a million other mixins for enabling sorting on the bloom pass
     * @author screret
     * @reason AFAIK there's no way to actually add render passes to be processed automatically except this
     */
    @WrapOperation(method = "<clinit>",
            at = @At(value = "FIELD",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/terrain/DefaultTerrainRenderPasses;ALL:[Lme/jellysquid/mods/sodium/client/render/chunk/terrain/TerrainRenderPass;",
                    opcode = Opcodes.PUTSTATIC))
    private static void gtceu$addBloomRenderPass(TerrainRenderPass[] value, Operation<Void> setter) {
        value = ArrayUtils.add(value, GTEmbeddiumCompat.BLOOM_PASS);
        setter.call((Object) value);
    }
}
