package com.gregtechceu.gtceu.integration.embeddium;

import com.gregtechceu.gtceu.integration.embeddium.renderer.BloomMeshAppender;

import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.embeddedt.embeddium.api.ChunkMeshEvent;

public class GTEmbeddiumCompat {

    @SubscribeEvent
    public static void registerChunkMeshAppenders(ChunkMeshEvent event) {
        event.addMeshAppender(BloomMeshAppender.INSTANCE);
    }
}
