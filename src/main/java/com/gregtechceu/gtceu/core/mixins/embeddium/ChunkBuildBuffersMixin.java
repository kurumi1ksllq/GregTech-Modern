package com.gregtechceu.gtceu.core.mixins.embeddium;

import com.gregtechceu.gtceu.integration.embeddium.GTEmbeddiumCompat;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChunkBuildBuffers.class, remap = false)
public class ChunkBuildBuffersMixin {

    @Shadow @Final private Reference2ReferenceOpenHashMap<TerrainRenderPass, BakedChunkModelBuilder> builders;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void gtceu$addBloomChunkBuffer(ChunkVertexType vertexType, CallbackInfo ci) {
        TerrainRenderPass pass = GTEmbeddiumCompat.BLOOM_PASS;

        ChunkMeshBufferBuilder[] vertexBuffers = new ChunkMeshBufferBuilder[ModelQuadFacing.COUNT];
        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            vertexBuffers[facing] = new ChunkMeshBufferBuilder(vertexType, 128 * 1024,
                    pass.isSorted() && facing == ModelQuadFacing.UNASSIGNED.ordinal());
        }

        this.builders.put(pass, new BakedChunkModelBuilder(vertexBuffers, !pass.isSorted()));
    }
}
