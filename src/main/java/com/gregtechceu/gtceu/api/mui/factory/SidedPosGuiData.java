package com.gregtechceu.gtceu.api.mui.factory;

import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;

/**
 * See {@link GuiData} for an explanation for what this is for.
 */
public class SidedPosGuiData extends PosGuiData {

    @Getter
    private final Direction side;

    public SidedPosGuiData(Player player, int x, int y, int z, Direction side) {
        super(player, x, y, z);
        this.side = side;
    }
}
