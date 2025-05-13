package com.gregtechceu.gtceu.api.mui.utils.fakeworld;

import com.gregtechceu.gtceu.api.mui.base.drawable.IDrawable;
import com.gregtechceu.gtceu.client.mui.screen.viewport.GuiContext;
import com.gregtechceu.gtceu.api.mui.theme.WidgetTheme;
import com.gregtechceu.gtceu.api.mui.utils.Color;
import com.gregtechceu.gtceu.api.mui.widget.sizer.Area;
import com.mojang.blaze3d.vertex.BufferBuilder;
import net.minecraft.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.BlockEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormat;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;
import org.lwjgl.opengl.EXTFramebufferObject;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.vector.Vector3f;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

public class SchemaRenderer implements IDrawable {

    private static final Framebuffer FBO = new Framebuffer(1080, 1080, true);

    private final ISchema schema;
    private final IBlockAccess renderWorld;
    private final Framebuffer framebuffer;
    private final Camera camera = new Camera(new Vector3f(), new Vector3f());
    private boolean cameraSetup = false;
    private DoubleSupplier scale;
    private BooleanSupplier disableTESR;
    private Consumer<IRayTracer> onRayTrace;
    private Consumer<Projection> afterRender;
    private BiConsumer<Camera, ISchema> cameraFunc;
    private int clearColor = 0;
    private boolean isometric = false;

    public SchemaRenderer(ISchema schema, Framebuffer framebuffer) {
        this.schema = schema;
        this.framebuffer = framebuffer;
        this.renderWorld = new RenderWorld(schema);
    }

    public SchemaRenderer(ISchema schema) {
        this(schema, FBO);
    }

    public SchemaRenderer cameraFunc(BiConsumer<Camera, ISchema> camera) {
        this.cameraFunc = camera;
        return this;
    }

    public SchemaRenderer onRayTrace(Consumer<IRayTracer> consumer) {
        this.onRayTrace = consumer;
        return this;
    }

    public SchemaRenderer afterRender(Consumer<Projection> consumer) {
        this.afterRender = consumer;
        return this;
    }

    public SchemaRenderer isometric(boolean isometric) {
        this.isometric = isometric;
        return this;
    }

    public SchemaRenderer scale(double scale) {
        return scale(() -> scale);
    }

    public SchemaRenderer scale(DoubleSupplier scale) {
        this.scale = scale;
        return this;
    }

    public SchemaRenderer disableTESR(boolean disable) {
        return disableTESR(() -> disable);
    }

    public SchemaRenderer disableTESR(BooleanSupplier disable) {
        this.disableTESR = disable;
        return this;
    }

    @Override
    public void draw(GuiContext context, int x, int y, int width, int height, WidgetTheme widgetTheme) {
        render(x, y, width, height, context.getMouseX(), context.getMouseY());
    }

    public void render(int x, int y, int width, int height, int mouseX, int mouseY) {
        if (this.cameraFunc != null) {
            this.cameraFunc.accept(this.camera, this.schema);
        }
        if (Objects.nonNull(scale)) {
            Vector3f cameraPos = camera.getPos();
            Vector3f looking = camera.getLookAt();
            Vector3f.sub(cameraPos, looking, cameraPos);
            if (cameraPos.length() != 0.0f) cameraPos.normalise();
            cameraPos.scale((float) scale.getAsDouble());
            Vector3f.add(looking, cameraPos, cameraPos);
        }
        int lastFbo = bindFBO();
        setupCamera(this.framebuffer.framebufferWidth, this.framebuffer.framebufferHeight);
        renderWorld();
        if (this.onRayTrace != null && Area.isInside(x, y, width, height, mouseX, mouseY)) {
            this.onRayTrace.accept(new IRayTracer() {
                @Override
                public RayTraceResult rayTrace(int screenX, int screenY) {
                    return SchemaRenderer.this.rayTrace(Projection.INSTANCE.unProject(screenX, screenY));
                }

                @Override
                public RayTraceResult rayTraceMousePos() {
                    return rayTrace(mouseX, mouseY);
                }
            });
        }
        resetCamera();
        unbindFBO(lastFbo);

        // bind FBO as texture
        RenderSystem.enableTexture2D();
        lastFbo = GL11.glGetInteger(GL11.GL_TEXTURE_2D);
        RenderSystem.bindTexture(this.framebuffer.framebufferTexture);
        RenderSystem.color(1, 1, 1, 1);

        // render rect with FBO texture
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferbuilder = tesselator.getBuilder();
        bufferbuilder.begin(7, DefaultVertexFormat.POSITION_TEX);

        bufferbuilder.vertex(x + width, y + height, 0).tex(1, 0).endVertex();
        bufferbuilder.vertex(x + width, y, 0).tex(1, 1).endVertex();
        bufferbuilder.vertex(x, y, 0).tex(0, 1).endVertex();
        bufferbuilder.vertex(x, y + height, 0).tex(0, 0).endVertex();
        tesselator.end();

        RenderSystem.bindTexture(lastFbo);
    }

