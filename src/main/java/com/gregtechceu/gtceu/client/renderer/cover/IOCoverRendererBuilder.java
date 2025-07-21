package com.gregtechceu.gtceu.client.renderer.cover;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.capability.recipe.IO;
import com.gregtechceu.gtceu.api.cover.IIOCover;
import com.gregtechceu.gtceu.client.renderer.pipe.cache.ColorQuadCache;
import com.gregtechceu.gtceu.client.renderer.pipe.quad.QuadHelper;
import com.gregtechceu.gtceu.client.renderer.pipe.quad.UVMapper;
import com.gregtechceu.gtceu.client.util.ModelUtils;
import com.gregtechceu.gtceu.utils.GTUtil;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

import lombok.Setter;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;

public class IOCoverRendererBuilder extends CoverRendererBuilder {

    public static final CoverRenderer PUMP_LIKE_COVER_RENDERER = new IOCoverRendererBuilder(
            GTCEu.id("block/cover/pump"), null,
            GTCEu.id("block/cover/pump_inverted"), null)
            .build();

    protected final @Nullable ResourceLocation textureInverted;
    protected final @Nullable ResourceLocation textureInvertedEmissive;

    protected TextureAtlasSprite spriteInverted;
    protected TextureAtlasSprite spriteInvertedEmissive;

    @Setter
    protected @NotNull UVMapper mapperInverted = defaultMapper;
    @Setter
    protected @NotNull UVMapper mapperInvertedEmissive = defaultMapper;

    public IOCoverRendererBuilder(@NotNull ResourceLocation texture,
                                  @Nullable ResourceLocation textureEmissive,
                                  @Nullable ResourceLocation textureInverted,
                                  @Nullable ResourceLocation textureInvertedEmissive) {
        super(texture, textureEmissive);
        this.textureInverted = textureInverted;
        this.textureInvertedEmissive = textureInvertedEmissive;

        ModelUtils.registerAtlasStitchedEventListener(false, InventoryMenu.BLOCK_ATLAS, event -> {
            var atlas = event.getAtlas();

            if (textureInverted != null) {
                spriteInverted = atlas.getSprite(textureInverted);
            }
            if (textureInvertedEmissive != null) {
                spriteInvertedEmissive = atlas.getSprite(textureInvertedEmissive);
            } else if (textureInverted != null) {
                ResourceLocation emissiveTex = textureInverted.withSuffix("_emissive");
                if (atlas.getTextureLocations().contains(emissiveTex)) {
                    spriteInvertedEmissive = atlas.getSprite(emissiveTex);
                }
            }
        });
    }

    @Override
    public @NotNull IOCoverRendererBuilder setMapper(@NotNull UVMapper mapper) {
        return (IOCoverRendererBuilder) super.setMapper(mapper);
    }

    @Override
    public @NotNull IOCoverRendererBuilder setMapperEmissive(@NotNull UVMapper mapperEmissive) {
        return (IOCoverRendererBuilder) super.setMapperEmissive(mapperEmissive);
    }

    @Override
    public @NotNull IOCoverRendererBuilder setPlateQuads(ColorQuadCache plateQuads) {
        return (IOCoverRendererBuilder) super.setPlateQuads(plateQuads);
    }

    @Override
    public CoverRenderer build() {
        EnumMap<Direction, Pair<BakedQuad, BakedQuad>> spriteQuads = texture != null ?
                new EnumMap<>(Direction.class) : null;
        EnumMap<Direction, Pair<BakedQuad, BakedQuad>> spriteEmissiveQuads = textureEmissive != null ?
                new EnumMap<>(Direction.class) : null;
        EnumMap<Direction, Pair<BakedQuad, BakedQuad>> spriteInvertedQuads = textureInverted != null ?
                new EnumMap<>(Direction.class) : null;
        EnumMap<Direction, Pair<BakedQuad, BakedQuad>> spriteInvertedEmissiveQuads = textureInvertedEmissive != null ?
                new EnumMap<>(Direction.class) : null;
        for (Direction facing : GTUtil.DIRECTIONS) {
            if (texture != null) spriteQuads.put(facing, ImmutablePair.of(
                    QuadHelper.buildQuad(facing, CoverRendererValues.OVERLAY_BOXES_1.get(facing), mapper, sprite),
                    QuadHelper.buildQuad(facing.getOpposite(), CoverRendererValues.OVERLAY_BOXES_1.get(facing), mapper,
                            sprite)));

            if (textureEmissive != null) spriteEmissiveQuads.put(facing, ImmutablePair.of(
                    QuadHelper.buildQuad(facing, CoverRendererValues.OVERLAY_BOXES_2.get(facing), mapperEmissive,
                            spriteEmissive),
                    QuadHelper.buildQuad(facing.getOpposite(), CoverRendererValues.OVERLAY_BOXES_2.get(facing),
                            mapperEmissive,
                            spriteEmissive)));

            if (textureInverted != null) spriteInvertedQuads.put(facing, ImmutablePair.of(
                    QuadHelper.buildQuad(facing, CoverRendererValues.OVERLAY_BOXES_2.get(facing), mapperInverted,
                            spriteInverted),
                    QuadHelper.buildQuad(facing.getOpposite(), CoverRendererValues.OVERLAY_BOXES_2.get(facing),
                            mapperInverted,
                            spriteInverted)));

            if (textureInvertedEmissive != null) spriteInvertedEmissiveQuads.put(facing, ImmutablePair.of(
                    QuadHelper.buildQuad(facing, CoverRendererValues.OVERLAY_BOXES_2.get(facing),
                            mapperInvertedEmissive,
                            spriteInvertedEmissive),
                    QuadHelper.buildQuad(facing.getOpposite(), CoverRendererValues.OVERLAY_BOXES_2.get(facing),
                            mapperInvertedEmissive,
                            spriteInvertedEmissive)));
        }

        return (quads, renderSide, attachedSide, level, pos, renderPlate, renderBackside, rand, modelData, colorData,
                renderType) -> {
            IO io = modelData.get(IIOCover.IO_PROPERTY);

            addPlates(quads, getPlates(attachedSide, colorData, plateQuads), renderPlate, renderSide);
            if (renderSide == null || renderSide == attachedSide) {
                boolean isInverted = io != null && io != IO.OUT;

                if (isInverted && spriteInvertedQuads != null) {
                    quads.add(spriteInvertedQuads.get(attachedSide).getLeft());
                    if (renderBackside) quads.add(spriteInvertedQuads.get(attachedSide).getRight());

                } else if (spriteQuads != null) {
                    quads.add(spriteQuads.get(attachedSide).getLeft());
                    if (renderBackside) quads.add(spriteQuads.get(attachedSide).getRight());
                }
                if (isInverted && spriteInvertedEmissiveQuads != null) {
                    quads.add(spriteInvertedEmissiveQuads.get(attachedSide).getLeft());
                    if (renderBackside) quads.add(spriteInvertedEmissiveQuads.get(attachedSide).getRight());
                } else if (spriteEmissiveQuads != null) {
                    quads.add(spriteEmissiveQuads.get(attachedSide).getLeft());
                    if (renderBackside) quads.add(spriteEmissiveQuads.get(attachedSide).getRight());
                }
            }
        };
    }
}
