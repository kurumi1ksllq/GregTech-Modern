package com.gregtechceu.gtceu.client.renderer.cover;

import com.gregtechceu.gtceu.client.renderer.pipe.util.ColorData;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.data.ModelData;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

@FunctionalInterface
public interface CoverRenderer {

    @OnlyIn(Dist.CLIENT)
    void addQuads(List<BakedQuad> quads, @Nullable Direction renderSide,
                  @NotNull Direction attachedSide, @Nullable BlockAndTintGetter level, @Nullable BlockPos pos,
                  EnumSet<Direction> renderPlate, boolean renderBackside,
                  RandomSource rand, ModelData modelData, ColorData colorData, RenderType renderType);
}
