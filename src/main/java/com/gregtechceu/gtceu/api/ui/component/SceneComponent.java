package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.container.StackLayout;
import com.gregtechceu.gtceu.api.ui.core.Sizing;
import com.gregtechceu.gtceu.api.ui.core.UIComponentMenuAccess;
import com.gregtechceu.gtceu.api.ui.core.UIGuiGraphics;
import com.gregtechceu.gtceu.api.ui.texture.UITextures;

import com.lowdragmc.lowdraglib.client.scene.*;
import com.lowdragmc.lowdraglib.client.utils.RenderUtils;
import com.lowdragmc.lowdraglib.utils.BlockPosFace;
import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FastColor;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings({ "unused", "UnusedReturnValue", "SameParameterValue" })
@Accessors(fluent = true, chain = true)
public class SceneComponent extends StackLayout {

    @Getter
    protected WorldSceneRenderer renderer;
    @Getter
    protected TrackedDummyWorld dummyWorld;
    protected boolean dragging;
    @Setter
    protected boolean renderFacing = true;
    @Setter
    protected boolean renderSelect = true;
    @Setter
    protected boolean draggable = true;
    @Setter
    protected boolean scalable = true;
    @Setter
    protected boolean intractable = true;
    @Setter
    protected boolean hoverTips;
    protected int currentMouseX;
    protected int currentMouseY;
    @Getter
    protected Vector3f center;
    @Getter
    protected float rotationYaw = 25;
    @Getter
    protected float rotationPitch = -135;
    @Getter
    protected float zoom = 5;
    protected float range;
    @Getter
    protected BlockPosFace clickPosFace;
    @Getter
    protected BlockPosFace hoverPosFace;
    @Getter
    protected BlockPosFace selectedPosFace;
    @Getter
    protected ItemStack hoverItem;
    @Setter
    protected BiConsumer<BlockPos, Direction> onSelected;
    @Getter
    protected Set<BlockPos> core;
    protected boolean useCache;
    protected boolean useOrthographicCamera = false;
    protected boolean autoReleased;
    protected Consumer<SceneComponent> beforeWorldRender;
    protected Consumer<SceneComponent> afterWorldRender;

    protected boolean initialized;

    public SceneComponent(Sizing horizontalSizing, Sizing verticalSizing, Level world, boolean useFBO) {
        super(horizontalSizing, verticalSizing);
        createScene(world, useFBO);
    }

    public SceneComponent(Sizing horizontalSizing, Sizing verticalSizing, Level world) {
        super(horizontalSizing, verticalSizing);
        createScene(world);
    }

    public SceneComponent useCacheBuffer() {
        return useCacheBuffer(true);
    }

    public SceneComponent useCacheBuffer(boolean autoReleased) {
        useCache = true;
        this.autoReleased = autoReleased;
        if (renderer != null) {
            renderer.useCacheBuffer(true);
        }
        return this;
    }

    public SceneComponent useOrthographicCamera() {
        return useOrthographicCamera(true);
    }

    public SceneComponent useOrthographicCamera(boolean useOrtho) {
        this.useOrthographicCamera = useOrtho;
        if (renderer != null) {
            renderer.useOrtho(useOrtho);
        }
        return this;
    }

    public SceneComponent beforeWorldRender(Consumer<SceneComponent> beforeWorldRender) {
        this.beforeWorldRender = beforeWorldRender;
        if (this.beforeWorldRender != null && renderer != null) {
            renderer.setBeforeWorldRender(s -> beforeWorldRender.accept(this));
        }
        return this;
    }

    public SceneComponent afterWorldRender(Consumer<SceneComponent> afterWorldRender) {
        this.afterWorldRender = afterWorldRender;
        return this;
    }

    private float camZoom() {
        if (useOrthographicCamera) {
            return 0.1f;
        } else {
            return zoom;
        }
    }

    public ParticleManager getParticleManager() {
        if (renderer == null) return null;
        return renderer.getParticleManager();
    }

    @Override
    public void containerAccess(UIComponentMenuAccess access) {
        super.containerAccess(access);
        if (initialized) {
            releaseCacheBuffer();
        }
    }

    @Override
    public void init() {
        super.init();
        this.initialized = true;
    }

    @Override
    public void dispose() {
        super.dispose();
        if (initialized) {
            releaseCacheBuffer();
        }
    }

    @Override
    public void dismount(DismountReason reason) {
        super.dismount(reason);
        if (initialized) {
            releaseCacheBuffer();
        }
    }

