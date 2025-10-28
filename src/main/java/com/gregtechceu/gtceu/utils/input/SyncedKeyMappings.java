package com.gregtechceu.gtceu.utils.input;

import com.gregtechceu.gtceu.GTCEu;

import com.gregtechceu.gtceu.api.gui.ModuleUIHolder;
import com.gregtechceu.gtceu.api.gui.factory.ModuleUIFactory;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoader;

public class SyncedKeyMappings {

    public static final SyncedKeyMapping VANILLA_JUMP = SyncedKeyMapping
            .createFromMC(() -> () -> Minecraft.getInstance().options.keyJump);
    public static final SyncedKeyMapping VANILLA_SNEAK = SyncedKeyMapping
            .createFromMC(() -> () -> Minecraft.getInstance().options.keyShift);
    public static final SyncedKeyMapping VANILLA_FORWARD = SyncedKeyMapping
            .createFromMC(() -> () -> Minecraft.getInstance().options.keyUp);
    public static final SyncedKeyMapping VANILLA_BACKWARD = SyncedKeyMapping
            .createFromMC(() -> () -> Minecraft.getInstance().options.keyDown);
    public static final SyncedKeyMapping VANILLA_LEFT = SyncedKeyMapping
            .createFromMC(() -> () -> Minecraft.getInstance().options.keyLeft);
    public static final SyncedKeyMapping VANILLA_RIGHT = SyncedKeyMapping
            .createFromMC(() -> () -> Minecraft.getInstance().options.keyRight);
    public static final SyncedKeyMapping OPEN_MODULE_GUI = SyncedKeyMapping.createConfigurable(
            "openModuleGui",
            KeyConflictContext.IN_GAME,
            InputConstants.KEY_M
    );

    public static void init() {
        if (GTCEu.isClientSide()) {
            MinecraftForge.EVENT_BUS.register(SyncedKeyMapping.class);
        }
        OPEN_MODULE_GUI.registerGlobalListener((player, key, isDown) -> {
            if (key == OPEN_MODULE_GUI) ModuleUIFactory.INSTANCE.openUI(new ModuleUIHolder(player), player);
        });
        ModLoader.get().postEvent(new SyncedKeyMappingEvent());
    }
}
