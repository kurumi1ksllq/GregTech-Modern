package com.gregtechceu.gtceu.api.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

public interface ContainmentFailureHandler {

    void handleFailure(Level world, BlockPos failingBlock, FluidStack failingStack);

    void handleFailure(Player failingPlayer, FluidStack failingStack);
}
