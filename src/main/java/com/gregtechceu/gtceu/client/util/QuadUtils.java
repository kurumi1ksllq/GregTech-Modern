package com.gregtechceu.gtceu.client.util;

import com.lowdragmc.lowdraglib.client.bakedpipeline.Submap;
import com.lowdragmc.lowdraglib.client.model.custommodel.Connections;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IQuadTransformer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.gregtechceu.gtceu.client.util.ModelUtils.*;

public class QuadUtils {

    public static Vector2f[] getQuadUVs(int[] vertices) {
        Vector2f[] uvs = new Vector2f[4];

        for (int i = 0; i < 4; i++) {
            int offset = i * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
            float u = Float.intBitsToFloat(vertices[offset]);
            float v = Float.intBitsToFloat(vertices[offset + 1]);
            uvs[i] = new Vector2f(u, v);
        }
        return uvs;
    }

    public static Vector3f[] getQuadVertices(int[] vertices) {
        Vector3f[] vertPos = new Vector3f[4];

        for (int i = 0; i < 4; i++) {
            int offset = i * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            vertPos[i] = new Vector3f(x, y, z);
        }
        return vertPos;
    }

    public static QuadInfo[] subdivide(BakedQuad baked) {
        Vector3f[] vertPos = getQuadVertices(baked.getVertices());
        Vector2f[] uvs = getQuadUVs(baked.getVertices());
        var maxUVs = findMinMaxUVs(uvs);
        QuadInfo quad = new QuadInfo(baked.getSprite(), baked.getTintIndex(), baked.getDirection(),
                baked.isShade(), baked.hasAmbientOcclusion(),
                vertPos, uvs, maxUVs.first(), maxUVs.second());

        return quad.subdivide();
    }

    private static void putVertexData(int[] vertices, int index, Vector3f pos, Vector2f uv) {
        int posOffset = index * IQuadTransformer.STRIDE + IQuadTransformer.POSITION;
        vertices[posOffset] = Float.floatToRawIntBits(pos.x());
        vertices[posOffset + 1] = Float.floatToRawIntBits(pos.y());
        vertices[posOffset + 2] = Float.floatToRawIntBits(pos.z());

        int uvOffset = index * IQuadTransformer.STRIDE + IQuadTransformer.UV0;
        vertices[uvOffset] = Float.floatToRawIntBits(uv.x());
        vertices[uvOffset + 1] = Float.floatToRawIntBits(uv.y());
    }

    public static Vector2f[] normalizeUVs(Vector2f min, Vector2f max, Vector2f... uvs) {
        Vector2f[] ret = new Vector2f[uvs.length];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = normalizeUV(min, max, uvs[i]);
        }
        return ret;
    }

    public static Vector2f normalizeUV(TextureAtlasSprite sprite, Vector2f vec) {
        return normalizeUV(
                new Vector2f(sprite.getU0(), sprite.getU1()),
                new Vector2f(sprite.getV0(), sprite.getV1()),
                vec);
    }

    public static Vector2f normalizeUV(Vector2f min, Vector2f max, Vector2f vec) {
        return new Vector2f(
                Mth.inverseLerp(vec.x(), min.x(), max.x()),
                Mth.inverseLerp(vec.y(), min.y(), max.y()));
    }

    public static Vector2f[] relativizeUVs(TextureAtlasSprite sprite, Vector2f... uvs) {
        for (int i = 0; i < uvs.length; i++) {
            uvs[i] = relativizeUV(sprite, uvs[i]);
        }
        return uvs;
    }

    public static Vector2f relativizeUV(TextureAtlasSprite sprite, Vector2f vec) {
        return new Vector2f(
                Mth.lerp(vec.x(), sprite.getU0(), sprite.getU1()),
                Mth.lerp(vec.y(), sprite.getV0(), sprite.getV1()));
    }

    public static List<BakedQuad> buildCTMQuads(List<BakedQuad> quads, BlockAndTintGetter level, BlockPos pos,
                                                @NotNull BlockState state, @Nullable Direction elementSide) {
        return buildCTMQuads(Connections.checkConnections(level, pos, state, elementSide), quads);
    }

    public static List<BakedQuad> buildCTMQuads(Connections connections, List<BakedQuad> base) {
        List<BakedQuad> result = new ArrayList<>();
        for (BakedQuad originalQuad : base) {
            TextureAtlasSprite connection = CTM_SPRITE_CACHE.get(originalQuad.getSprite());
            if (connection == null) {
                result.add(originalQuad);
                continue;
            }
            GTQuadTransformers.derotate().processInPlace(originalQuad);

            QuadInfo[] subdivided = QuadUtils.subdivide(originalQuad);
            int[] ctm = connections.getSubmapIndices();

            for (int j = 0; j < subdivided.length; j++) {
                QuadInfo quad = subdivided[j];
                if (quad != null) {
                    int quadrant = quad.getNormalizedUVQuadrant();
                    Submap submap = quad.findSubmap(connections);
                    TextureAtlasSprite ctmSprite = ctm[quadrant] > 15 ? originalQuad.getSprite() : connection;
                    subdivided[j] = quad.grow().transformUVs(ctmSprite, submap);
                }
            }
            result.addAll(Arrays.stream(subdivided).filter(Objects::nonNull).map(QuadInfo::rebake).toList());
        }
        return result;
    }
}
