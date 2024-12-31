package com.gregtechceu.gtceu.common.block;

import com.gregtechceu.gtceu.common.data.GTBlocks;
import com.tterrag.registrate.Registrate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class BlockEventHandlers {

    public BlockEventHandlers() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    private final Map<Block, Double> SpeedModifiers = new HashMap<>();
    private static final UUID MOVEMENT_SPEED_MODIFIER_ID = UUID.fromString("b14c1720-b06f-40f6-98fd-625af4ed1076");

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            return;
        }

        var player = event.player;
        Level world = player.level();
        AttributeInstance movementSpeedAttribute = player.getAttribute(Attributes.MOVEMENT_SPEED);

        if (player.onGround() && movementSpeedAttribute != null) {
            BlockPos pos = player.blockPosition().below();
            BlockState state = world.getBlockState(pos);

            movementSpeedAttribute.removeModifier(MOVEMENT_SPEED_MODIFIER_ID);


                Block block = state.getBlock();
                if (SpeedModifiers.containsKey(block)) {
                    double speed = SpeedModifiers.get(block);

                    AttributeModifier speedModifier = new AttributeModifier(
                            MOVEMENT_SPEED_MODIFIER_ID,
                            "Movement Speed Modifier",
                            speed,
                            AttributeModifier.Operation.ADDITION
                    );
                    movementSpeedAttribute.addPermanentModifier(speedModifier);
                }


            }

        }

}