    private void renderWorld() {
        Minecraft mc = Minecraft.getInstance();
        RenderSystem.enableCull();
        Lighting.setupForFlatItems();
        mc.entityRenderer.disableLightmap();
        mc.renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        BlockRenderLayer oldRenderLayer = MinecraftForgeClient.getRenderLayer();
        RenderSystem.enableTexture2D();

        try { // render block in each layer
            for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                ForgeHooksClient.setRenderLayer(layer);
                int pass = layer == BlockRenderLayer.TRANSLUCENT ? 1 : 0;
                setDefaultPassRenderState(pass);
                BufferBuilder buffer = Tesselator.getInstance().getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormat.BLOCK);
                BlockRendererDispatcher blockrendererdispatcher = mc.getBlockRendererDispatcher();
                this.schema.forEach(pair -> {
                    BlockPos pos = pair.getKey();
                    BlockState state = pair.getValue().getBlockState();
                    if (!state.getBlock().isAir(state, this.renderWorld, pos) && state.getBlock().canRenderInLayer(state, layer)) {
                        blockrendererdispatcher.renderBlock(state, pos, this.renderWorld, buffer);
                    }
                });
                Tesselator.getInstance().draw();
                Tesselator.getInstance().getBuffer().setTranslation(0, 0, 0);
            }
        } finally {
            ForgeHooksClient.setRenderLayer(oldRenderLayer);
        }

        Lighting.setupFor3DItems();

        // render TESR
        if (disableTESR == null || !disableTESR.getAsBoolean()) {
            for (int pass = 0; pass < 2; pass++) {
                ForgeHooksClient.setRenderPass(pass);
                int finalPass = pass;
                RenderSystem.color(1, 1, 1, 1);
                setDefaultPassRenderState(pass);
                this.schema.forEach(pair -> {
                    BlockPos pos = pair.getKey();
                    BlockEntity tile = pair.getValue().getBlockEntity();
                    if (tile != null && tile.shouldRenderInPass(finalPass)) {
                        BlockEntityRendererDispatcher.instance.render(tile, pos.getX(), pos.getY(), pos.getZ(), 0);
                    }
                });
            }
        }
        ForgeHooksClient.setRenderPass(-1);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        if (this.afterRender != null) {
            this.afterRender.accept(Projection.INSTANCE);
        }
    }

    private static void setDefaultPassRenderState(int pass) {
        RenderSystem.color(1, 1, 1, 1);
        if (pass == 0) { // SOLID
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        } else { // TRANSLUCENT
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            RenderSystem.depthMask(false);
        }
    }

    protected void setupCamera(int width, int height) {
        //RenderSystem.pushAttrib();

        Minecraft.getInstance().entityRenderer.disableLightmap();
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();

        // setup viewport and clear GL buffers
        RenderSystem.viewport(0, 0, width, height);
        Color.setGlColor(clearColor);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);

        // setup projection matrix to perspective
        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();

        float near = this.isometric ? 1f : 0.1f;
        float far = 10000.0f;
        float fovY = 60.0f; // Field of view in the Y direction
        float aspect = (float) width / height; // width and height are the dimensions of your window
        float top = near * (float) Math.tan(Math.toRadians(fovY) / 2.0);
        float bottom = -top;
        float left = aspect * bottom;
        float right = aspect * top;
        if (this.isometric) {
            GL11.glOrtho(left, right, bottom, top, near, far);
        } else {
            GL11.glFrustum(left, right, bottom, top, near, far);
        }

        // setup modelview matrix
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        if (this.isometric) {
            RenderSystem.scale(0.1, 0.1, 0.1);
        }
        var c = this.camera.getPos();
        var lookAt = this.camera.getLookAt();
        GLU.gluLookAt(c.x, c.y, c.z, lookAt.x, lookAt.y, lookAt.z, 0, 1, 0);
        this.cameraSetup = true;
    }

    protected void resetCamera() {
        this.cameraSetup = false;
        // reset viewport
        Minecraft minecraft = Minecraft.getInstance();
        RenderSystem.viewport(0, 0, minecraft.displayWidth, minecraft.displayHeight);

        // reset projection matrix
        RenderSystem.matrixMode(GL11.GL_PROJECTION);
        RenderSystem.popMatrix();

        // reset modelview matrix
        RenderSystem.matrixMode(GL11.GL_MODELVIEW);
        RenderSystem.popMatrix();

        RenderSystem.disableBlend();
        RenderSystem.disableDepthTest();

        // reset attributes
        // RenderSystem.popAttrib();
    }

    private int bindFBO() {
        int lastID = GL11.glGetInteger(EXTFramebufferObject.GL_FRAMEBUFFER_BINDING_EXT);
        this.framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        this.framebuffer.framebufferClear();
        this.framebuffer.bindFramebuffer(true);
        RenderSystem.pushMatrix();
        return lastID;
    }

    private void unbindFBO(int lastID) {
        RenderSystem.popMatrix();
        this.framebuffer.unbindFramebufferTexture();
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, lastID);
    }

    private RayTraceResult rayTrace(Vector3f hitPos) {
        Vec3d startPos = new Vec3d(this.camera.getPos().x, this.camera.getPos().y, this.camera.getPos().z);
        hitPos.scale(2); // Double view range to ensure pos can be seen.
        Vec3d endPos = new Vec3d((hitPos.x - startPos.x), (hitPos.y - startPos.y), (hitPos.z - startPos.z));
        return this.schema.getLevel().rayTraceBlocks(startPos, endPos);
    }

    public boolean isCameraSetup() {
        return cameraSetup;
    }

    public interface IRayTracer {

        RayTraceResult rayTrace(int screenX, int screenY);

        RayTraceResult rayTraceMousePos();
    }

    public interface ICamera {

        void setupCamera(Vector3f cameraPos, Vector3f lookAt);
    }
}
