package com.gregtechceu.gtceu.common.gui.widget;

import com.gregtechceu.gtceu.api.gui.widget.BlockableSlotWidget;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.item.armor.ArmorComponentItem;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.world.item.ItemStack;

public class EquipmentFoundryBaseWidget extends WidgetGroup {

    private final CustomItemStackHandler equipmentSlot;
    private final CustomItemStackHandler modifierSlots;

    private WidgetGroup slotGroup;

    public EquipmentFoundryBaseWidget(int x, int y, int width, int height,
                                      CustomItemStackHandler equipmentSlot,
                                      CustomItemStackHandler modifierSlots) {
        super(x, y, width, height);
        this.equipmentSlot = equipmentSlot;
        this.modifierSlots = modifierSlots;

        addWidget(new BlockableSlotWidget(equipmentSlot, 0, 20, 20)
                .setIsBlocked(() -> !equipmentSlot.getStackInSlot(0).isEmpty())
                .setChangeListener(this::onEquipmentItemChanged));
    }

    public void onEquipmentItemChanged() {
        ItemStack stack = equipmentSlot.getStackInSlot(0);
        if (stack.getItem() instanceof ArmorComponentItem armorComponentItem) {
            this.removeWidget(slotGroup);

            // TODO implement modification

            this.slotGroup = new WidgetGroup(0, 0, 0, 0);
            for (int i = 0; i < armorComponentItem.getMaxModifiers(); i++) {
                SlotWidget slot = new SlotWidget(modifierSlots, i, 0, i * 18, true, true);
                slotGroup.addWidget(slot);
            }
            slotGroup.setSelfPosition((this.getSizeWidth() + slotGroup.getSizeWidth() - 18) / 2,
                    50);
            this.addWidget(slotGroup);
        }
    }
}
