package com.gregtechceu.gtceu.client.bloom.particle;

import com.gregtechceu.gtceu.client.particle.GTParticle;
import com.gregtechceu.gtceu.client.bloom.IRenderSetup;
import com.gregtechceu.gtceu.client.bloom.shader.BloomType;
import com.gregtechceu.gtceu.client.bloom.BloomEffectUtil;
import com.gregtechceu.gtceu.client.bloom.IBloomEffect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class GTBloomParticle extends GTParticle implements IBloomEffect {

    public GTBloomParticle(double posX, double posY, double posZ) {
        super(posX, posY, posZ);
        BloomEffectUtil.registerBloomRender(getBloomRenderSetup(), getBloomType(), this, this);
    }

    @Nullable
    protected abstract IRenderSetup getBloomRenderSetup();

    @NotNull
    protected abstract BloomType getBloomType();
}
