package com.gregtechceu.gtceu.core.mixins.embeddium;

import com.gregtechceu.gtceu.client.model.BloomMetadataSection;
import com.gregtechceu.gtceu.client.shader.GTShaders;
import com.gregtechceu.gtceu.integration.embeddium.GTEmbeddiumCompat;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.LinkedList;
import java.util.List;

@Mixin(value = BlockRenderer.class, remap = false)
public abstract class BlockRendererMixin {

    @Shadow protected abstract QuadLightData getVertexLight(BlockRenderContext ctx, LightPipeline lighter,
                                                            Direction cullFace, BakedQuadView quad);

    @Shadow @Final private LightPipelineProvider lighters;

    @WrapOperation(method = "renderModel",
            at = @At(value = "INVOKE",
                    target = "Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderer;renderQuadList(Lme/jellysquid/mods/sodium/client/render/chunk/compile/pipeline/BlockRenderContext;Lme/jellysquid/mods/sodium/client/render/chunk/terrain/material/Material;Lme/jellysquid/mods/sodium/client/model/light/LightPipeline;Lme/jellysquid/mods/sodium/client/model/color/ColorProvider;Lnet/minecraft/world/phys/Vec3;Lme/jellysquid/mods/sodium/client/render/chunk/compile/buffers/ChunkModelBuilder;Ljava/util/List;Lnet/minecraft/core/Direction;)V"))
    private void gtceu$copyBloomQuads(BlockRenderer instance, BlockRenderContext ctx, Material material,
                                      LightPipeline lighter, ColorProvider<BlockState> colorizer,
                                      Vec3 offset, ChunkModelBuilder meshBuilder,
                                      List<BakedQuad> quads, Direction cullFace,
                                      Operation<Void> original,
                                      BlockRenderContext _ctx, ChunkBuildBuffers buffers) {
        original.call(instance, ctx, material, lighter, colorizer, offset, meshBuilder, quads, cullFace);
        if (!GTShaders.allowedShader()) {
            return;
        }

        // Check if quad is full brightness OR we have bloom enabled for the quad
        List<BakedQuad> emissiveQuads = new LinkedList<>();
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad quad = quads.get(i);
            QuadLightData light = this.getVertexLight(ctx,
                    quad.hasAmbientOcclusion() ? lighter : this.lighters.getLighter(LightMode.FLAT),
                    cullFace, (BakedQuadView) quad);
            if (BloomMetadataSection.isEmissive(quad, light.lm)) {
                emissiveQuads.add(quad);
            }
        }

        if (!emissiveQuads.isEmpty()) {
            // copy the emissive quads' vertex data to the bloom buffer
            ChunkModelBuilder bloomBuilder = buffers.get(GTEmbeddiumCompat.BLOOM_PASS);
            original.call(instance, ctx, material, lighter, colorizer, offset, bloomBuilder, emissiveQuads, cullFace);
        }
    }
}
