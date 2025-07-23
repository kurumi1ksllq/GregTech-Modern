package com.gregtechceu.gtceu.client.model;

import com.gregtechceu.gtceu.client.util.ModelUtils;

import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.IDynamicBakedModel;

import org.jetbrains.annotations.NotNull;

public abstract class BaseBakedModel implements IDynamicBakedModel {

    public BaseBakedModel() {}

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return ModelUtils.getBlockSprite(MissingTextureAtlasSprite.getLocation());
    }
}
