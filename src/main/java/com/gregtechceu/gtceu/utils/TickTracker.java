package com.gregtechceu.gtceu.utils;

import com.gregtechceu.gtceu.GTCEu;

import lombok.Setter;
import org.jetbrains.annotations.ApiStatus;

/**
 * Utility class caching the current server tick in a slightly more lightweight class, instead of going through
 * FMLCommonHandler.instance().getMinecraftServerInstance().getTickCounter() every time.
 */
public class TickTracker {

    @Setter
    @ApiStatus.Internal
    private static int clientTick;
    @ApiStatus.Internal
    private static int serverTick;

    public static int getTick() {
        if (GTCEu.isClientThread()) {
            return clientTick;
        } else {
            return serverTick;
        }
    }

    /**
     * Should only be called on {@link net.minecraftforge.event.TickEvent.ServerTickEvent}
     * {@link net.minecraftforge.event.TickEvent.Phase#START}
     */
    @ApiStatus.Internal
    public static void updateServer() {
        serverTick = GTCEu.getMinecraftServer().getTickCount() + 1;
    }

    /**
     * Should only be called on {@link net.minecraftforge.event.TickEvent.ClientTickEvent}
     * {@link net.minecraftforge.event.TickEvent.Phase#START}
     */
    @ApiStatus.Internal
    public static void updateClient() {
        clientTick++;
    }
}
