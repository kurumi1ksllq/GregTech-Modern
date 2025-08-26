package com.gregtechceu.gtceu.client.renderer.cover;

import com.gregtechceu.gtceu.api.cover.CoverBehavior;
import com.gregtechceu.gtceu.client.util.GTQuadTransformers;
import com.gregtechceu.gtceu.common.cover.MultiCover;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraftforge.client.model.IQuadTransformer;
import net.minecraftforge.client.model.data.ModelData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class MultiCoverRenderer implements ICoverRenderer {

    @Override
    public void renderCover(List<BakedQuad> quads, @Nullable Direction side, RandomSource rand,
                            @NotNull CoverBehavior coverBehavior, BlockPos pos, BlockAndTintGetter level,
                            @NotNull ModelData modelData, @Nullable RenderType renderType) {
        if (!(coverBehavior instanceof MultiCover multiCover)) return;
        IQuadTransformer quadTransformer = GTQuadTransformers.scale(1f / MultiCover.SLOTS_ROOT);
        List<CoverBehavior> covers = multiCover.getCovers();
        for (int i = 0; i < covers.size(); i++) {
            int x = i / MultiCover.SLOTS_ROOT;
            int y = i % MultiCover.SLOTS_ROOT;
            IQuadTransformer transformer = quadTransformer.andThen(GTQuadTransformers.offset(
                    (float) x / MultiCover.SLOTS_ROOT,
                    (float) y / MultiCover.SLOTS_ROOT,
                    0));
            CoverBehavior cover = covers.get(i);
            if (cover == null) continue;
            List<BakedQuad> coverQuads = new ArrayList<>();
            Supplier<ICoverRenderer> coverRendererSupplier = cover.getCoverRenderer();
            if (coverRendererSupplier != null)
                coverRendererSupplier.get().renderCover(coverQuads, side, rand, cover, pos, level, modelData,
                        renderType);
            for (BakedQuad quad : coverQuads) {
                quads.add(transformer.process(quad));
            }
        }
    }
}
