package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.item.tool.ToolHelper;
import com.gregtechceu.gtceu.api.item.tool.aoe.AoESymmetrical;
import com.gregtechceu.gtceu.client.shader.GTShaders;
import com.gregtechceu.gtceu.client.util.BloomEffectUtil;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(LevelRenderer.class)
@OnlyIn(Dist.CLIENT)
public abstract class LevelRendererMixin {

    @WrapOperation(method = "applyFrustum",
                   at = @At(value = "INVOKE",
                            target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;add(Ljava/lang/Object;)Z",
                            remap = false))
    private boolean gtceu$compileBloomBuffers(ObjectArrayList<LevelRenderer.RenderChunkInfo> instance,
                                              Object chunkInfo,
                                              Operation<Boolean> original) {
        // cast back to RenderChunkInfo
        BloomEffectUtil.bakeBloomChunkBuffers(((LevelRenderer.RenderChunkInfo) chunkInfo).chunk.getOrigin());
        return original.call(instance, chunkInfo);
    }

    @Inject(method = "resize", at = @At("TAIL"))
    private void gtceu$resize(int width, int height, CallbackInfo ci) {
        if (GTShaders.BLOOM_CHAIN != null) {
            GTShaders.BLOOM_CHAIN.resize(width, height);
        }
    }
}
