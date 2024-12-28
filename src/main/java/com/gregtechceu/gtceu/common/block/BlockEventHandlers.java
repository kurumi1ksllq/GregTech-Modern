package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.tterrag.registrate.Registrate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
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

        if (player.onGround()) {

            BlockPos pos = player.blockPosition().below();
            BlockState state = world.getBlockState(pos);
            AttributeInstance movementSpeedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
            if (movementSpeedAttribute != null) {
                if (state.is((GTBlocks.LIGHT_CONCRETE.get()))) {

                    movementSpeedAttribute.setBaseValue(0.3);

                } else {

                    movementSpeedAttribute.setBaseValue(0.1);

                }

            }

        }
    }

}
