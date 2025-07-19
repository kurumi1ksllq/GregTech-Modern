package com.gregtechceu.gtceu.integration.embeddium;

import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.integration.embeddium.renderer.BloomMeshAppender;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.parameters.AlphaCutoffParameter;
import org.embeddedt.embeddium.api.ChunkMeshEvent;

public class GTEmbeddiumCompat {

    public static final TerrainRenderPass BLOOM_PASS = new TerrainRenderPass(GTRenderTypes.getBloom(), true, false);
    public static final Material BLOOM_MATERIAL = new Material(BLOOM_PASS, AlphaCutoffParameter.ZERO, true);

    @SubscribeEvent
    public static void registerExtraChunkMeshers(ChunkMeshEvent event) {
        //event.addMeshAppender(BloomMeshAppender.INSTANCE);
    }
}
