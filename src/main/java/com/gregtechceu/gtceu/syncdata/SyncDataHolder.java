package com.gregtechceu.gtceu.syncdata;

import com.gregtechceu.gtceu.syncdata.data_transformers.ValueTransformers;

import net.minecraft.nbt.CompoundTag;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

public class SyncDataHolder {

    public final ClassSyncData syncData;
    private final ISyncManaged holder;

    @Getter
    @Setter
    private boolean clientSyncDirty;
    @Getter
    @Setter
    private boolean saveDirty;

    private boolean hasLoaded;

    public SyncDataHolder(@NotNull ISyncManaged o) {
        holder = o;
        syncData = ClassSyncData.CACHE.get(o.getClass());
    }

    public CompoundTag saveNBT() {
        return (CompoundTag) ValueTransformers.get(ISyncManaged.class).serializeNBT(holder);
    }

    public void loadFromNBT(CompoundTag tag) {
        ValueTransformers.get(ISyncManaged.class).deserializeNBT(tag, holder);
    }

    public void update() {}
}
