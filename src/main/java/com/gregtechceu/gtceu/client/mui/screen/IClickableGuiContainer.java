package com.gregtechceu.gtceu.client.mui.screen;

import net.minecraft.world.inventory.Slot;

public interface IClickableGuiContainer {

    void modularUI$setClickedSlot(Slot slot);

    Slot modularUI$getClickedSlot();
}
