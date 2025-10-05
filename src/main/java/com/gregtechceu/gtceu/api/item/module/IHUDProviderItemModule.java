package com.gregtechceu.gtceu.api.item.module;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface IHUDProviderItemModule {

    @OnlyIn(Dist.CLIENT)
    default boolean shouldDrawHUD(AppliedItemModule module) {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    void drawHUD(AppliedItemModule module, GuiGraphics graphics);

    @OnlyIn(Dist.CLIENT)
    static void tryDrawHUD(ItemStack stack, GuiGraphics graphics) {
        if (stack == null || stack.isEmpty()) return;
        for (AppliedItemModule module : AppliedItemModule.getAppliedModules(stack)) {
            if (module instanceof IHUDProviderItemModule hudProvider) {
                if (hudProvider.shouldDrawHUD(module)) hudProvider.drawHUD(module, graphics);
            }
        }
    }
}
