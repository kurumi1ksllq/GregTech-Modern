package com.cleanroommc.modularui;

import com.cleanroommc.modularui.factory.UIFactories;
import com.cleanroommc.modularui.factory.inventory.InventoryTypes;
import com.cleanroommc.modularui.network.MUINetwork;
import com.cleanroommc.modularui.screen.ModularContainerMenu;
import com.cleanroommc.modularui.theme.ThemeManager;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;

public class CommonProxy {

    public CommonProxy() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();
        eventBus.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::registerReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(this::onTick);
        // used for forge events (ClientProxy + CommonProxy)
        ConfigHolder.init();

        /* MUI Initialization */
        UIFactories.init();
        InventoryTypes.init();
    }

    public void init() {
        MUINetwork.init();
        MUIMenuTypes.init();
    }

    @SubscribeEvent
    public void register(RegisterEvent event) {}

    @SubscribeEvent
    public void modConstruct(FMLConstructModEvent event) {
        // this is done to delay initialization of content to be after KJS has set up.
        event.enqueueWork(this::init);
    }

    @SubscribeEvent
    public void commonSetup(FMLCommonSetupEvent event) {}

    @SubscribeEvent
    public void loadComplete(FMLLoadCompleteEvent e) {}

    public void onTick(TickEvent.PlayerTickEvent event) {
        if (event.player.containerMenu instanceof ModularContainerMenu containerMenu) {
            containerMenu.onUpdate();
        }
    }

    public void registerReloadListeners(AddReloadListenerEvent event) {
        ModularUI.updateFrozenRegistry(event.getRegistryAccess());
        if (ModularUI.isClientThread()) {
            event.addListener(new ThemeManager());
        }
    }
}
