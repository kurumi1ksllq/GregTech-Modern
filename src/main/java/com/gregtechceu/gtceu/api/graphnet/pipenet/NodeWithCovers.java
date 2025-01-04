package com.gregtechceu.gtceu.api.graphnet.pipenet;

import com.gregtechceu.gtceu.api.capability.ICoverable;

import org.jetbrains.annotations.Nullable;

public interface NodeWithCovers {

    @Nullable
    ICoverable getCoverable();
}
