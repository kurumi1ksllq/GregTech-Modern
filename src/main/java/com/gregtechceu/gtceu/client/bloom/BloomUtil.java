package com.gregtechceu.gtceu.client.bloom;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.client.model.BloomMetadataSection;
import com.gregtechceu.gtceu.client.particle.GTParticle;
import com.gregtechceu.gtceu.client.renderer.GTRenderTypes;
import com.gregtechceu.gtceu.client.shader.GTShaders;
import com.gregtechceu.gtceu.config.ConfigHolder;
import com.gregtechceu.gtceu.core.mixins.client.LevelRendererAccessor;
import com.gregtechceu.gtceu.core.mixins.client.PostChainAccessor;
import com.gregtechceu.gtceu.core.mixins.client.VertexBufferAccessor;
import com.gregtechceu.gtceu.integration.embeddium.GTEmbeddiumCompat;

import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.ForgeHooksClient;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class BloomUtil {

    public static float strength = ConfigHolder.INSTANCE.client.shader.strength;
    public static float baseBrightness = ConfigHolder.INSTANCE.client.shader.baseBrightness;
    public static float highBrightnessThreshold = ConfigHolder.INSTANCE.client.shader.highBrightnessThreshold;
    public static float lowBrightnessThreshold = ConfigHolder.INSTANCE.client.shader.lowBrightnessThreshold;
    public static float step = ConfigHolder.INSTANCE.client.shader.step;

    private static final Map<BloomRenderKey, List<BloomRenderTicket>> BLOOM_RENDERS = new Object2ObjectOpenHashMap<>();
    private static final List<BloomRenderTicket> SCHEDULED_BLOOM_RENDERS = new ArrayList<>();

    private static final ReadWriteLock BLOOM_RENDER_LOCK = new ReentrantReadWriteLock();

    public static Map<BlockPos, VertexBuffer> BLOOM_BUFFERS = new HashMap<>();
    public static Map<BlockPos, BufferBuilder> BLOOM_BUFFER_BUILDERS = new ConcurrentHashMap<>();
    public static Map<BlockPos, BufferBuilder.SortState> BLOOM_BUFFER_SORT_STATES = new HashMap<>();

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
        BLOOM_RENDER_LOCK.writeLock().lock();
        try {
            SCHEDULED_BLOOM_RENDERS.add(ticket);
        } finally {
            BLOOM_RENDER_LOCK.writeLock().unlock();
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
        BLOOM_RENDER_LOCK.readLock().lock();
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
            BLOOM_RENDER_LOCK.readLock().lock();
        }
    }

    public static void init() {}

    public static void renderBloom(Camera camera, @NotNull Entity entity, LevelRenderer levelRenderer,
                                   PoseStack poseStack, Matrix4f projectionMatrix, Frustum frustum,
                                   float partialTicks) {
        if (!GTShaders.allowedShader()) {
            return;
        }
        Vec3 camPos = camera.getPosition();
        Minecraft.getInstance().getProfiler().popPush("gtceu_block_bloom");

        BLOOM_RENDER_LOCK.writeLock().lock();
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

                setupRenderState(false);
                render(partialTicks, poseStack, projectionMatrix, levelRenderer, camera, frustum);
                VertexBuffer.unbind();
                Minecraft.getInstance().getProfiler().pop();

                // noinspection UnstableApiUsage
                ForgeHooksClient.dispatchRenderStage(GTRenderTypes.getBloom(), levelRenderer,
                        poseStack, projectionMatrix, levelRenderer.getTicks(), camera, frustum);

                GTRenderTypes.getBloom().clearRenderState();
                return;
            }

            strength = ConfigHolder.INSTANCE.client.shader.strength;
            baseBrightness = ConfigHolder.INSTANCE.client.shader.baseBrightness;
            highBrightnessThreshold = ConfigHolder.INSTANCE.client.shader.highBrightnessThreshold;
            lowBrightnessThreshold = ConfigHolder.INSTANCE.client.shader.lowBrightnessThreshold;
            step = ConfigHolder.INSTANCE.client.shader.step;

            // ********** render custom bloom ************

            RenderSystem.depthMask(true);
            if (!BLOOM_RENDERS.isEmpty()) {
                for (List<BloomRenderTicket> list : BLOOM_RENDERS.values()) {
                    BufferBuilder buffer = Tesselator.getInstance().getBuilder();
                    draw(poseStack, buffer, context, list);
                }
            }
            RenderSystem.depthMask(false);
            postDraw();

            setupRenderState(true);
            drawBlockBloom(poseStack, projectionMatrix, camPos);
            render(partialTicks, poseStack, projectionMatrix, levelRenderer, camera, frustum);
        } finally {
            BLOOM_RENDER_LOCK.writeLock().unlock();
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

    public static void finishBloomBuffer(BlockPos pos, BufferBuilder builder) {
        BufferBuilder.RenderedBuffer buffer = builder.endOrDiscardIfEmpty();
        if (buffer != null) {
            BLOOM_BUFFER_BUILDERS.remove(pos, builder);
            BLOOM_BUFFER_SORT_STATES.put(pos, builder.getSortState());

            if (RenderSystem.isOnRenderThread()) {
                VertexBuffer vertexBuffer = BLOOM_BUFFERS.computeIfAbsent(pos,
                        $ -> new VertexBuffer(VertexBuffer.Usage.STATIC));
                BloomUtil.uploadBloomBuffer(buffer, vertexBuffer);
            } else {
                RenderSystem.recordRenderCall(() -> {
                    VertexBuffer vertexBuffer = BLOOM_BUFFERS.computeIfAbsent(pos,
                            $ -> new VertexBuffer(VertexBuffer.Usage.STATIC));
                    BloomUtil.uploadBloomBuffer(buffer, vertexBuffer);
                });
            }
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
        BLOOM_BUFFER_BUILDERS.remove(origin);
        VertexBuffer buffer = BLOOM_BUFFERS.remove(origin);
        if (buffer != null) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(buffer::close);
            } else {
                buffer.close();
            }
        }
    }

    public static BufferBuilder getOrStartBloomBuffer(BlockPos pos) {
        BufferBuilder builder = BLOOM_BUFFER_BUILDERS.computeIfAbsent(pos,
                $ -> new BufferBuilder(GTRenderTypes.getBloom().bufferSize()));
        if (!builder.building()) {
            builder.begin(GTRenderTypes.getBloom().mode(), GTRenderTypes.getBloom().format());
        }
        return builder;
    }

    public static void bakeBloomChunkBuffers(BlockPos pos, Vec3 camPos) {
        if (!GTShaders.allowedShader()) {
            return;
        }

        BufferBuilder builder = BLOOM_BUFFER_BUILDERS.get(pos);
        if (builder == null || !builder.building()) {
            return;
        }
        builder.setQuadSorting(VertexSorting.byDistance((float) camPos.x() - pos.getX(),
                (float) camPos.y() - pos.getY(), (float) camPos.z() - pos.getZ()));

        finishBloomBuffer(pos, builder);
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

    private static void setupRenderState(boolean drawBlockBloom) {
        // RenderSystem.enableDepthTest();
        // RenderSystem.enableBlend();
        // RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        // Forcefully insert config values to shader
        List<PostPass> passes = ((PostChainAccessor) GTShaders.BLOOM_CHAIN).getPasses();
        for (PostPass pass : passes) {
            EffectInstance shader = pass.getEffect();
            String name = shader.getName();

            shader.safeGetUniform("EnableFilter").set(drawBlockBloom ? 1 : 0);

            if (GTShaders.BLOOM_TYPE == BloomAlgorithm.UNREAL && name.equals(SEPERABLE_BLUR_SHADER_NAME)) {
                int index = passes.indexOf(pass);
                if (index % 2 == 0) {
                    shader.safeGetUniform(BLUR_DIR_UNIFORM).set(0.0f, step);
                } else {
                    shader.safeGetUniform(BLUR_DIR_UNIFORM).set(step, 0.0f);
                }
            }
            if (name.equals(UNITY_COMPOSITE_SHADER_NAME) || name.equals(UNREAL_COMPOSITE_SHADER_NAME)) {
                shader.safeGetUniform(BLOOM_INTENSIVE_UNIFORM).set(strength);
                shader.safeGetUniform(BLOOM_BASE_UNIFORM).set(baseBrightness);
                shader.safeGetUniform(BLOOM_THRESHOLD_UP_UNIFORM).set(highBrightnessThreshold);
                shader.safeGetUniform(BLOOM_THRESHOLD_DOWN_UNIFORM).set(lowBrightnessThreshold);
            }
        }
    }

    private static void drawBlockBloom(PoseStack poseStack, Matrix4f projectionMatrix, Vec3 camPos) {
        for (var it = BLOOM_BUFFERS.entrySet().iterator(); it.hasNext();) {
            var entry = it.next();
            BlockPos pos = entry.getKey();

            // return early if buffer is invalid or has no vertex data bound
            // VertexBuffer#mode's nullness is the easiest way to check this.
            try {
                if (entry.getValue().isInvalid() || ((VertexBufferAccessor) entry.getValue()).getMode() == null) {
                    continue;
                }
                entry.getValue().bind();

                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
                poseStack.translate(-camPos.x(), -camPos.y(), -camPos.z());

                entry.getValue().drawWithShader(poseStack.last().pose(), projectionMatrix,
                        GameRenderer.getRendertypeTranslucentShader());
                poseStack.popPose();
            } finally {
                entry.getValue().close();
                BLOOM_BUFFER_BUILDERS.remove(pos);
                BLOOM_BUFFER_SORT_STATES.remove(pos);
                it.remove();
            }
        }
    }

    private static void render(float partialTicks, PoseStack poseStack, Matrix4f projectionMatrix,
                               LevelRenderer levelRenderer, Camera camera, Frustum frustum) {
        // RenderSystem.disableDepthTest();
        // RenderSystem.disableBlend();
        // RenderSystem.defaultBlendFunc();
        RenderSystem.resetTextureMatrix();

        GTShaders.BLOOM_CHAIN.process(partialTicks);
        Minecraft.getInstance().getMainRenderTarget().bindWrite(false);

        VertexBuffer.unbind();
        Minecraft.getInstance().getProfiler().pop();

        // noinspection UnstableApiUsage
        ForgeHooksClient.dispatchRenderStage(GTRenderTypes.getBloom(), levelRenderer,
                poseStack, projectionMatrix, levelRenderer.getTicks(), camera, frustum);

        GTRenderTypes.getBloom().clearRenderState();
    }

    private static double xBloomOld;
    private static double yBloomOld;
    private static double zBloomOld;

    public static void resortBloomTransparency(Vec3 camPos, LevelRenderer renderer) {
        Minecraft.getInstance().getProfiler().push("translucent_sort");

        for (BlockPos pos : getVisibleRenderRegions(camPos, renderer)) {
            Util.backgroundExecutor().submit(() -> BloomUtil.resortTransparencyInner(pos, camPos));
        }

        Minecraft.getInstance().getProfiler().pop();
    }

    private static void resortTransparencyInner(BlockPos pos, Vec3 camPos) {
        BufferBuilder builder = getOrStartBloomBuffer(pos);
        builder.restoreSortState(BLOOM_BUFFER_SORT_STATES.get(pos));
        builder.setQuadSorting(VertexSorting.byDistance((float) camPos.x() - pos.getX(),
                (float) camPos.y() - pos.getY(), (float) camPos.z() - pos.getZ()));
        finishBloomBuffer(pos, builder);
    }

    private static List<BlockPos> getVisibleRenderRegions(Vec3 camPos, LevelRenderer renderer) {
        if (GTCEu.Mods.isSodiumEmbeddiumLoaded()) {
            return GTEmbeddiumCompat.getVisibleRenderSections(camPos);
        } else {
            List<BlockPos> result = new ArrayList<>();

            for (var chunkInfo : ((LevelRendererAccessor) renderer).gtceu$getRenderChunksInFrustum()) {
                double dx = camPos.x - xBloomOld;
                double dy = camPos.y - yBloomOld;
                double dz = camPos.z - zBloomOld;

                double camDelta = (dx * dx) + (dy * dy) + (dz * dz);

                if (camDelta < 1) {
                    // Didn't move enough, ignore
                    continue;
                }

                int camSectionX = SectionPos.posToSectionCoord(camPos.x);
                int camSectionY = SectionPos.posToSectionCoord(camPos.y);
                int camSectionZ = SectionPos.posToSectionCoord(camPos.z);

                boolean posChanged = camSectionX != SectionPos.posToSectionCoord(xBloomOld) ||
                        camSectionY != SectionPos.posToSectionCoord(yBloomOld) ||
                        camSectionZ != SectionPos.posToSectionCoord(zBloomOld);
                xBloomOld = camPos.x;
                yBloomOld = camPos.y;
                zBloomOld = camPos.z;

                BlockPos pos = SectionPos.of(camSectionX, camSectionY, camSectionZ).origin();
                if (!BLOOM_BUFFERS.containsKey(pos) || !BLOOM_BUFFER_SORT_STATES.containsKey(pos)) {
                    continue;
                }

                if (posChanged || chunkInfo.isAxisAlignedWith(camSectionX, camSectionY, camSectionZ)) {
                    result.add(pos);
                }
            }

            return result;
        }
    }

    public static void copyToBloomBuffer(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad,
                                         float[] colorMuls, float red, float green, float blue,
                                         int[] combinedLights, int combinedOverlay, boolean mulColor,
                                         Operation<Void> original) {
        original.call(consumer, pose, quad,
                colorMuls, red, green, blue,
                combinedLights, combinedOverlay, mulColor);
        if (!GTShaders.allowedShader()) {
            return;
        }

        BlockPos chunkOrigin = BloomUtil.CURRENT_RENDERING_CHUNK_POS.get();
        if (chunkOrigin != null && BloomMetadataSection.hasBloom(quad, combinedLights)) {
            original.call(BloomUtil.getOrStartBloomBuffer(chunkOrigin), pose, quad,
                    colorMuls, red, green, blue,
                    combinedLights, combinedOverlay, mulColor);
        }
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

    @ApiStatus.Internal
    @FunctionalInterface
    public interface RegionVisibilityTest {

        boolean isAxisAlignedWith(int x, int y, int z);
    }
}
