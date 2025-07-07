package com.gregtechceu.gtceu.syncdata;

import java.util.ArrayList;
import java.util.List;

public interface IManagedBlockEntity {

    List<SyncDataHolder> managedObjects = new ArrayList<>();

    default void attachDataHolder(SyncDataHolder obj) {
        managedObjects.add(obj);
    }

    default void removeDataHolder(SyncDataHolder obj) {
        managedObjects.remove(obj);
    }
}
