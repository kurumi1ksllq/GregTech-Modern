package com.gregtechceu.gtceu.api.machine.feature;

import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public interface IDataStickInteractable {

    default ItemInteractionResult onDataStickShiftUse(Player player, ItemStack dataStick) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    default ItemInteractionResult onDataStickUse(Player player, ItemStack dataStick) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
