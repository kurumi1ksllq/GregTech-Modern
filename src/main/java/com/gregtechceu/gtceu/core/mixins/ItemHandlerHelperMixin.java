package com.gregtechceu.gtceu.core.mixins;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.item.component.ISpoilableItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ItemHandlerHelper.class, remap = false)
public class ItemHandlerHelperMixin {
    @Inject(method = "canItemStacksStack", at = @At("HEAD"), cancellable = true)
    private static void gtceu$canItemStacksStack(ItemStack a, ItemStack b, CallbackInfoReturnable<Boolean> cir) {
        ISpoilableItem spoilable = GTCapabilityHelper.getSpoilable(a);
        if (spoilable != null) {
            spoilable.isEqualTo(b).ifPresent(cir::setReturnValue);
        } else {
            ISpoilableItem spoilable2 = GTCapabilityHelper.getSpoilable(b);
            if (spoilable2 != null) {
                spoilable2.isEqualTo(a).ifPresent(cir::setReturnValue);
            }
        }
    }
}
