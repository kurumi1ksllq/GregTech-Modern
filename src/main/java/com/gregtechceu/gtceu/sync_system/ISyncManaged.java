package com.gregtechceu.gtceu.sync_system;

import net.minecraft.nbt.CompoundTag;

/**
 * An object which has fields that should be synced to clients and saved on the server.
 * Must be attached to an {@code IManagedBlockEntity}
 */
public interface ISyncManaged {

    /**
     * Get the data holder for this object.
     * Easiest way to implement is by adding it as a field to a class:
     * {@code @Getter private final SyncDataHolder syncDataStorage = new SyncDataHolder(this);}
     * @return {@code SyncDataHolder} holder
     */
    SyncDataHolder getSyncDataStorage();

    void onChanged();

    default void saveExtendedNBTData(CompoundTag tag, boolean isDrop) {}
    default void loadExtendedNBTData(CompoundTag tag) {}
}
