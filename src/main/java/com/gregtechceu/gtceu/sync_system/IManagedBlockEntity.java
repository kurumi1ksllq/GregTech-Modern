package com.gregtechceu.gtceu.sync_system;

import java.util.ArrayList;
import java.util.List;

/**
 * A block entity onto which data is stored for multiple ISyncManaged objects
 * @see ISyncManaged
 */
public interface IManagedBlockEntity {

    List<ISyncManaged> managedSyncObjects = new ArrayList<>();

    default void attachManagedObject(ISyncManaged obj) {
        managedSyncObjects.add(obj);
    }

    default void removeManagedObject(ISyncManaged obj) {
        managedSyncObjects.remove(obj);
    }
}
