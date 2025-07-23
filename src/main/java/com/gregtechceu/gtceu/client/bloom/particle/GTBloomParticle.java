package com.gregtechceu.gtceu.client.bloom.particle;

import com.gregtechceu.gtceu.client.bloom.BloomAlgorithm;
import com.gregtechceu.gtceu.client.bloom.BloomUtil;
import com.gregtechceu.gtceu.client.bloom.IBloomEffect;
import com.gregtechceu.gtceu.client.bloom.IRenderSetup;
import com.gregtechceu.gtceu.client.particle.GTParticle;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class GTBloomParticle extends GTParticle implements IBloomEffect {

    public GTBloomParticle(double posX, double posY, double posZ) {
        super(posX, posY, posZ);
        BloomUtil.registerBloomRender(getBloomRenderSetup(), getBloomType(), this, this);
    }

    @Nullable
    protected abstract IRenderSetup getBloomRenderSetup();

    @NotNull
    protected abstract BloomAlgorithm getBloomType();
}
