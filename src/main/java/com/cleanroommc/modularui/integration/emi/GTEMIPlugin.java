package com.cleanroommc.modularui.integration.emi;

import com.cleanroommc.modularui.integration.emi.handler.EmiScreenHandler;
import com.cleanroommc.modularui.screen.ContainerScreenWrapper;
import com.cleanroommc.modularui.screen.ScreenWrapper;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

@EmiEntrypoint
public class GTEMIPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        registry.addExclusionArea(ScreenWrapper.class, EmiScreenHandler.of(ScreenWrapper.class));
        registry.addExclusionArea(ContainerScreenWrapper.class, EmiScreenHandler.of(ContainerScreenWrapper.class));
    }
}
