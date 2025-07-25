package com.gregtechceu.gtceu.client.shader.rendertarget;

import com.mojang.blaze3d.pipeline.TextureTarget;

public class ScaledTextureTarget extends TextureTarget {

    private final float widthScale;
    private final float heightScale;
    private final boolean isInit;

    public ScaledTextureTarget(float widthScale, float heightScale, int width, int height,
                               boolean useDepth, boolean clearError) {
        super((int) (width * widthScale), (int) (height * heightScale), useDepth, clearError);
        this.widthScale = widthScale;
        this.heightScale = heightScale;
        this.isInit = true;
        this.resize(width, height, clearError);
    }

    @Override
    public void resize(int width, int height, boolean clearError) {
        if (!isInit) {
            super.resize(width, height, clearError);
            return;
        }
        super.resize(Math.max((int) (width * widthScale), 1), Math.max((int) (height * heightScale), 1), clearError);
    }
}
