package com.gregtechceu.gtceu.client.renderer.cover;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.GTValues;
import com.gregtechceu.gtceu.client.renderer.pipe.cache.ColorQuadCache;
import com.gregtechceu.gtceu.client.renderer.pipe.cache.SubListAddress;
import com.gregtechceu.gtceu.client.renderer.pipe.quad.QuadHelper;
import com.gregtechceu.gtceu.client.renderer.pipe.quad.UVMapper;
import com.gregtechceu.gtceu.client.renderer.pipe.util.ColorData;
import com.gregtechceu.gtceu.client.renderer.pipe.util.SpriteInformation;
import com.gregtechceu.gtceu.client.util.ModelUtils;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.memoization.GTMemoizer;
import com.gregtechceu.gtceu.utils.memoization.MemoizedSupplier;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.Tolerate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
@Accessors(chain = true)
public class CoverRendererBuilder {

    public static final MemoizedSupplier<ColorQuadCache[]> PLATE_QUADS = GTMemoizer.memoize(() -> {
        ColorQuadCache[] plateQuads = new ColorQuadCache[GTValues.TIER_COUNT];
        for (int i = 0; i < GTValues.TIER_COUNT; i++) {
            plateQuads[i] = buildPlates(new SpriteInformation(plateSprite(i), 0));
        }
        return plateQuads;
    });
    private static final EnumMap<Direction, SubListAddress> PLATE_COORDS = new EnumMap<>(Direction.class);

    protected static final UVMapper defaultMapper = UVMapper.standard(0);

    private static @NotNull TextureAtlasSprite plateSprite(int tier) {
        return ModelUtils.getBlockSprite(
                GTCEu.id("block/casings/voltage/%s/side".formatted(GTValues.VN[tier].toLowerCase(Locale.ROOT))));
    }

    public static ColorQuadCache buildPlates(SpriteInformation sprite) {
        List<BakedQuad> quads = new ObjectArrayList<>();
        for (Direction facing : GTUtil.DIRECTIONS) {
            PLATE_COORDS.put(facing, buildPlates(quads, facing, sprite));
        }
        return new ColorQuadCache(quads);
    }

    protected static SubListAddress buildPlates(List<BakedQuad> quads, Direction facing,
                                                SpriteInformation sprite) {
        int start = quads.size();
        Pair<Vector3f, Vector3f> box = CoverRendererValues.PLATE_BOXES.get(facing);
        for (Direction dir : GTUtil.DIRECTIONS) {
            quads.add(QuadHelper.buildQuad(dir, box, CoverRendererBuilder.defaultMapper, sprite));
        }
        return new SubListAddress(start, quads.size());
    }

    protected static void addPlates(List<BakedQuad> quads, List<BakedQuad> plateQuads,
                                    EnumSet<Direction> plates, @Nullable Direction renderSide) {
        if (renderSide != null) {
            if (plates.contains(renderSide)) {
                quads.add(plateQuads.get(renderSide.ordinal()));
            }
        } else {
            for (Direction facing : plates) {
                quads.add(plateQuads.get(facing.ordinal()));
            }
        }
    }

    protected final @Nullable ResourceLocation texture;
    protected final @Nullable ResourceLocation textureEmissive;

    protected TextureAtlasSprite sprite;
    protected TextureAtlasSprite spriteEmissive;

    @Setter
    protected @NotNull UVMapper mapper = defaultMapper;
    @Setter
    protected @NotNull UVMapper mapperEmissive = defaultMapper;

    @Setter
    protected Supplier<ColorQuadCache> plateQuads = getPlateQuadCache(GTValues.LV);

    public CoverRendererBuilder(@Nullable ResourceLocation texture) {
        this(texture, null);
    }

    public CoverRendererBuilder(@Nullable ResourceLocation texture, @Nullable ResourceLocation textureEmissive) {
        this.texture = texture;
        this.textureEmissive = textureEmissive;

        ModelUtils.registerAtlasStitchedEventListener(false, InventoryMenu.BLOCK_ATLAS, event -> {
            var atlas = event.getAtlas();

            if (texture != null) {
                sprite = atlas.getSprite(texture);
            }
            if (textureEmissive != null) {
                spriteEmissive = atlas.getSprite(textureEmissive);
            } else if (texture != null) {
                ResourceLocation emissiveTex = texture.withSuffix("_emissive");
                if (atlas.getTextureLocations().contains(emissiveTex)) {
                    spriteEmissive = atlas.getSprite(emissiveTex);
                }
            }
        });
    }

    @Tolerate
    public CoverRendererBuilder setPlateQuads(int tier) {
        this.plateQuads = getPlateQuadCache(tier);
        return this;
    }

    public static Supplier<ColorQuadCache> getPlateQuadCache(int tier) {
        return () -> PLATE_QUADS.get()[tier];
    }

    protected static List<BakedQuad> getPlates(Direction facing, ColorData data, ColorQuadCache plateQuads) {
        return PLATE_COORDS.get(facing).getSublist(plateQuads.getQuads(data));
    }

    public final Supplier<CoverRenderer> build() {
        return this::makeRenderer;
    }

    protected CoverRenderer makeRenderer() {
        EnumMap<Direction, Pair<BakedQuad, BakedQuad>> spriteQuads = texture != null ?
                new EnumMap<>(Direction.class) : null;
        EnumMap<Direction, Pair<BakedQuad, BakedQuad>> spriteEmissiveQuads = textureEmissive != null ?
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
        }

        return (quads, renderSide, attachedSide, level, pos, renderPlate, renderBackside, rand, modelData, colorData,
                renderType) -> {
            addPlates(quads, getPlates(attachedSide, colorData, plateQuads.get()), renderPlate, renderSide);
            if (renderSide == null || renderSide == attachedSide) {
                if (spriteQuads != null) {
                    quads.add(spriteQuads.get(attachedSide).getLeft());
                    if (renderBackside) quads.add(spriteQuads.get(attachedSide).getRight());
                }
                if (spriteEmissiveQuads != null) {
                    quads.add(spriteEmissiveQuads.get(attachedSide).getLeft());
                    if (renderBackside) quads.add(spriteEmissiveQuads.get(attachedSide).getRight());
                }
            }
        };
    }
}
