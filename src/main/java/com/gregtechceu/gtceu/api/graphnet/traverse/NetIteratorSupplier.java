package com.gregtechceu.gtceu.api.graphnet.traverse;

import com.gregtechceu.gtceu.api.graphnet.net.NetNode;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface NetIteratorSupplier {

    @NotNull
    NetIterator create(@NotNull NetNode origin, @NotNull EdgeSelector direction);
}
