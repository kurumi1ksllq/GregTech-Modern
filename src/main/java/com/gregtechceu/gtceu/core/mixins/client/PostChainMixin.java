package com.gregtechceu.gtceu.core.mixins.client;

import com.gregtechceu.gtceu.client.shader.rendertarget.ScaledTextureTarget;

import net.minecraft.client.renderer.PostChain;
import net.minecraft.util.GsonHelper;

import com.google.gson.JsonObject;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.pipeline.TextureTarget;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PostChain.class)
public class PostChainMixin {

    @Unique
    private float gtceu$widthScale = -1;
    @Unique
    private float gtceu$heightScale = -1;
    @Unique
    private boolean gtceu$bilinear = false;

    @Inject(method = "parseTargetNode",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/client/renderer/PostChain;addTempTarget(Ljava/lang/String;II)V",
                     ordinal = 1))
    private void gtceu$scaleTargetSize(CallbackInfo ci, @Local JsonObject json, @Local String name) {
        if (json.has("scale")) {
            JsonObject scale = GsonHelper.getAsJsonObject(json, "scale");
            gtceu$widthScale = GsonHelper.getAsFloat(scale, "width", 1.0f);
            gtceu$heightScale = GsonHelper.getAsFloat(scale, "height", 1.0f);
        }
        gtceu$bilinear = GsonHelper.getAsBoolean(json, "bilinear");
    }

    @WrapOperation(method = "addTempTarget",
                   at = @At(value = "NEW", target = "com/mojang/blaze3d/pipeline/TextureTarget"))
    private TextureTarget gtceu$wrapScaledTextureTarget(int width, int height, boolean useDepth, boolean clearError,
                                                        Operation<TextureTarget> original) {
        TextureTarget target;

        if (gtceu$widthScale > 0 && gtceu$heightScale > 0) {
            target = new ScaledTextureTarget(gtceu$widthScale, gtceu$heightScale, width, height, useDepth, clearError);
            gtceu$widthScale = -1;
            gtceu$heightScale = -1;
        } else {
            target = original.call(width, height, useDepth, clearError);
        }
        if (gtceu$bilinear) {
            target.setFilterMode(GL11.GL_NEAREST);
            gtceu$bilinear = false;
        }

        return target;
    }
}
