package com.gregtechceu.gtceu.core.mixins.client;

import com.gregtechceu.gtceu.client.mui.screen.RichTooltip;
import com.gregtechceu.gtceu.client.util.RenderUtil;

import com.gregtechceu.gtceu.config.ConfigHolder;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

@Mixin(GuiGraphics.class)
public class GuiGraphicsMixin {

    @WrapMethod(method = "renderItem(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;IIII)V")
    private void gtceu$renderResearchItemContent(@Nullable LivingEntity entity, @Nullable Level level,
                                                 ItemStack stack, int x, int y, int seed, int z,
                                                 Operation<Void> original) {
        if (!RenderUtil.renderResearchItemContent((GuiGraphics) (Object) this, original,
                entity, level, stack, x, y, z, seed)) {
            original.call(entity, level, stack, x, y, seed, z);
        }
    }

    @Inject(method = "renderTooltip(Lnet/minecraft/client/gui/Font;Ljava/util/List;Ljava/util/Optional;II)V", at = @At("HEAD"), cancellable = true)
    private void gtceu$richTooltipInject(Font font, List<Component> tooltipLines, Optional<TooltipComponent> visualTooltipComponent, int mouseX, int mouseY, CallbackInfo ci) {
        if(ConfigHolder.INSTANCE.client.ui.replaceVanillaTooltips && !tooltipLines.isEmpty()) {
            RichTooltip.injectRichTooltip(stack, textLines, visualTooltipComponent, mouseX, mouseY);

            ci.cancel();
        }
    }
}
