package com.gregtechceu.gtceu.api.mui.value.sync;

import net.minecraft.network.FriendlyByteBuf;

import java.io.IOException;

public class CursorSlotSyncHandler extends SyncHandler {

    public void sync() {
        sync(0, buffer -> buffer.writeItemStack(getSyncManager().getPlayer().inventory.getCarried()));
    }

    @Override
    public void readOnClient(int id, FriendlyByteBuf buf) throws IOException {
        getSyncManager().getPlayer().inventory.setCarried(buf.readItemStack());
    }

    @Override
    public void readOnServer(int id, FriendlyByteBuf buf) throws IOException {
        getSyncManager().getPlayer().inventory.setCarried(buf.readItemStack());
    }
}
