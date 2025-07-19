package com.gregtechceu.gtceu.client.bloom;

import com.gregtechceu.gtceu.client.model.BloomMetadataSection;
import com.gregtechceu.gtceu.client.particle.GTParticle;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.client.shader.GTShaders;
import com.gregtechceu.gtceu.client.bloom.shader.BloomEffect;
import com.gregtechceu.gtceu.client.bloom.shader.BloomAlgorithm;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.core.mixins.client.PostChainAccessor;
import com.gregtechceu.gtceu.core.mixins.client.VertexBufferAccessor;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class BloomEffectUtil {

    private static final Map<BloomRenderKey, List<BloomRenderTicket>> BLOOM_RENDERS = new Object2ObjectOpenHashMap<>();
    private static final List<BloomRenderTicket> SCHEDULED_BLOOM_RENDERS = new ArrayList<>();

    private static final ReentrantLock BLOOM_RENDER_LOCK = new ReentrantLock();

    /**
     * <p>
     * Register a custom bloom render callback for subsequent world render. The render call persists until the
     * {@code blockEntity} is invalidated, or the world associated with {@code blockEntity} or the ticket is
     * manually freed by calling {@link BloomRenderTicket#invalidate()}.
     * </p>
     * <p>
     * This method does not register bloom render ticket when Iris/Oculus is present, and an invalid ticket will be
     * returned instead.
     * </p>
     *
     * @param setup       Render setup, if exists
     * @param algorithm   Type of the bloom
     * @param render      Rendering callback
     * @param blockEntity Meta tile entity instance
     * @return Ticket for the registered bloom render callback
     * @throws NullPointerException if {@code bloomType == null || render == null || blockEntity == null}
     */
    @NotNull
    public static BloomRenderTicket registerBloomRender(@Nullable IRenderSetup setup,
                                                        @NotNull BloomAlgorithm algorithm,
                                                        @NotNull IBloomEffect render,
                                                        @NotNull BlockEntity blockEntity) {
        Objects.requireNonNull(blockEntity, "blockEntity == null");
        return registerBloomRender(setup, algorithm,
                new IBloomEffect() {

                    @Override
                    public void renderBloomEffect(@NotNull PoseStack poseStack, @NotNull BufferBuilder buffer,
                                                  @NotNull EffectRenderContext context) {
                        render.renderBloomEffect(poseStack, buffer, context);
                    }

                    @Override
                    public boolean shouldRenderBloomEffect(@NotNull EffectRenderContext context) {
                        return blockEntity.getLevel() == context.getRenderViewEntity().level() &&
                                render.shouldRenderBloomEffect(context);
                    }
                },
                t -> !blockEntity.isRemoved(),
                blockEntity::getLevel);
    }

    /**
     * <p>
     * Register a custom bloom render callback for subsequent world render. The render call persists until the
     * {@code particle} is invalidated, or the ticket is manually freed by calling
     * {@link BloomRenderTicket#invalidate()}.
     * </p>
     * <p>
     * This method does not register bloom render ticket when Iris/Oculus is present, and an invalid ticket will be
     * returned instead.
     * </p>
     *
     * @param setup     Render setup, if exists
     * @param algorithm Type of the bloom
     * @param render    Rendering callback
     * @param particle  Particle instance
     * @return Ticket for the registered bloom render callback
     * @throws NullPointerException if {@code bloomType == null || render == null || metaTileEntity == null}
     */
    @NotNull
    public static BloomRenderTicket registerBloomRender(@Nullable IRenderSetup setup,
                                                        @NotNull BloomAlgorithm algorithm,
                                                        @NotNull IBloomEffect render,
                                                        @NotNull GTParticle particle) {
        Objects.requireNonNull(particle, "particle == null");
        return registerBloomRender(setup, algorithm, render, t -> particle.isAlive());
    }

    /**
     * <p>
     * Register a custom bloom render callback for subsequent world render. The render call persists until it is
     * manually freed by calling {@link BloomRenderTicket#invalidate()}, or invalidated by validity checker.
     * </p>
     * <p>
     * This method does not register bloom render ticket when Iris/Oculus is present, and an invalid ticket will be
     * returned instead.
     * </p>
     *
     * @param setup           Render setup, if exists
     * @param algorithm       Type of the bloom
     * @param render          Rendering callback
     * @param validityChecker Optional validity checker; returning {@code false} causes the ticket to be invalidated.
     *                        Checked on both pre-/post-render each frame.
     * @return Ticket for the registered bloom render callback
     * @throws NullPointerException if {@code bloomType == null || render == null}
     * @see #registerBloomRender(IRenderSetup, BloomAlgorithm, IBloomEffect, BlockEntity)
     * @see #registerBloomRender(IRenderSetup, BloomAlgorithm, IBloomEffect, GTParticle)
     * @see #registerBloomRender(IRenderSetup, BloomAlgorithm, IBloomEffect, Predicate, Supplier)
     */
    @NotNull
    public static BloomRenderTicket registerBloomRender(@Nullable IRenderSetup setup,
                                                        @NotNull BloomAlgorithm algorithm,
                                                        @NotNull IBloomEffect render,
                                                        @Nullable Predicate<BloomRenderTicket> validityChecker) {
        return registerBloomRender(setup, algorithm, render, validityChecker, null);
    }

    /**
     * <p>
     * Register a custom bloom render callback for subsequent world render. The render call persists until it is
     * manually freed by calling {@link BloomRenderTicket#invalidate()}, or invalidated by validity checker.
     * </p>
     * <p>
     * This method does not register bloom render ticket when Iris/Oculus is present, and an invalid ticket will be
     * returned instead.
     * </p>
     *
     * @param setup           Render setup, if exists
     * @param algorithm       Type of the bloom
     * @param render          Rendering callback
     * @param validityChecker Optional validity checker; returning {@code false} causes the ticket to be invalidated.
     *                        Checked on both pre/post render each frame.
     * @param worldContext    Optional world bound to the ticket. If the world returned is not null, the bloom ticket
     *                        will be automatically invalidated on world unload. If world context returns {@code null},
     *                        it will not be affected by aforementioned automatic invalidation.
     * @return Ticket for the registered bloom render callback
     * @throws NullPointerException if {@code bloomType == null || render == null}
     * @see #registerBloomRender(IRenderSetup, BloomAlgorithm, IBloomEffect, BlockEntity)
     * @see #registerBloomRender(IRenderSetup, BloomAlgorithm, IBloomEffect, GTParticle)
     */
    @NotNull
    public static BloomRenderTicket registerBloomRender(@Nullable IRenderSetup setup,
                                                        @NotNull BloomAlgorithm algorithm,
                                                        @NotNull IBloomEffect render,
                                                        @Nullable Predicate<BloomRenderTicket> validityChecker,
                                                        @Nullable Supplier<Level> worldContext) {
        if (!GTShaders.allowedShader()) return BloomRenderTicket.INVALID;
        if (algorithm == BloomAlgorithm.DISABLED) return BloomRenderTicket.INVALID;
        BloomRenderTicket ticket = new BloomRenderTicket(setup, algorithm, render, validityChecker, worldContext);
        BLOOM_RENDER_LOCK.lock();
        try {
            SCHEDULED_BLOOM_RENDERS.add(ticket);
        } finally {
            BLOOM_RENDER_LOCK.unlock();
        }
        return ticket;
    }

    /**
     * Invalidate tickets associated with given level.
     *
     * @param level the level that was unloaded
     */
    public static void invalidateLevelTickets(@NotNull LevelAccessor level) {
        Objects.requireNonNull(level, "level == null");
        BLOOM_RENDER_LOCK.lock();
        try {
            for (BloomRenderTicket ticket : SCHEDULED_BLOOM_RENDERS) {
                if (ticket.isValid() && ticket.worldContext != null && ticket.worldContext.get() == level) {
                    ticket.invalidate();
                }
            }

            for (var e : BLOOM_RENDERS.entrySet()) {
                for (BloomRenderTicket ticket : e.getValue()) {
                    if (ticket.isValid() && ticket.worldContext != null && ticket.worldContext.get() == level) {
                        ticket.invalidate();
                    }
                }
            }
        } finally {
            BLOOM_RENDER_LOCK.unlock();
        }
    }

    public static void init() {}

    public static final AtomicBoolean isDrawingBlockBloom = new AtomicBoolean(false);

    public static void renderBloom(Vec3 camPos, @NotNull Entity entity, PoseStack poseStack,
                                   Matrix4f projectionMatrix, Frustum frustum, float partialTicks) {
        if (!GTShaders.allowedShader()) {
            return;
        }
        Minecraft.getInstance().getProfiler().popPush("gtceu_block_bloom");

        BLOOM_RENDER_LOCK.lock();
        try {
            preDraw();

            GTShaders.BLOOM_TARGET.bindWrite(false);

            EffectRenderContext context = EffectRenderContext.getInstance()
                    .update(entity, camPos, frustum, partialTicks);

            GTRenderTypes.getBloom().setupRenderState();

            if (!ConfigHolder.INSTANCE.client.shader.emissiveTexturesBloom) {
                RenderSystem.depthMask(true);

                if (!BLOOM_RENDERS.isEmpty()) {
                    for (List<BloomRenderTicket> list : BLOOM_RENDERS.values()) {
                        BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                        draw(poseStack, buffer, context, list);
                    }
                }

                RenderSystem.depthMask(false);
                postDraw();

                render(partialTicks, poseStack, projectionMatrix, camPos);
                return;
            }

            BloomEffect.strength = ConfigHolder.INSTANCE.client.shader.strength;
            BloomEffect.baseBrightness = ConfigHolder.INSTANCE.client.shader.baseBrightness;
            BloomEffect.highBrightnessThreshold = ConfigHolder.INSTANCE.client.shader.highBrightnessThreshold;
            BloomEffect.lowBrightnessThreshold = ConfigHolder.INSTANCE.client.shader.lowBrightnessThreshold;
            BloomEffect.step = ConfigHolder.INSTANCE.client.shader.step;

            // ********** render custom bloom ************

            RenderSystem.depthMask(true);
            if (!BLOOM_RENDERS.isEmpty()) {
                for (var e : BLOOM_RENDERS.entrySet()) {
                    List<BloomRenderTicket> list = e.getValue();
                    BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                    draw(poseStack, buffer, context, list);
                }
            }
            RenderSystem.depthMask(false);

            isDrawingBlockBloom.set(true);
            render(partialTicks, poseStack, projectionMatrix, camPos);
            isDrawingBlockBloom.set(false);

            postDraw();
            GTRenderTypes.getBloom().clearRenderState();
        } finally {
            BLOOM_RENDER_LOCK.unlock();
        }
    }

    private static void preDraw() {
        for (BloomRenderTicket ticket : SCHEDULED_BLOOM_RENDERS) {
            if (!ticket.isValid()) continue;
            BLOOM_RENDERS.computeIfAbsent(new BloomRenderKey(ticket.renderSetup, ticket.algorithm),
                    k -> new ArrayList<>()).add(ticket);
        }
        SCHEDULED_BLOOM_RENDERS.clear();
    }

    private static void draw(@NotNull PoseStack poseStack, @NotNull BufferBuilder buffer,
                             @NotNull EffectRenderContext context, @NotNull List<BloomRenderTicket> tickets) {
        boolean initialized = false;
        @Nullable
        IRenderSetup renderSetup = null;
        for (BloomRenderTicket ticket : tickets) {
            ticket.checkValidity();
            if (!ticket.isValid() || !ticket.render.shouldRenderBloomEffect(context)) continue;
            if (!initialized) {
                initialized = true;
                renderSetup = ticket.renderSetup;
                if (renderSetup != null) {
                    renderSetup.preDraw(buffer);
                }
            }

            poseStack.pushPose();
            poseStack.translate(-context.camPos().x(), -context.camPos().y(), -context.camPos().z());
            ticket.render.renderBloomEffect(poseStack, buffer, context);
            poseStack.popPose();
        }
        if (initialized && renderSetup != null) {
            renderSetup.postDraw(buffer);
        }
    }

    private static void postDraw() {
        for (var it = BLOOM_RENDERS.values().iterator(); it.hasNext();) {
            List<BloomRenderTicket> list = it.next();

            if (!list.isEmpty()) {
                if (!list.removeIf(ticket -> {
                    ticket.checkValidity();
                    return !ticket.isValid();
                }) || !list.isEmpty()) continue;
            }

            it.remove();
        }
    }

    public static void uploadBloomBuffer(BufferBuilder.RenderedBuffer builder, VertexBuffer buffer) {
        if (!buffer.isInvalid()) {
            buffer.bind();
            buffer.upload(builder);
            VertexBuffer.unbind();
        }
    }

    public static void removeBloomChunk(BlockPos origin) {
        GTShaders.BLOOM_BUFFER_BUILDERS.remove(origin);
        VertexBuffer buffer = GTShaders.BLOOM_BUFFERS.remove(origin);
        if (buffer != null) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(buffer::close);
            } else {
                buffer.close();
            }
        }
        GTShaders.RENDERED_BLOOM_BUFFERS.remove(origin);
    }

    public static BufferBuilder getOrStartBloomBuffer(BlockPos pos) {
        BufferBuilder builder = GTShaders.BLOOM_BUFFER_BUILDERS.computeIfAbsent(pos,
                $ -> new BufferBuilder(GTRenderTypes.getBloom().bufferSize()));
        if (!builder.building()) {
            builder.begin(GTRenderTypes.getBloom().mode(), GTRenderTypes.getBloom().format());
        }
        return builder;
    }

    public static void bakeBloomChunkBuffers(BlockPos pos) {
        if (!GTShaders.allowedShader()) {
            return;
        }

        BufferBuilder builder = GTShaders.BLOOM_BUFFER_BUILDERS.get(pos);
        if (builder == null || !builder.building()) {
            return;
        }
        BufferBuilder.RenderedBuffer buffer = builder.endOrDiscardIfEmpty();
        if (buffer != null) {
            GTShaders.RENDERED_BLOOM_BUFFERS.put(pos, buffer);
            if (RenderSystem.isOnRenderThread()) {
                VertexBuffer vertexBuffer = GTShaders.BLOOM_BUFFERS.computeIfAbsent(pos,
                        $ -> new VertexBuffer(VertexBuffer.Usage.STATIC));
                BloomEffectUtil.uploadBloomBuffer(buffer, vertexBuffer);
            } else {
                RenderSystem.recordRenderCall(() -> {
                    VertexBuffer vertexBuffer = GTShaders.BLOOM_BUFFERS.computeIfAbsent(pos,
                            $ -> new VertexBuffer(VertexBuffer.Usage.STATIC));
                    BloomEffectUtil.uploadBloomBuffer(buffer, vertexBuffer);
                });
            }
        }
    }

    public static ThreadLocal<BlockPos> CURRENT_RENDERING_CHUNK_POS = new ThreadLocal<>();

    private static final String UNREAL_COMPOSITE_SHADER_NAME = "gtceu:unreal_composite";
    private static final String UNITY_COMPOSITE_SHADER_NAME = "gtceu:unity_composite";
    private static final String SEPERABLE_BLUR_SHADER_NAME = "gtceu:seperable_blur";
    private static final String BLOOM_INTENSIVE_UNIFORM = "BloomIntensive";
    private static final String BLOOM_BASE_UNIFORM = "BloomBase";
    private static final String BLOOM_THRESHOLD_UP_UNIFORM = "BloomThresholdUp";
    private static final String BLOOM_THRESHOLD_DOWN_UNIFORM = "BloomThresholdDown";
    private static final String BLUR_DIR_UNIFORM = "BlurDir";

    private static void render(float partialTicks, PoseStack poseStack, Matrix4f projectionMatrix, Vec3 camPos) {
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        // Forcefully insert config values to shader
        List<PostPass> passes = ((PostChainAccessor) GTShaders.BLOOM_CHAIN).getPasses();
        for (PostPass pass : passes) {
            EffectInstance shader = pass.getEffect();
            String name = shader.getName();

            shader.safeGetUniform("EnableFilter").set(BloomEffectUtil.isDrawingBlockBloom.get() ? 1 : 0);

            if (GTShaders.BLOOM_TYPE == BloomAlgorithm.UNREAL && name.equals(SEPERABLE_BLUR_SHADER_NAME)) {
                int index = passes.indexOf(pass);
                switch (index) {
                    case 1, 3, 5, 7 -> shader.safeGetUniform(BLUR_DIR_UNIFORM).set(BloomEffect.step, 0.0f);
                    case 2, 4, 6, 8 -> shader.safeGetUniform(BLUR_DIR_UNIFORM).set(0.0f, BloomEffect.step);
                }
            }
            if (name.equals(UNITY_COMPOSITE_SHADER_NAME) || name.equals(UNREAL_COMPOSITE_SHADER_NAME)) {
                shader.safeGetUniform(BLOOM_INTENSIVE_UNIFORM).set(BloomEffect.strength);
                shader.safeGetUniform(BLOOM_BASE_UNIFORM).set(BloomEffect.baseBrightness);
                shader.safeGetUniform(BLOOM_THRESHOLD_UP_UNIFORM).set(BloomEffect.highBrightnessThreshold);
                shader.safeGetUniform(BLOOM_THRESHOLD_DOWN_UNIFORM).set(BloomEffect.lowBrightnessThreshold);
            }
        }

        for (var entry : GTShaders.BLOOM_BUFFERS.entrySet()) {
            // return early if buffer is invalid or has no vertex data bound
            // VertexBuffer#mode's nullness is the easiest way to check this.
            if (entry.getValue().isInvalid() || ((VertexBufferAccessor) entry.getValue()).getMode() == null) {
                continue;
            }
            entry.getValue().bind();
            poseStack.pushPose();
            poseStack.translate(entry.getKey().getX(), entry.getKey().getY(), entry.getKey().getZ());
            poseStack.translate(-camPos.x(), -camPos.y(), -camPos.z());
            entry.getValue().drawWithShader(poseStack.last().pose(), projectionMatrix, RenderSystem.getShader());
            poseStack.popPose();
        }

        GTShaders.BLOOM_CHAIN.process(partialTicks);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        VertexBuffer.unbind();
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);
    }

    public static void copyToBloomBuffer(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad,
                                         float[] colorMuls, float red, float green, float blue,
                                         int[] combinedLights, int combinedOverlay, boolean mulColor,
                                         Operation<Void> original) {
        BlockPos chunkOrigin = BloomEffectUtil.CURRENT_RENDERING_CHUNK_POS.get();
        if (GTShaders.allowedShader() && chunkOrigin != null && BloomMetadataSection.hasBloom(quad, combinedLights)) {
            original.call(BloomEffectUtil.getOrStartBloomBuffer(chunkOrigin), pose, quad,
                    colorMuls, red, green, blue,
                    combinedLights, combinedOverlay, mulColor);
        }
        original.call(consumer, pose, quad,
                colorMuls, red, green, blue,
                combinedLights, combinedOverlay, mulColor);
    }

    private record BloomRenderKey(@Nullable IRenderSetup renderSetup, @NotNull BloomAlgorithm algorithm) {

    }

    public static final class BloomRenderTicket {

        public static final BloomRenderTicket INVALID = new BloomRenderTicket();

        @Nullable
        private final IRenderSetup renderSetup;
        private final BloomAlgorithm algorithm;
        private final IBloomEffect render;
        @Nullable
        private final Predicate<BloomRenderTicket> validityChecker;
        @Nullable
        private final Supplier<Level> worldContext;

        private boolean invalidated;

        BloomRenderTicket() {
            this(null, BloomAlgorithm.DISABLED, (p, b, c) -> {}, null, null);
            this.invalidated = true;
        }

        BloomRenderTicket(@Nullable IRenderSetup renderSetup, @NotNull BloomAlgorithm algorithm,
                          @NotNull IBloomEffect render, @Nullable Predicate<BloomRenderTicket> validityChecker,
                          @Nullable Supplier<Level> worldContext) {
            this.renderSetup = renderSetup;
            this.algorithm = Objects.requireNonNull(algorithm, "algorithm == null");
            this.render = Objects.requireNonNull(render, "render == null");
            this.validityChecker = validityChecker;
            this.worldContext = worldContext;
        }

        public boolean isValid() {
            return !this.invalidated;
        }

        public void invalidate() {
            this.invalidated = true;
        }

        private void checkValidity() {
            if (!this.invalidated && this.validityChecker != null && !this.validityChecker.test(this)) {
                invalidate();
            }
        }
    }
}
