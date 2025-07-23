package com.gregtechceu.gtceu.client.renderer.pipe;

import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.block.PipeBlock;
import com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity.PipeBlockEntity;
import com.gregtechceu.gtceu.client.renderer.pipe.cache.ActivableSQC;
import com.gregtechceu.gtceu.client.renderer.pipe.cache.StructureQuadCache;
import com.gregtechceu.gtceu.client.renderer.pipe.quad.PipeQuadHelper;
import com.gregtechceu.gtceu.client.renderer.pipe.util.ActivableCacheKey;
import com.gregtechceu.gtceu.client.renderer.pipe.util.ColorData;
import com.gregtechceu.gtceu.client.renderer.pipe.util.SpriteInformation;
import com.gregtechceu.gtceu.client.renderer.pipe.util.TextureInformation;
import com.gregtechceu.gtceu.client.util.ModelUtils;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.utils.GTMath;

import com.lowdragmc.lowdraglib.client.bakedpipeline.Quad;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

public class ActivablePipeModel extends AbstractPipeModel<ActivableCacheKey> {

    private final TextureInformation inTex;
    private final TextureInformation sideTex;
    private final TextureInformation overlayTex;
    private final TextureInformation overlayActiveTex;

    private SpriteInformation inSprite;
    private SpriteInformation sideSprite;
    private SpriteInformation overlaySprite;
    private SpriteInformation overlayActiveSprite;

    private final boolean emissiveActive;

    public ActivablePipeModel(@NotNull TextureInformation inTex,
                              @NotNull TextureInformation sideTex,
                              @NotNull TextureInformation overlayTex,
                              @NotNull TextureInformation overlayActiveTex, boolean emissiveActive) {
        this.inTex = inTex;
        this.sideTex = sideTex;
        this.overlayTex = overlayTex;
        this.overlayActiveTex = overlayActiveTex;
        this.emissiveActive = emissiveActive;

        ModelUtils.registerAtlasStitchedEventListener(false, InventoryMenu.BLOCK_ATLAS, event -> {
            var atlas = event.getAtlas();

            inSprite = new SpriteInformation(atlas.getSprite(inTex.texture()), inTex.colorID());
            sideSprite = new SpriteInformation(atlas.getSprite(sideTex.texture()), sideTex.colorID());
            overlaySprite = new SpriteInformation(atlas.getSprite(overlayTex.texture()), overlayTex.colorID());
            overlayActiveSprite = new SpriteInformation(atlas.getSprite(overlayActiveTex.texture()),
                    overlayActiveTex.colorID());
        });
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public @NotNull List<BakedQuad> getQuads(ActivableCacheKey key, @Nullable BlockAndTintGetter level,
                                             @Nullable BlockPos pos, @Nullable Direction side,
                                             byte connectionMask, byte closedMask, byte blockedMask, byte frameMask,
                                             byte coverMask, @NotNull Material frameMaterial, ColorData data,
                                             RandomSource randomSource, ModelData modelData, RenderType renderType) {
        List<BakedQuad> quads = super.getQuads(key, level, pos, side,
                connectionMask, closedMask, blockedMask, frameMask, coverMask,
                frameMaterial, data, randomSource, modelData, renderType);

        if (key.isActive() && allowActive()) {
            if (emissiveActive) {
                ((ActivableSQC) pipeCache.get(key)).addOverlay(quads, connectionMask, data, true);
                // TODO bake this into the original quads
                quads = quads.stream()
                        .map(quad -> Quad.from(quad).setLight(15, 15).rebake())
                        .collect(Collectors.toList());
            }
            ((ActivableSQC) pipeCache.get(key)).addOverlay(quads, connectionMask, data, true);
        } else {
            ((ActivableSQC) pipeCache.get(key)).addOverlay(quads, connectionMask, data, false);
        }
        return quads;
    }

    @Override
    protected @NotNull ActivableCacheKey toKey(@NotNull ModelData state) {
        return ActivableCacheKey.of(
                GTMath.safeFloat(state.get(PipeRenderProperties.THICKNESS_PROPERTY)),
                GTMath.safeBool(state.get(PipeRenderProperties.ACTIVE_PROPERTY)));
    }

    @Override
    public SpriteInformation getParticleSprite(@NotNull Material material) {
        return sideSprite;
    }

    @Override
    protected StructureQuadCache constructForKey(ActivableCacheKey key) {
        return ActivableSQC.create(PipeQuadHelper.create(key.getThickness()), inSprite, sideSprite,
                overlaySprite, overlayActiveSprite);
    }

    public boolean allowActive() {
        return !ConfigHolder.INSTANCE.client.preventAnimatedCables;
    }

    @Override
    protected @Nullable PipeItemModel<ActivableCacheKey> getItemModel(PipeModelRedirector redirector,
                                                                      @NotNull ItemStack stack, ClientLevel world,
                                                                      LivingEntity entity) {
        PipeBlock block = PipeBlock.getBlockFromItem(stack);
        if (block == null) return null;
        return new PipeItemModel<>(redirector, this,
                new ActivableCacheKey(block.getStructure().getRenderThickness(), false),
                new ColorData(PipeBlockEntity.DEFAULT_COLOR));
    }
}
