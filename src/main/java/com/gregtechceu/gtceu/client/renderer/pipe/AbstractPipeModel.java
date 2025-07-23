package com.gregtechceu.gtceu.client.renderer.pipe;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.client.renderer.cover.CoverRendererPackage;
import com.gregtechceu.gtceu.client.renderer.pipe.cache.StructureQuadCache;
import com.gregtechceu.gtceu.client.renderer.pipe.quad.PipeQuadHelper;
import com.gregtechceu.gtceu.client.renderer.pipe.util.CacheKey;
import com.gregtechceu.gtceu.client.renderer.pipe.util.ColorData;
import com.gregtechceu.gtceu.client.renderer.pipe.util.SpriteInformation;
import com.gregtechceu.gtceu.common.data.GTMaterialBlocks;
import com.gregtechceu.gtceu.common.data.GTMaterials;
import com.gregtechceu.gtceu.utils.GTMath;
import com.gregtechceu.gtceu.utils.GTUtil;
import com.gregtechceu.gtceu.utils.collections.WeakHashSet;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

import static com.gregtechceu.gtceu.api.machine.IMachineBlockEntity.*;

public abstract class AbstractPipeModel<K extends CacheKey> {

    protected final Object2ObjectOpenHashMap<K, StructureQuadCache> pipeCache;

    protected static final WeakHashSet<Object2ObjectOpenHashMap<? extends CacheKey, StructureQuadCache>> PIPE_CACHES = new WeakHashSet<>();

    public static void invalidateCaches() {
        for (var cache : PIPE_CACHES) {
            cache.clear();
            cache.trim(16);
        }
    }

    public AbstractPipeModel() {
        pipeCache = new Object2ObjectOpenHashMap<>();
        PIPE_CACHES.add(pipeCache);
    }

    @OnlyIn(Dist.CLIENT)
    public @NotNull List<BakedQuad> getQuads(@NotNull BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource rand, @NotNull ModelData modelData,
                                             @Nullable RenderType renderType) {
        ColorData colorData = computeColorData(modelData);
        CoverRendererPackage rendererPackage = modelData.get(CoverRendererPackage.PROPERTY);
        byte coverMask = rendererPackage == null ? 0 : rendererPackage.getMask();
        BlockAndTintGetter level = modelData.get(MODEL_DATA_LEVEL);
        BlockPos pos = modelData.get(MODEL_DATA_POS);

        List<BakedQuad> quads = getQuads(toKey(modelData), level, pos, side,
                GTMath.safeByte(modelData.get(PipeRenderProperties.CONNECTED_MASK_PROPERTY)),
                GTMath.safeByte(modelData.get(PipeRenderProperties.CLOSED_MASK_PROPERTY)),
                GTMath.safeByte(modelData.get(PipeRenderProperties.BLOCKED_MASK_PROPERTY)),
                GTMath.safeByte(modelData.get(PipeRenderProperties.FRAME_MASK_PROPERTY)),
                coverMask,
                Objects.requireNonNullElse(modelData.get(PipeRenderProperties.FRAME_MATERIAL_PROPERTY),
                        GTMaterials.NULL),
                colorData, rand, modelData, renderType);
        if (rendererPackage != null) renderCovers(quads, rendererPackage,
                side, level, pos, rand, modelData, renderType);
        return quads;
    }

    @OnlyIn(Dist.CLIENT)
    protected void renderCovers(List<BakedQuad> quads, @NotNull CoverRendererPackage rendererPackage,
                                @Nullable Direction side, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos,
                                RandomSource rand, @NotNull ModelData data, RenderType renderType) {
        int color = GTMath.safeInt(data.get(PipeRenderProperties.COLOR_PROPERTY));
        Material material = Objects.requireNonNullElse(data.get(PipeRenderProperties.MATERIAL_PROPERTY),
                GTMaterials.NULL);

        if (!material.isNull()) {
            int matColor = GTUtil.convertRGBtoARGB(material.getMaterialRGB());
            if (color == 0 || color == matColor) {
                // unpainted
                color = 0xFFFFFFFF;
            }
        }
        rendererPackage.addQuads(quads, level, pos, side, rand, data, new ColorData(color), renderType);
    }

    protected ColorData computeColorData(@NotNull ModelData data) {
        return new ColorData(GTMath.safeInt(data.get(PipeRenderProperties.COLOR_PROPERTY)));
    }

    @OnlyIn(Dist.CLIENT)
    public @NotNull List<BakedQuad> getQuads(K key, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos,
                                             @Nullable Direction side, byte connectionMask, byte closedMask,
                                             byte blockedMask, byte coverMask, byte frameMask,
                                             @NotNull Material frameMaterial, ColorData colorData,
                                             RandomSource rand, ModelData modelData, RenderType renderType) {
        List<BakedQuad> quads = new ObjectArrayList<>();

        StructureQuadCache cache = pipeCache.computeIfAbsent(key, this::constructForKey);
        cache.addToList(quads, colorData, connectionMask, closedMask, blockedMask, coverMask);

        if (frameMaterial.isNull() || (renderType != null && renderType != RenderType.translucent())) {
            return quads;
        }

        var frameBlock = GTMaterialBlocks.MATERIAL_BLOCKS.get(TagPrefix.frameGt, frameMaterial);
        if (frameBlock != null) {
            BlockState frameState = frameBlock.getDefaultState();
            BakedModel frameModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(frameState);

            PipeQuadHelper.createFrame(quads, frameModel, level, pos,
                    frameState, frameMask, side, rand, modelData, renderType);
        }
        return quads;
    }

    protected abstract @NotNull K toKey(@NotNull ModelData state);

    protected final @NotNull CacheKey defaultKey(@NotNull ModelData state) {
        return CacheKey.of(state.get(PipeRenderProperties.THICKNESS_PROPERTY));
    }

    @OnlyIn(Dist.CLIENT)
    protected abstract StructureQuadCache constructForKey(K key);

    @OnlyIn(Dist.CLIENT)
    public @Nullable TextureAtlasSprite getParticleTexture(int paintColor, @NotNull Material material) {
        SpriteInformation spriteInformation = getParticleSprite(material);
        return spriteInformation != null ? spriteInformation.sprite() : null;
    }

    @OnlyIn(Dist.CLIENT)
    public @Nullable TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        return getParticleTexture(GTMath.safeInt(data.get(PipeRenderProperties.COLOR_PROPERTY)),
                Objects.requireNonNullElse(data.get(PipeRenderProperties.MATERIAL_PROPERTY), GTMaterials.NULL));
    }

    public abstract SpriteInformation getParticleSprite(@NotNull Material material);

    @Nullable
    protected abstract PipeItemModel<K> getItemModel(PipeModelRedirector redirector, @NotNull ItemStack stack,
                                                     ClientLevel world, LivingEntity entity);

    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand,
                                             @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    }
}
