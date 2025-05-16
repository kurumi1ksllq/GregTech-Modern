package com.gregtechceu.gtceu.core.mixins.jei;

import com.gregtechceu.gtceu.integration.jei.handler.JEIScreenHandler;
import com.llamalad7.mixinextras.sugar.Local;
import mezz.jei.api.registration.IRuntimeRegistration;
import mezz.jei.gui.input.handlers.DragRouter;
import mezz.jei.gui.startup.JeiEventHandlers;
import mezz.jei.gui.startup.JeiGuiStarter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = JeiGuiStarter.class, remap = false)
public class JeiGuiStarterMixin {

    /**
     * I don't like this at all, but (as far as I can find) there's no better way.
     * @author screret
     */
    @Inject(method = "start", at = @At("RETURN"))
    private static void gtceu$captureDragRouter(IRuntimeRegistration registration,
                                                CallbackInfoReturnable<JeiEventHandlers> cir,
                                                @Local DragRouter dragRouter) {
        JEIScreenHandler.dragRouter = dragRouter;
    }
}
