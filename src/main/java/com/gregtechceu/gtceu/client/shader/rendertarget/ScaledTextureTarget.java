package com.gregtechceu.gtceu.client.shader.rendertarget;

import com.mojang.blaze3d.pipeline.TextureTarget;

public class ScaledTextureTarget extends TextureTarget {

    private final float widthScale;
    private final float heightScale;
    private final boolean isInit;

    public ScaledTextureTarget(float widthScale, float heightScale, int width, int height,
                               boolean useDepth, boolean clearError) {
        super(width, height, useDepth, clearError);
        this.widthScale = widthScale;
        this.heightScale = heightScale;
        this.isInit = true;
        this.resize(width, height, clearError);
    }

    @Override
    public void resize(int width, int height, boolean clearError) {
        int renderWidth = width;
        int renderHeight = height;
        if (isInit) {
            renderWidth *= widthScale;
            renderHeight *= heightScale;
        }
        super.resize(renderWidth, renderHeight, clearError);

        // set the screen width/height back to the actual values
        this.viewWidth = width;
        this.viewHeight = height;
    }
}
