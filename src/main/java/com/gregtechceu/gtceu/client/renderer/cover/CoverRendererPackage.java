package com.gregtechceu.gtceu.client.renderer.cover;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.renderer.pipe.util.ColorData;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

public final class CoverRendererPackage {

    @OnlyIn(Dist.CLIENT)
    public static final TextureAtlasSprite[] COVER_BACK_PLATE = new TextureAtlasSprite[1];

    @OnlyIn(Dist.CLIENT)
    public static void initSprites(TextureAtlas atlas) {
        COVER_BACK_PLATE[0] = atlas.getSprite(GTCEu.id("block/material_sets/dull/wire_side"));
    }

    public static final ModelProperty<CoverRendererPackage> PROPERTY = new ModelProperty<>();

    public static final CoverRendererPackage EMPTY = new CoverRendererPackage(false);

    private final EnumMap<Direction, CoverRenderer> renderers = new EnumMap<>(Direction.class);
    private final EnumMap<Direction, ModelData> modelDatas = new EnumMap<>(Direction.class);
    private final EnumSet<Direction> plates = EnumSet.allOf(Direction.class);

    private final boolean renderBackside;

    public CoverRendererPackage(boolean renderBackside) {
        this.renderBackside = renderBackside;
    }

    public void addRenderer(CoverRenderer renderer, @NotNull Direction facing) {
        renderers.put(facing, renderer);
        plates.remove(facing);
    }

    public void addModelData(@NotNull Direction facing, @NotNull ModelData modelData) {
        modelDatas.put(facing, modelData);
    }

    @OnlyIn(Dist.CLIENT)
    public void addQuads(List<BakedQuad> quads, @Nullable BlockAndTintGetter level,
                         @Nullable BlockPos pos, @Nullable Direction side,
                         RandomSource rand, ModelData modelData, ColorData data, RenderType renderType) {
        if (side != null) {
            CoverRenderer renderer = this.renderers.get(side);
            if (renderer != null) {
                ModelData coverModelData = modelDatas.get(side);
                if (coverModelData == null || coverModelData == ModelData.EMPTY) {
                    coverModelData = modelData;
                }

                EnumSet<Direction> plates = EnumSet.copyOf(this.plates);
                // force front and back plates to render
                plates.add(side);
                plates.add(side.getOpposite());
                renderer.addQuads(quads, side, side, level, pos, plates, renderBackside,
                        rand, coverModelData, data, renderType);
            }
            return;
        }

        for (var renderer : this.renderers.entrySet()) {
            EnumSet<Direction> plates = EnumSet.copyOf(this.plates);
            // force front and back plates to render
            plates.add(renderer.getKey());
            plates.add(renderer.getKey().getOpposite());
            renderer.getValue().addQuads(quads, null, renderer.getKey(), level, pos, plates, renderBackside,
                    rand, modelData, data, renderType);
        }
    }

    public byte getMask() {
        byte mask = 0;
        for (Direction facing : renderers.keySet()) {
            mask |= 1 << facing.ordinal();
        }
        return mask;
    }
}
