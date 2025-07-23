package com.gregtechceu.gtceu.integration.embeddium;

import com.gregtechceu.gtceu.client.bloom.BloomUtil;
import com.gregtechceu.gtceu.core.mixins.embeddium.SodiumWorldRendererAccessor;
import com.gregtechceu.gtceu.integration.embeddium.renderer.BloomMeshAppender;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.util.iterator.ByteIterator;
import org.embeddedt.embeddium.api.ChunkMeshEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class GTEmbeddiumCompat {

    public static List<BlockPos> getVisibleRenderSections(Vec3 camPos) {
        SodiumWorldRenderer renderer = SodiumWorldRenderer.instanceNullable();
        if (renderer == null) {
            return Collections.emptyList();
        }
        List<BlockPos> list = new ArrayList<>();

        int camSectionX = SectionPos.blockToSectionCoord(camPos.x);
        int camSectionY = SectionPos.blockToSectionCoord(camPos.y);
        int camSectionZ = SectionPos.blockToSectionCoord(camPos.z);

        RenderSectionManager sectionManager = ((SodiumWorldRendererAccessor) renderer).gtceu$getRenderSectionManager();

        for (Iterator<ChunkRenderList> it = sectionManager.getRenderLists().iterator(); it.hasNext();) {
            ChunkRenderList entry = it.next();
            RenderRegion region = entry.getRegion();
            ByteIterator sectionIterator = entry.sectionsWithGeometryIterator(false);
            if (sectionIterator == null) {
                continue;
            }
            while (sectionIterator.hasNext()) {
                var section = region.getSection(sectionIterator.nextByteAsInt());

                if (section == null || !section.isBuilt()) {
                    // Nonexistent/unbuilt sections are not relevant
                    continue;
                }

                double dx = camPos.x - section.lastCameraX;
                double dy = camPos.y - section.lastCameraY;
                double dz = camPos.z - section.lastCameraZ;
                double camDelta = (dx * dx) + (dy * dy) + (dz * dz);

                if (camDelta < 1) {
                    // Didn't move enough, ignore
                    continue;
                }

                boolean cameraChangedSection = camSectionX != SectionPos.blockToSectionCoord(section.lastCameraX) ||
                        camSectionY != SectionPos.blockToSectionCoord(section.lastCameraY) ||
                        camSectionZ != SectionPos.blockToSectionCoord(section.lastCameraZ);

                BlockPos pos = SectionPos.of(camSectionX, camSectionY, camSectionZ).origin();
                if (!BloomUtil.BLOOM_BUFFERS.containsKey(pos) || !BloomUtil.BLOOM_BUFFER_SORT_STATES.containsKey(pos)) {
                    continue;
                }

                if (cameraChangedSection || section.isAlignedWithSectionOnGrid(camSectionX, camSectionY, camSectionZ)) {
                    list.add(pos);
                }
            }
        }
        return list;
    }

    @SubscribeEvent
    public static void registerChunkMeshAppenders(ChunkMeshEvent event) {
        event.addMeshAppender(BloomMeshAppender.INSTANCE);
    }
}
