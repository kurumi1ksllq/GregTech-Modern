package com.gregtechceu.gtceu.api.machine.feature;

import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.ui.factory.MachineUIFactory;
import com.gregtechceu.gtceu.api.ui.holder.IUIHolder;

import com.lowdragmc.lowdraglib.LDLib;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public interface IUIMachine extends IUIHolder<MetaMachine>, IMachineFeature {

    default boolean shouldOpenUI(Player player, InteractionHand hand, BlockHitResult hit) {
        return true;
    }

    default InteractionResult tryToOpenUI(Player player, InteractionHand hand, BlockHitResult result) {
        if (this.shouldOpenUI(player, hand, result)) {
            if (player instanceof ServerPlayer serverPlayer) {
                MachineUIFactory.INSTANCE.openUI(self(), serverPlayer);
            }
        } else {
            return InteractionResult.PASS;
        }
        return InteractionResult.sidedSuccess(player.level().isClientSide);
    }

    @Override
    default boolean isInvalid() {
        return self().isInValid();
    }

    @Override
    default boolean isClientSide() {
        var level = self().getLevel();
        return level == null ? GTCEu.isClientThread() : level.isClientSide;
    }

    @Override
    default void markDirty() {
        self().markDirty();
    }
}
