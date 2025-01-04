package com.gregtechceu.gtceu.api.graphnet;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.event.IModBusEvent;

import it.unimi.dsi.fastutil.objects.ObjectRBTreeSet;

import java.util.Comparator;

public final class GraphClassRegistrationEvent extends Event implements IModBusEvent {

    private final ObjectRBTreeSet<GraphClassType<?>> gather = new ObjectRBTreeSet<>(
            Comparator.comparing(GraphClassType::getSerializedName));

    public void accept(GraphClassType<?> type) {
        if (!gather.add(type))
            throw new IllegalStateException(
                    "Detected a name collision during Graph Class registration! Collision on name: " +
                            type.getSerializedName());
    }

    ObjectRBTreeSet<GraphClassType<?>> getGather() {
        return gather;
    }
}
