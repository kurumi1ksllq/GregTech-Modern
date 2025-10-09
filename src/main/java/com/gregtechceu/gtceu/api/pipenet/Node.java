package com.gregtechceu.gtceu.api.pipenet;

import net.minecraft.core.Direction;

/**
 * Represents a single node in network of pipes
 * It can have blocked connections and be active or not
 */
public final class Node {

    public static final int ALL_OPENED = 0b111111;
    public static final int ALL_CLOSED = 0b000000;

    public int openConnections;
    public boolean isActive;

    public Node(int openConnections, boolean isActive) {
        this.openConnections = openConnections;
        this.isActive = isActive;
    }

    public boolean isBlocked(Direction facing) {
        return (openConnections & 1 << facing.ordinal()) == 0;
    }
}
