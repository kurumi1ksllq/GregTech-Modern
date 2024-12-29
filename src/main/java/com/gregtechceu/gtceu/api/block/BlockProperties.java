package com.gregtechceu.gtceu.api.block;

import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * BlockProperties is a utility class that contains custom block properties.
 * This is useful for blocks that have custom properties that are not included in the default properties.
 * For example, a block that has a property that determines whether the block should tick on the server.
 * @see BooleanProperty
 */
public final class BlockProperties {
    // Properties
    public static final BooleanProperty SERVER_TICK = BooleanProperty.create("server_tick");
}