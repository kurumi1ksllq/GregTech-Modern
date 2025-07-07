package com.gregtechceu.gtceu.syncdata;

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

    public void update() {}
}
