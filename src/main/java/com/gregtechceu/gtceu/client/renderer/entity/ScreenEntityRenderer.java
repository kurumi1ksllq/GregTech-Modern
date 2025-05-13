package com.gregtechceu.gtceu.client.renderer.entity;

import com.gregtechceu.gtceu.api.mui.holoui.HoloScreenEntity;
import com.gregtechceu.gtceu.api.mui.holoui.Plane3D;
import com.gregtechceu.gtceu.api.mui.holoui.ScreenOrientation;
import com.gregtechceu.gtceu.client.mui.screen.GuiContainerWrapper;
import com.gregtechceu.gtceu.core.mixins.GuiGraphicsAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Highly experimental
 */
@ApiStatus.Experimental
public class ScreenEntityRenderer extends EntityRenderer<HoloScreenEntity> {

    private final GuiGraphics guiGraphics;

    public ScreenEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.guiGraphics = new GuiGraphics(Minecraft.getInstance(),
                Minecraft.getInstance().renderBuffers().bufferSource());
    }

    @Override
    public void render(@NotNull HoloScreenEntity entity, float entityYaw, float partialTick,
                       @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight) {
        GuiContainerWrapper screenWrapper = entity.getWrapper();
        if (screenWrapper == null) return;
        PoseStack oldPose = guiGraphics.pose();
        ((GuiGraphicsAccessor) guiGraphics).setPose(poseStack);

        Plane3D plane3D = entity.getPlane3D();
        if (entity.getOrientation() == ScreenOrientation.TO_PLAYER) {
            Player player = Minecraft.getInstance().player;
            float xN = (float) (player.getX() - entity.getX());
            float yN = (float) (player.getY() - entity.getY());
            float zN = (float) (player.getZ() - entity.getZ());
            plane3D.setNormal(xN, yN, zN);
        }
        poseStack.pushPose();
        poseStack.translate(entity.getX(), entity.getY(), entity.getZ());
        plane3D.transformRectangle();
        screenWrapper.render(guiGraphics, 0, 0, partialTick);
        poseStack.popPose();
        ((GuiGraphicsAccessor) guiGraphics).setPose(oldPose);
    }

    @NotNull
    @Override
    public ResourceLocation getTextureLocation(@NotNull HoloScreenEntity entity) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
