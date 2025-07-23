package com.gregtechceu.gtceu.client.renderer.pipe;

import com.gregtechceu.gtceu.client.model.BaseBakedModel;
import com.gregtechceu.gtceu.client.renderer.pipe.util.CacheKey;
import com.gregtechceu.gtceu.client.renderer.pipe.util.ColorData;
import com.gregtechceu.gtceu.common.data.GTMaterials;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class PipeItemModel<K extends CacheKey> extends BaseBakedModel {

    private final PipeModelRedirector redirector;
    private final AbstractPipeModel<K> basis;
    private final K key;
    private final ColorData data;

    public PipeItemModel(PipeModelRedirector redirector, AbstractPipeModel<K> basis, K key, ColorData data) {
        this.redirector = redirector;
        this.basis = basis;
        this.key = key;
        this.data = data;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side,
                                             @NotNull RandomSource rand, @NotNull ModelData modelData,
                                             @Nullable RenderType renderType) {
        byte z = 0;
        return basis.getQuads(key, null, null, side,
                (byte) 0b1100, z, z, z, z,
                GTMaterials.NULL, data, rand, modelData, renderType);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return redirector.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return redirector.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return redirector.usesBlockLight();
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext transformType, @NotNull PoseStack poseStack,
                                              boolean applyLeftHandTransform) {
        redirector.applyTransform(transformType, poseStack, applyLeftHandTransform);
        // poseStack.mulPoseMatrix(CAMERA_TRANSFORMS.get(transformType));
        return this;
    }

    public static Matrix4f mul(@Nullable Vector3f translation, @Nullable Quaternionf leftRot, @Nullable Vector3f scale,
                               @Nullable Quaternionf rightRot) {
        Matrix4f res = new Matrix4f(), t = new Matrix4f();
        res.identity();
        if (leftRot != null) {
            t.set(leftRot);
            res.mul(t);
        }
        if (scale != null) {
            t.identity();
            t.m00(scale.x);
            t.m11(scale.y);
            t.m22(scale.z);
            res.mul(t);
        }
        if (rightRot != null) {
            t.set(rightRot);
            res.mul(t);
        }
        if (translation != null) res.setTranslation(translation);
        return res;
    }
}
