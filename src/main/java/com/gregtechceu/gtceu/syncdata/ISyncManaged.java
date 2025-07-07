package com.gregtechceu.gtceu.syncdata;

import net.minecraft.nbt.CompoundTag;

public interface ISyncManaged {

    SyncDataHolder getSyncDataHolder();

    default void serializeCustomNBTData(CompoundTag tag, boolean isItemDrop) {}

    default void deserializeCustomNBTData(CompoundTag tag) {}

    void onChanged();
}
