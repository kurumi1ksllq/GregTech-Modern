package com.gregtechceu.gtceu.sync_system;

public interface IManagedBlockEntity {

    ISyncManaged getManagedObject();

    void onSaveDirty();
}
