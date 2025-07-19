package com.gregtechceu.gtceu.integration.embeddium.renderer;

import com.gregtechceu.gtceu.client.bloom.BloomEffectUtil;
import com.gregtechceu.gtceu.client.shader.GTShaders;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import org.embeddedt.embeddium.api.MeshAppender;

public class BloomMeshAppender implements MeshAppender {

    public static BloomMeshAppender INSTANCE = new BloomMeshAppender();

    @Override
    public void render(Context context) {
        if (!GTShaders.allowedShader()) {
            return;
        }

        Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        BlockPos chunkOrigin = context.sectionOrigin().origin();
        BloomEffectUtil.CURRENT_RENDERING_CHUNK_POS.set(chunkOrigin);
        BloomEffectUtil.bakeBloomChunkBuffers(chunkOrigin, camPos);
    }
}
