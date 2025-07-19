package com.gregtechceu.gtceu.core.mixins.client;

import net.minecraft.client.renderer.LevelRenderer;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LevelRenderer.class)
public interface LevelRendererAccessor {

    @Accessor("renderChunksInFrustum")
    ObjectArrayList<LevelRenderer.RenderChunkInfo> gtceu$getRenderChunksInFrustum();
}
