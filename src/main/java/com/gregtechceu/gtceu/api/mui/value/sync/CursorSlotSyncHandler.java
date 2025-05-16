package com.gregtechceu.gtceu.api.mui.value.sync;

import net.minecraft.network.FriendlyByteBuf;

import java.io.IOException;

public class CursorSlotSyncHandler extends SyncHandler {

    public void sync() {
        sync(0, buffer -> buffer.writeItem(getSyncManager().getPlayer().inventoryMenu.getCarried()));
    }

    @Override
    public void readOnClient(int id, FriendlyByteBuf buf) {
        getSyncManager().getPlayer().inventoryMenu.setCarried(buf.readItem());
    }

    @Override
    public void readOnServer(int id, FriendlyByteBuf buf) {
        getSyncManager().getPlayer().inventoryMenu.setCarried(buf.readItem());
    }
}
