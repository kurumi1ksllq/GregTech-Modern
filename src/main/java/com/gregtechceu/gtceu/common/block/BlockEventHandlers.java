package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.tterrag.registrate.Registrate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;


public class BlockEventHandlers {

    public BlockEventHandlers() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }
        var player = event.player;
        Level world = player.level();

        if(player.onGround())
        {

            BlockPos pos = player.blockPosition().below();
            BlockState state = world.getBlockState(pos);

            if(state.is((GTBlocks.LIGHT_CONCRETE.get())))
            {

                player.getAbilities().setWalkingSpeed(0.8f);

            }
            else
            {

                player.getAbilities().setWalkingSpeed(0.1f);

            }

        }

    }

}