    @Override
    protected void parentUpdate(float delta, int mouseX, int mouseY) {
        super.parentUpdate(delta, mouseX, mouseY);
        ParticleManager particleManager = getParticleManager();
        if (particleManager != null) {
            particleManager.tick();
        }
        if (hoverItem != null) {
            tooltip(Screen.getTooltipFromItem(Minecraft.getInstance(), hoverItem));
        }
    }

    public void releaseCacheBuffer() {
        if (renderer != null && autoReleased) {
            renderer.deleteCacheBuffer();
        }
    }

    public void needCompileCache() {
        if (renderer != null) {
            renderer.needCompileCache();
        }
    }

    public final void createScene(Level world) {
        createScene(world, false);
    }

    protected ParticleManager createParticleManager() {
        return new ParticleManager();
    }

    public final void createScene(Level world, boolean useFBOSceneRenderer) {
        if (world == null) return;
        core = new HashSet<>();
        dummyWorld = new TrackedDummyWorld(world);
        dummyWorld.setRenderFilter(pos -> renderer.renderedBlocksMap.keySet().stream().anyMatch(c -> c.contains(pos)));
        if (renderer != null) {
            renderer.deleteCacheBuffer();
        }
        if (useFBOSceneRenderer) {
            renderer = new FBOWorldSceneRenderer(dummyWorld, 1080, 1080);
        } else {
            renderer = new ImmediateWorldSceneRenderer(dummyWorld);
        }
        center = new Vector3f(0, 0, 0);
        renderer.useOrtho(useOrthographicCamera);
        renderer.setOnLookingAt(ray -> {});
        renderer.setBeforeBatchEnd(this::renderBeforeBatchEnd);
        renderer.setAfterWorldRender(this::renderBlockOverLay);
        if (this.beforeWorldRender != null) {
            renderer.setBeforeWorldRender(s -> beforeWorldRender.accept(this));
        }
        renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
        renderer.useCacheBuffer(useCache);
        renderer.setParticleManager(createParticleManager());
        clickPosFace = null;
        hoverPosFace = null;
        hoverItem = null;
        selectedPosFace = null;
    }

    public SceneComponent clearColor(int color) {
        renderer.setClearColor(color);
        return this;
    }

    public SceneComponent renderedCore(Collection<BlockPos> blocks) {
        return renderedCore(blocks, null);
    }

