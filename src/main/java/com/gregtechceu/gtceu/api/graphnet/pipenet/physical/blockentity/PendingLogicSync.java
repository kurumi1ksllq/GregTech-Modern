package com.gregtechceu.gtceu.api.graphnet.pipenet.physical.blockentity;

import com.gregtechceu.gtceu.api.graphnet.logic.NetLogicEntry;

import lombok.Getter;
import lombok.experimental.Accessors;

public final class PendingLogicSync {

    @Accessors(fluent = true)
    @Getter
    private final int networkID;
    @Accessors(fluent = true)
    @Getter
    private final NetLogicEntry<?, ?> entry;
    @Getter
    private boolean removed;
    @Getter
    private boolean fullChange;

    public PendingLogicSync(int networkID, NetLogicEntry<?, ?> entry, boolean removed, boolean fullChange) {
        this.networkID = networkID;
        this.entry = entry;
        this.removed = removed;
        this.fullChange = fullChange;
    }

    public void markUnremoved() {
        this.removed = false;
    }

    public void markRemoved() {
        this.removed = true;
    }

    public void markFullChange() {
        this.fullChange = true;
    }
}
