package com.gregtechceu.gtceu.integration.embeddium.renderer;

import com.gregtechceu.gtceu.client.bloom.BloomEffectUtil;
import com.gregtechceu.gtceu.client.shader.GTShaders;
import com.gregtechceu.gtceu.integration.embeddium.CopyingVertexConsumer;
import com.gregtechceu.gtceu.integration.embeddium.GTEmbeddiumCompat;

import net.minecraft.core.BlockPos;

import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.api.MeshAppender;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class BloomMeshAppender implements MeshAppender {

    public static BloomMeshAppender INSTANCE = new BloomMeshAppender();

    @Override
    public void render(Context context) {
        if (!GTShaders.allowedShader()) {
            return;
        }

        BlockPos chunkOrigin = context.sectionOrigin().origin();
        Vector3fc originVec = new Vector3f(chunkOrigin.getX(), chunkOrigin.getY(), chunkOrigin.getZ());

        BloomEffectUtil.CURRENT_RENDERING_CHUNK_POS.set(chunkOrigin);
        BloomEffectUtil.bakeBloomChunkBuffers(chunkOrigin);

        var consumer = CopyingVertexConsumer.copyFrom(GTShaders.BLOOM_BUFFER_BUILDERS.get(chunkOrigin));
        ChunkModelBuilder chunkBuilder = context.sodiumBuildBuffers().get(GTEmbeddiumCompat.BLOOM_PASS);
        consumer.flush(chunkBuilder, GTEmbeddiumCompat.BLOOM_MATERIAL, originVec);
    }
}
