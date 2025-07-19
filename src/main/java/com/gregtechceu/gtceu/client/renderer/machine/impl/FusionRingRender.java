package com.gregtechceu.gtceu.client.renderer.machine.impl;

import com.gregtechceu.gtceu.api.pattern.util.RelativeDirection;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.client.bloom.IRenderSetup;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRender;
import com.gregtechceu.gtceu.client.renderer.machine.DynamicRenderType;
import com.gregtechceu.gtceu.client.bloom.shader.BloomEffect;
import com.gregtechceu.gtceu.client.bloom.shader.BloomAlgorithm;
import com.gregtechceu.gtceu.client.bloom.BloomEffectUtil;
import com.gregtechceu.gtceu.client.bloom.EffectRenderContext;
import com.gregtechceu.gtceu.client.bloom.IBloomEffect;
import com.gregtechceu.gtceu.client.util.RenderBufferHelper;
import com.gregtechceu.gtceu.common.machine.multiblock.electric.FusionReactorMachine;
import com.gregtechceu.gtceu.config.ConfigHolder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.Codec;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import static net.minecraft.util.FastColor.ARGB32.*;

public class FusionRingRender extends DynamicRender<FusionReactorMachine, FusionRingRender> {

    // spotless:off
    public static final Codec<FusionRingRender> CODEC = Codec.unit(FusionRingRender::new);
    public static final DynamicRenderType<FusionReactorMachine, FusionRingRender> TYPE = new DynamicRenderType<>(FusionRingRender.CODEC);
    // spotless:on

    public static final float FADEOUT = 60;

    protected float delta = 0;
    protected int lastColor = -1;

    public FusionRingRender() {}

    @Override
    public DynamicRenderType<FusionReactorMachine, FusionRingRender> getType() {
        return TYPE;
    }

    @Override
    public boolean shouldRender(FusionReactorMachine machine, Vec3 cameraPos) {
        return machine.recipeLogic.isWorking() || delta > 0;
    }

    @Override
    public void render(FusionReactorMachine machine, float partialTick,
                       PoseStack poseStack, MultiBufferSource buffer,
                       int packedLight, int packedOverlay) {
        if (!machine.recipeLogic.isWorking() && delta <= 0) {
            return;
        }

        if (!machine.isRegisteredBloomTicket()) {
            machine.setRegisteredBloomTicket(true);
            BloomEffectUtil.registerBloomRender(FusionBloomSetup.INSTANCE, getBloomType(),
                    new FusionBloomEffect(machine), machine.getHolder().self());
        }
        // TODO fix bloom on fusion reactor light
        renderLightRing(machine, partialTick, poseStack, buffer.getBuffer(GTRenderTypes.getLightRing()));
    }

    @OnlyIn(Dist.CLIENT)
    private void renderLightRing(FusionReactorMachine machine, float partialTicks, PoseStack stack,
                                 VertexConsumer buffer) {
        float alpha = 1f;
        if (machine.recipeLogic.isWorking()) {
            lastColor = machine.getColor();
            delta = FADEOUT;
        } else {
            alpha = delta / FADEOUT;
            lastColor = color(Mth.floor(alpha * 255), red(lastColor), green(lastColor), blue(lastColor));
            delta -= Minecraft.getInstance().getDeltaFrameTime();
        }

        final float lerpFactor = Math.abs((Math.abs(machine.getOffsetTimer() % 50) + partialTicks) - 25) / 25;
        Direction front = machine.getFrontFacing();
        Direction upwards = machine.getUpwardsFacing();
        boolean flipped = machine.isFlipped();
        Direction back = RelativeDirection.BACK.getRelative(front, upwards, flipped);
        Direction.Axis axis = RelativeDirection.UP.getRelative(front, upwards, flipped).getAxis();
        float r = Mth.lerp(lerpFactor, red(lastColor), 255) / 255f;
        float g = Mth.lerp(lerpFactor, green(lastColor), 255) / 255f;
        float b = Mth.lerp(lerpFactor, blue(lastColor), 255) / 255f;
        RenderBufferHelper.renderRing(stack, buffer,
                back.getStepX() * 7 + 0.5F,
                back.getStepY() * 7 + 0.5F,
                back.getStepZ() * 7 + 0.5F,
                6, 0.2F, 10, 20,
                r, g, b, alpha, axis);
    }

    @Override
    public boolean shouldRenderOffScreen(FusionReactorMachine machine) {
        return machine.recipeLogic.isWorking() || delta > 0;
    }

    @Override
    public AABB getRenderBoundingBox(FusionReactorMachine machine) {
        return new AABB(machine.getPos()).inflate(getViewDistance() / 2.0D);
    }

    private static BloomAlgorithm getBloomType() {
        var config = ConfigHolder.INSTANCE.client.shader.fusionBloom;
        return config.useShader ? config.bloomAlgorithm : BloomAlgorithm.DISABLED;
    }

    @RequiredArgsConstructor
    private final class FusionBloomEffect implements IBloomEffect {

        private final FusionReactorMachine machine;

        private static final BufferBuilder lightRingBuffer = new BufferBuilder(
                GTRenderTypes.getLightRing().bufferSize());

        @Override
        public void renderBloomEffect(@NotNull PoseStack poseStack, @NotNull BufferBuilder buffer,
                                      @NotNull EffectRenderContext context) {
            BlockPos pos = machine.getPos();

            lightRingBuffer.begin(GTRenderTypes.getLightRing().mode(), GTRenderTypes.getLightRing().format());
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

            FusionRingRender.this.renderLightRing(machine, context.partialTicks(), poseStack, lightRingBuffer);

            poseStack.popPose();

            BufferUploader.drawWithShader(lightRingBuffer.end());
        }

        @Override
        public boolean shouldRenderBloomEffect(@NotNull EffectRenderContext context) {
            return machine.recipeLogic.isWorking() && delta > 0 &&
                    context.frustum().isVisible(FusionRingRender.this.getRenderBoundingBox(machine));
        }
    }

    private static final class FusionBloomSetup implements IRenderSetup {

        private static final FusionBloomSetup INSTANCE = new FusionBloomSetup();

        @Override
        public void preDraw(@NotNull BufferBuilder buffer) {
            var config = ConfigHolder.INSTANCE.client.shader.fusionBloom;

            BloomEffect.strength = config.strength;
            BloomEffect.baseBrightness = config.baseBrightness;
            BloomEffect.highBrightnessThreshold = config.highBrightnessThreshold;
            BloomEffect.lowBrightnessThreshold = config.lowBrightnessThreshold;
            BloomEffect.step = 1;

            RenderSystem.setShaderColor(1, 1, 1, 1);
        }

        @Override
        public void postDraw(@NotNull BufferBuilder buffer) {}
    }
}
