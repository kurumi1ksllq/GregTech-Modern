package com.gregtechceu.gtceu.core.mixins.emi;

import net.minecraft.client.gui.components.events.ContainerEventHandler;

import dev.emi.emi.api.widget.Widget;
import dev.emi.emi.screen.RecipeScreen;
import dev.emi.emi.screen.WidgetGroup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * Sorry, Emi, we're bypassing your interaction restrictions so that our renders' dragging works.
 */
@Mixin(RecipeScreen.class)
public abstract class RecipeScreenMixin {

    @Shadow(remap = false)
    private List<WidgetGroup> currentPage;

    @Inject(method = "charTyped", at = @At(value = "HEAD"), cancellable = true)
    private void gtceu$captureCharTyped(char chr, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        for (var widgetGroup : currentPage) {
            for (Widget widget : widgetGroup.widgets) {
                if (widget instanceof ContainerEventHandler wrapperWidget) {
                    if (wrapperWidget.charTyped(chr, modifiers)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseReleased", at = @At(value = "HEAD"), cancellable = true)
    private void gtceu$captureMouseReleased(double mouseX, double mouseY, int button,
                                            CallbackInfoReturnable<Boolean> cir) {
        for (var widgetGroup : currentPage) {
            for (Widget widget : widgetGroup.widgets) {
                if (widget instanceof ContainerEventHandler wrapperWidget) {
                    if (wrapperWidget.mouseReleased(mouseX, mouseY, button)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseDragged", at = @At(value = "HEAD"), cancellable = true)
    private void gtceu$captureMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY,
                                           CallbackInfoReturnable<Boolean> cir) {
        for (var widgetGroup : currentPage) {
            for (Widget widget : widgetGroup.widgets) {
                if (widget instanceof ContainerEventHandler wrapperWidget) {
                    if (wrapperWidget.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseScrolled", at = @At(value = "HEAD"), cancellable = true)
    private void gtceu$captureMouseScrolled(double mouseX, double mouseY, double amount,
                                            CallbackInfoReturnable<Boolean> cir) {
        for (var widgetGroup : currentPage) {
            for (Widget widget : widgetGroup.widgets) {
                if (widget instanceof ContainerEventHandler wrapperWidget) {
                    if (wrapperWidget.mouseScrolled(mouseX, mouseY, amount)) {
                        cir.setReturnValue(true);
                    }
                }
            }
        }
    }
}
