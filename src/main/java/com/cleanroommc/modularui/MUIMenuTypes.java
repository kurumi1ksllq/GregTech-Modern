package com.cleanroommc.modularui;

import com.cleanroommc.modularui.screen.ContainerScreenWrapper;
import com.cleanroommc.modularui.screen.ModularContainerMenu;

import com.tterrag.registrate.util.entry.MenuEntry;

public class MUIMenuTypes {

    @SuppressWarnings("deprecation")
    public static final MenuEntry<ModularContainerMenu> MODULAR_CONTAINER = ModularUI.REG.get()
            .<ModularContainerMenu, ContainerScreenWrapper>menu("modular",
                    ModularContainerMenu::new, () -> ContainerScreenWrapper::new)
            .register();

    public static void init() {}
}
