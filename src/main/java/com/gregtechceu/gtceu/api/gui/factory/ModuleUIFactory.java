package com.gregtechceu.gtceu.api.gui.factory;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.gui.ModuleUIHolder;
import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

public class ModuleUIFactory extends UIFactory<ModuleUIHolder> {
    public static final ModuleUIFactory INSTANCE = new ModuleUIFactory();

    public ModuleUIFactory() {
        super(GTCEu.id("module"));
    }

    @Override
    protected ModularUI createUITemplate(ModuleUIHolder moduleUIHolder, Player player) {
        return moduleUIHolder.createUI(player);
    }

    @Override
    protected ModuleUIHolder readHolderFromSyncData(FriendlyByteBuf friendlyByteBuf) {
        return ModuleUIHolder.fromByteBuf(friendlyByteBuf);
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf friendlyByteBuf, ModuleUIHolder moduleUIHolder) {
        moduleUIHolder.writeToByteBuf(friendlyByteBuf);
    }
}
