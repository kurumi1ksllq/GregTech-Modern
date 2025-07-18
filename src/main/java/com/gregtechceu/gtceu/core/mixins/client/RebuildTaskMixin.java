package com.gregtechceu.gtceu.core.mixins.client;

import com.gregtechceu.gtceu.client.bloom.BloomEffectUtil;

import net.minecraft.client.renderer.ChunkBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.core.BlockPos;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.renderer.chunk.ChunkRenderDispatcher$RenderChunk$RebuildTask")
public class RebuildTaskMixin {

    @Shadow(aliases = { "this$1", "f_290687_", "f" })
    @Final
    ChunkRenderDispatcher.RenderChunk this$1;

    @Inject(method = "compile", at = @At(value = "HEAD"))
    private void gtceu$startBloomBufferForChunk(float x, float y, float z,
                                                ChunkBufferBuilderPack chunkBufferBuilderPack,
                                                CallbackInfoReturnable<Object> cir) {
        BlockPos pos = this.this$1.getOrigin();
        BloomEffectUtil.CURRENT_RENDERING_CHUNK_POS.set(pos);
    }
}