    public SceneComponent renderedCore(Collection<BlockPos> blocks, ISceneBlockRenderHook renderHook) {
        core.clear();
        core.addAll(blocks);
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos vPos : blocks) {
            minX = Math.min(minX, vPos.getX());
            minY = Math.min(minY, vPos.getY());
            minZ = Math.min(minZ, vPos.getZ());
            maxX = Math.max(maxX, vPos.getX());
            maxY = Math.max(maxY, vPos.getY());
            maxZ = Math.max(maxZ, vPos.getZ());
        }
        center = new Vector3f((minX + maxX) / 2f + 0.5F, (minY + maxY) / 2f + 0.5F, (minZ + maxZ) / 2f + 0.5F);
        renderer.addRenderedBlocks(core, renderHook);
        this.zoom = (float) (3.5 *
                Math.sqrt(Math.max(Math.max(Math.max(maxX - minX + 1, maxY - minY + 1), maxZ - minZ + 1), 1)));
        renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
        renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
        needCompileCache();
        return this;
    }

    protected void renderBeforeBatchEnd(MultiBufferSource bufferSource, float partialTicks) {}

    public void renderBlockOverLay(WorldSceneRenderer renderer) {
        PoseStack poseStack = new PoseStack();
        hoverPosFace = null;
        hoverItem = null;
        if (isMouseOverElement(currentMouseX, currentMouseY)) {
            BlockHitResult hit = renderer.getLastTraceResult();
            if (hit != null) {
                if (core.contains(hit.getBlockPos())) {
                    hoverPosFace = new BlockPosFace(hit.getBlockPos(), hit.getDirection());
                } else if (!useOrthographicCamera) {
                    Vector3f hitPos = hit.getLocation().toVector3f();
                    Level world = renderer.world;
                    Vec3 eyePos = new Vec3(renderer.getEyePos());
                    hitPos.mul(2); // Double view range to ensure pos can be seen.
                    Vec3 endPos = new Vec3((hitPos.x - eyePos.x), (hitPos.y - eyePos.y), (hitPos.z - eyePos.z));
                    double min = Float.MAX_VALUE;
                    for (BlockPos pos : core) {
                        BlockState blockState = world.getBlockState(pos);
                        if (blockState.isAir()) {
                            continue;
                        }
                        hit = world.clipWithInteractionOverride(eyePos, endPos, pos, blockState.getShape(world, pos),
                                blockState);
                        if (hit != null && hit.getType() != HitResult.Type.MISS) {
                            double dist = eyePos.distanceToSqr(hit.getLocation());
                            if (dist < min) {
                                min = dist;
                                hoverPosFace = new BlockPosFace(hit.getBlockPos(), hit.getDirection());
                            }
                        }
                    }
                }
                if (hoverPosFace != null) {
                    var state = dummyWorld().getBlockState(hoverPosFace.pos);
                    hoverItem = state.getCloneItemStack(hit, dummyWorld(), hoverPosFace.pos, player());
                }
            }
        }
        BlockPosFace tmp = dragging ? clickPosFace : hoverPosFace;
        if (selectedPosFace != null || tmp != null) {
            if (selectedPosFace != null && renderFacing) {
                drawFacingBorder(poseStack, selectedPosFace, 0xff00ff00);
            }
            if (tmp != null && !tmp.equals(selectedPosFace) && renderFacing) {
                drawFacingBorder(poseStack, tmp, 0xffffffff);
            }
        }
        if (selectedPosFace != null && renderSelect) {
            RenderUtils.renderBlockOverLay(poseStack, selectedPosFace.pos, 0.6f, 0, 0, 1.01f);
        }

        if (this.afterWorldRender != null) {
            this.afterWorldRender.accept(this);
        }
    }

    public void drawFacingBorder(PoseStack poseStack, BlockPosFace posFace, int color) {
        drawFacingBorder(poseStack, posFace, color, 0);
    }

    public void drawFacingBorder(PoseStack poseStack, BlockPosFace posFace, int color, int inner) {
        poseStack.pushPose();
        RenderSystem.disableDepthTest();
        RenderUtils.moveToFace(poseStack, posFace.pos.getX(), posFace.pos.getY(), posFace.pos.getZ(), posFace.facing);
        RenderUtils.rotateToFace(poseStack, posFace.facing, null);
        poseStack.scale(1f / 16, 1f / 16, 0);
        poseStack.translate(-8, -8, 0);
        drawBorder(poseStack, 1 + inner * 2, 1 + inner * 2, 14 - 4 * inner, 14 - 4 * inner, color, 1);
        RenderSystem.enableDepthTest();
        poseStack.popPose();
    }

    private static void drawBorder(PoseStack poseStack, int x, int y, int width, int height, int color, int border) {
        drawSolidRect(poseStack, x - border, y - border, width + 2 * border, border, color);
        drawSolidRect(poseStack, x - border, y + height, width + 2 * border, border, color);
        drawSolidRect(poseStack, x - border, y, border, height, color);
        drawSolidRect(poseStack, x + width, y, border, height, color);
    }

    private static void drawSolidRect(PoseStack poseStack, int x, int y, int width, int height, int color) {
        fill(poseStack, x, y, x + width, y + height, 0, color);
        RenderSystem.enableBlend();
    }

    private static void fill(PoseStack poseStack, int x1, int y1, int x2, int y2, int z, int color) {
        Matrix4f matrix4f = poseStack.last().pose();
        int i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        float a = (float) FastColor.ARGB32.alpha(color) / 255.0F;
        float r = (float) FastColor.ARGB32.red(color) / 255.0F;
        float g = (float) FastColor.ARGB32.green(color) / 255.0F;
        float b = (float) FastColor.ARGB32.blue(color) / 255.0F;
        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.vertex(matrix4f, (float) x1, (float) y1, (float) z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x1, (float) y2, (float) z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x2, (float) y2, (float) z).color(r, g, b, a).endVertex();
        bufferBuilder.vertex(matrix4f, (float) x2, (float) y1, (float) z).color(r, g, b, a).endVertex();
        BufferUploader.drawWithShader(bufferBuilder.end());
        RenderSystem.disableBlend();
    }

    /*
     * @Override
     * public Object getXEIIngredientOverMouse(double mouseX, double mouseY) {
     * Object result = super.getXEIIngredientOverMouse(mouseX, mouseY);
     * if (result == null && hoverItem != null && !hoverItem.m_41619_()) {
     * if (LDLib.isJeiLoaded()) {
     * return JEIPlugin.getItemIngredient(hoverItem, (int) mouseX, (int) mouseY, 1, 1);
     * }
     * if (LDLib.isReiLoaded()) {
     * return EntryStacks.of(hoverItem);
     * }
     * if (LDLib.isEmiLoaded()) {
     * return EmiStack.of(hoverItem);
     * }
     * return hoverItem;
     * }
     * return result;
     * }
     * 
     * @Override
     * public void receiveMessage(int id, FriendlyByteBuf buf) {
     * if (id == -1) {
     * selectedPosFace = new BlockPosFace(buf.readBlockPos(), buf.readEnum(Direction.class));
     * if (onSelected != null) {
     * onSelected.accept(selectedPosFace.pos, selectedPosFace.facing);
     * }
     * } else {
     * super.receiveMessage(id, buf);
     * }
     * }
     */

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (super.onMouseDown(mouseX, mouseY, button)) {
            return true;
        }
        if (!intractable) return false;
        if (true && isMouseOverElement(mouseX, mouseY)) {
            if (draggable) {
                dragging = true;
            }
            clickPosFace = hoverPosFace;
            return true;
        }
        return false;
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double wheelDelta) {
        var result = super.onMouseScroll(mouseX, mouseY, wheelDelta);
        if (!intractable) return result;
        if (!result && isMouseOverElement(mouseX, mouseY) && scalable) {
            zoom = (float) Mth.clamp(zoom + (wheelDelta < 0 ? 0.5 : -0.5), 0.1, 999);
            if (renderer != null) {
                renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
                renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
            }
            return true;
        }
        return result;
    }

    @Override
    public boolean canFocus(FocusSource source) {
        return true;
    }

    @Override
    public boolean onMouseDrag(double mouseX, double mouseY, double deltaX, double deltaY, int button) {
        if (!intractable) return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
        if (dragging) {
            rotationPitch += (float) (deltaX + 360);
            rotationPitch = rotationPitch % 360;
            rotationYaw = (float) Mth.clamp(rotationYaw + deltaY, -89.9, 89.9);
            if (renderer != null) {
                renderer.setCameraLookAt(center, camZoom(), Math.toRadians(rotationPitch), Math.toRadians(rotationYaw));
            }
            return false;
        }
        return super.onMouseDrag(mouseX, mouseY, deltaX, deltaY, button);
    }

    @Override
    public boolean onMouseUp(double mouseX, double mouseY, int button) {
        if (!intractable) return super.onMouseUp(mouseX, mouseY, button);
        dragging = false;
        if (hoverPosFace != null && hoverPosFace.equals(clickPosFace)) {
            selectedPosFace = hoverPosFace;
            // sendMessage(-1, buffer -> {
            // buffer.writeBlockPos(selectedPosFace.pos);
            // buffer.writeEnum(selectedPosFace.facing);
            // });
            if (onSelected != null) {
                onSelected.accept(selectedPosFace.pos, selectedPosFace.facing);
            }
            clickPosFace = null;
            return true;
        }
        clickPosFace = null;
        return super.onMouseUp(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        super.tick();
    }

    @Override
    public void draw(UIGuiGraphics graphics, int mouseX, int mouseY, float partialTicks, float delta) {
        super.draw(graphics, mouseX, mouseY, partialTicks, delta);

        if (renderer != null) {
            renderer.render(graphics.pose(), x, y, width, height, mouseX, mouseY);
            if (renderer.isCompiling()) {
                double progress = renderer.getCompileProgress();
                if (progress > 0) {
                    // FIXME wtf is this??
                    UITextures
                            .text(Component
                                    .literal("Renderer is compiling! " + String.format("%.1f", progress * 100) + "%%"))
                            .draw(graphics, mouseX, mouseY, x, y, width, height);
                }
            }
        }

        // draw widgets
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        drawChildren(graphics, mouseX, mouseY, partialTicks, delta, this.children());
        currentMouseX = mouseX;
        currentMouseY = mouseY;
    }

    public SceneComponent center(Vector3f center) {
        this.center = center;
        if (renderer != null) {
            renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationPitch),
                    Math.toRadians(rotationYaw));
        }
        return this;
    }

    public SceneComponent zoom(float zoom) {
        this.zoom = zoom;
        if (renderer != null) {
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
            renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationPitch),
                    Math.toRadians(rotationYaw));
        }
        return this;
    }

    public SceneComponent orthoRange(float range) {
        this.range = range;
        if (renderer != null) {
            renderer.setCameraOrtho(range * zoom, range * zoom, range * zoom);
        }
        return this;
    }

    public SceneComponent cameraYawAndPitch(float rotationYaw, float rotationPitch) {
        this.rotationYaw = rotationYaw;
        this.rotationPitch = rotationPitch;
        if (renderer != null) {
            renderer.setCameraLookAt(this.center, camZoom(), Math.toRadians(rotationPitch),
                    Math.toRadians(rotationYaw));
        }
        return this;
    }
}
