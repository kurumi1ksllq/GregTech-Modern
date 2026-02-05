package com.cleanroommc.modularui.integration.jei;

import com.cleanroommc.modularui.ModularUI;
import com.cleanroommc.modularui.integration.jei.handler.JEIContainerHandler;
import com.cleanroommc.modularui.integration.jei.handler.JEIScreenHandler;
import com.cleanroommc.modularui.screen.ContainerScreenWrapper;
import com.cleanroommc.modularui.screen.ScreenWrapper;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;

import lombok.Getter;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.runtime.IJeiRuntime;

import javax.annotation.ParametersAreNonnullByDefault;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
@JeiPlugin
public class GTJEIPlugin implements IModPlugin {

    @Getter
    private static IJeiRuntime runtime = null;

    @Override
    public ResourceLocation getPluginUid() {
        return ModularUI.id("jei_plugin");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }


    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        //TODO if (ModularUI.Mods.isREILoaded() || ModularUI.Mods.isEMILoaded()) return;
        registration.addGhostIngredientHandler(ScreenWrapper.class, JEIScreenHandler.of(ScreenWrapper.class));
        registration.addGhostIngredientHandler(ContainerScreenWrapper.class,
                JEIScreenHandler.of(ContainerScreenWrapper.class));
        registration.addGuiContainerHandler(ContainerScreenWrapper.class, JEIContainerHandler.INSTANCE);
    }
}
