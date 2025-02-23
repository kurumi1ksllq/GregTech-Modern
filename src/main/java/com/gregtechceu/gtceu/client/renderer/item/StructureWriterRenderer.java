package com.gregtechceu.gtceu.client.renderer.item;

import com.gregtechceu.gtceu.api.item.ComponentItem;
import com.gregtechceu.gtceu.common.item.debug.BlockPatternPrinter;
import com.gregtechceu.gtceu.common.item.debug.StructureWriterBehaviour;

import com.lowdragmc.lowdraglib.client.utils.RenderBufferUtils;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class StructureWriterRenderer {

    public static void renderSelectionBox(PoseStack pose, Camera camera) {
        var mc = Minecraft.getInstance();
        var player = mc.player;

        if (mc.level == null || player == null) return;

        ItemStack item = player.getMainHandItem();
        if (item.getItem() instanceof ComponentItem compItem &&
                compItem.getComponents().contains(StructureWriterBehaviour.INSTANCE)) {
            int index = item.getOrCreateTag().getInt("index");
            if (index < 3) {
                var look = player.getLookAngle().multiply(3, 3, 3).add(player.getEyePosition());

                BlockPos lookAt = new BlockPos((int) look.x, (int) look.y, (int) look.z);

                Vec3 cameraPos = camera.getPosition();

                pose.pushPose();
                pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

                RenderSystem.disableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.disableCull();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                Tesselator tess = Tesselator.getInstance();
                BufferBuilder buff = tess.getBuilder();

                buff.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                RenderSystem.setShader(GameRenderer::getPositionColorShader);

                RenderBufferUtils.renderCubeFace(pose, buff,
                        lookAt.getX(), lookAt.getY(), lookAt.getZ(),
                        lookAt.getX() + 1, lookAt.getY() + 1, lookAt.getZ() + 1,
                        0.75f, 0.75f, 0.95f, 0.35f, true);

                if(index == 2) {
                    BlockPos min, max;
                    var bps = StructureWriterBehaviour.getMinMax(item);
                    min = bps.getFirst();
                    max = bps.getSecond();

                    BlockPos current = (min.equals(StructureWriterBehaviour.MIN) ? (max.equals(StructureWriterBehaviour.MAX) ? null : max) : min);
                    if(current != null) {
                        RenderBufferUtils.renderCubeFace(pose, buff,
                                current.getX(), current.getY(), current.getZ(),
                                current.getX() + 1, current.getY() + 1, current.getZ() + 1,
                                0.95f, 0.75f, 0.95f, 0.35f, true);
                    }
                }

                tess.end();
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
                pose.popPose();
            } else if (index == 3) {
                BlockPos min, max;
                var bps = StructureWriterBehaviour.getMinMax(item);
                min = bps.getFirst();
                max = bps.getSecond();
                Vec3 cameraPos = camera.getPosition();

                pose.pushPose();
                pose.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

                RenderSystem.disableDepthTest();
                RenderSystem.enableBlend();
                RenderSystem.disableCull();
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA,
                        GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
                Tesselator tess = Tesselator.getInstance();
                BufferBuilder buff = tess.getBuilder();

                buff.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                RenderSystem.setShader(GameRenderer::getPositionColorShader);

                RenderBufferUtils.renderCubeFace(pose, buff,
                        min.getX(), min.getY(), min.getZ(),
                        max.getX() + 1, max.getY() + 1, max.getZ() + 1,
                        0.75f, 0.75f, 0.95f, 0.09f, true);

                tess.end();

                buff.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
                RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);
                RenderSystem.lineWidth(4);

                var dir = StructureWriterBehaviour.getDirection(item);
                var dirs = BlockPatternPrinter.getDirection(dir);
                Matrix4f mat4 = pose.last().pose();
                Matrix3f mat3 = new Matrix3f(mat4);

                drawArrow(buff, mat4, min, mat3, dirs[0].axis, 1.0f, 0.0f, 0.0f, 2.5f);
                drawArrow(buff, mat4, min, mat3, dirs[1].axis, 0.0f, 1.0f, 0.0f, 2.5f);
                drawArrow(buff, mat4, min, mat3, dirs[2].axis, 0.0f, 0.0f, 1.0f, 2.5f);

                tess.end();

                RenderSystem.enableCull();
                RenderSystem.disableBlend();
                RenderSystem.enableDepthTest();
                pose.popPose();
            }
        }
    }

    private static void drawArrow(BufferBuilder buff, Matrix4f pose, BlockPos pos, Matrix3f normal, Direction.Axis axis, float red, float green, float blue, float dist) {
        int x = axis == Direction.Axis.X ? 1 : 0, y = axis == Direction.Axis.Y ? 1 : 0, z = axis == Direction.Axis.Z ? 1 : 0;
        buff.vertex(pose, pos.getX(), pos.getY(), pos.getZ())
                .color(red, green, blue, 0.75f)
                .normal(normal, x, y, z).endVertex();
        buff.vertex(pose,
                pos.getX() + x * dist, pos.getY() + y * dist, pos.getZ() + z * dist)
                .color(red, green, blue, 0.75f)
                .normal(normal, x, y, z).endVertex();
    }
}
