package com.gregtechceu.gtceu.common.mui.widgets;

import com.gregtechceu.gtceu.api.mui.base.drawable.IDrawable;
import com.gregtechceu.gtceu.api.mui.base.drawable.IKey;
import com.gregtechceu.gtceu.api.mui.base.widget.Interactable;
import com.gregtechceu.gtceu.api.mui.drawable.GuiTextures;
import com.gregtechceu.gtceu.api.mui.utils.VectorUtil;
import com.gregtechceu.gtceu.api.mui.utils.fakeworld.ISchema;
import com.gregtechceu.gtceu.api.mui.utils.fakeworld.SchemaRenderer;
import com.gregtechceu.gtceu.api.mui.widget.Widget;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.util.vector.Vector3f;

public class SchemaWidget extends Widget<SchemaWidget> implements Interactable {

    public static final float PI = (float) Math.PI;
    public static final float PI2 = 2 * PI;

    private final SchemaRenderer schema;
    private boolean enableRotation = true;
    private boolean enableTranslation = true;
    private boolean enableScaling = true;
    private int lastMouseX;
    private int lastMouseY;
    private double scale = 10;
    private float pitch = (float) (Math.PI / 4f);
    private float yaw = (float) (Math.PI / 4f);
    private final Vector3f offset = new Vector3f();

    public SchemaWidget(ISchema schema) {
        this(new SchemaRenderer(schema));
    }

    public SchemaWidget(SchemaRenderer schema) {
        this.schema = schema;
        schema.cameraFunc((camera, $schema) -> {
            Vector3f focus = VectorUtil.vec3fAdd(this.offset, null, $schema.getFocus());
            camera.setLookAt(focus, scale, yaw, pitch);
        });
    }

    @Override
    public boolean onMouseScroll(double mouseX, double mouseY, double delta) {
        if (this.enableScaling) {
            scale(-mouseX.modifier * delta / 120.0);
            return true;
        }
        return false;
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        this.lastMouseX = getContext().getMouseX();
        this.lastMouseY = getContext().getMouseY();
        return Result.SUCCESS;
    }

    @Override
    public void onMouseDrag(double mouseX, double mouseY, int button, double dragX, double dragY) {
        int mouseX = getContext().getMouseX();
        int mouseY = getContext().getMouseY();
        int dx = mouseX - lastMouseX;
        int dy = mouseY - lastMouseY;
        if (mouseX == 0 && this.enableRotation) {
            float moveScale = 0.025f;
            yaw = (yaw + dx * moveScale + PI2) % PI2;
            pitch = MathHelper.clamp(pitch + dy * moveScale, -PI2 / 4 + 0.001f, PI2 / 4 - 0.001f);
        } else if (mouseX == 2 && this.enableTranslation) {
            // the idea is to construct a vector which points upwards from the camerae pov (y-axis on screen)
            // this vector determines the amount of z offset from mouse movement in y
            float y = (float) Math.cos(pitch);
            float moveScale = 0.06f;
            // with this the offset can be moved by dy
            offset.translate(0, dy * y * moveScale, 0);
            // to respect dx we need a new vector which is perpendicular on the previous vector (x-axis on screen)
            // y = 0 => mouse movement in x does not move y
            float phi = (yaw + PI / 2) % PI2;
            float x = (float) Math.cos(phi);
            float z = (float) Math.sin(phi);
            offset.translate(dx * x * moveScale, 0, dx * z * moveScale);
        }
        this.lastMouseX = mouseX;
        this.lastMouseY = mouseY;
    }

    public SchemaWidget scale(double scale) {
        this.scale += scale;
        return this;
    }

    public SchemaWidget pitch(float pitch) {
        this.pitch += pitch;
        return this;
    }

    public SchemaWidget yaw(float yaw) {
        this.yaw += yaw;
        return this;
    }

    public SchemaWidget offset(float x, float y, float z) {
        this.offset.set(x, y, z);
        return this;
    }

    public SchemaWidget enableDragRotation(boolean enable) {
        this.enableRotation = enable;
        return this;
    }

    public SchemaWidget enableDragTranslation(boolean enable) {
        this.enableTranslation = enable;
        return this;
    }

    public SchemaWidget enableScrollScaling(boolean enable) {
        this.enableScaling = enable;
        return this;
    }

    public SchemaWidget enableInteraction(boolean rotation, boolean translation, boolean scaling) {
        return enableDragRotation(rotation)
                .enableDragTranslation(translation)
                .enableScrollScaling(scaling);
    }

    public SchemaWidget enableAllInteraction(boolean enable) {
        return enableInteraction(enable, enable, enable);
    }

    @Override
    public @Nullable IDrawable getOverlay() {
        return schema;
    }

    public static class LayerButton extends ButtonWidget<LayerButton> {

        private final int minLayer;
        private final int maxLayer;
        private int currentLayer = Integer.MIN_VALUE;

        public LayerButton(ISchema schema, int minLayer, int maxLayer) {
            this.minLayer = minLayer;
            this.maxLayer = maxLayer;
            background(GuiTextures.MC_BACKGROUND);
            overlay(IKey.dynamic(() -> currentLayer > Integer.MIN_VALUE ? Integer.toString(currentLayer) : "ALL").scale(0.5f));

            onMousePressed(mouseButton -> {
                if (mouseButton == 0 || mouseButton == 1) {
                    if (mouseButton == 0) {
                        if (currentLayer == Integer.MIN_VALUE) {
                            currentLayer = minLayer;
                        } else {
                            currentLayer++;
                        }
                    } else {
                        if (currentLayer == Integer.MIN_VALUE) {
                            currentLayer = maxLayer;
                        } else {
                            currentLayer--;
                        }
                    }
                    if (currentLayer > maxLayer || currentLayer < minLayer) {
                        currentLayer = Integer.MIN_VALUE;
                    }
                    return true;
                }
                return false;
            });
            schema.setRenderFilter((blockPos, blockInfo) -> currentLayer == Integer.MIN_VALUE || currentLayer >= blockPos.getY());
        }

        public LayerButton startLayer(int start) {
            this.currentLayer = start;
            return this;
        }
    }
}
