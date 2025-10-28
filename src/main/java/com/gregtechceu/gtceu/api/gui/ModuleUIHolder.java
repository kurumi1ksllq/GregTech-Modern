package com.gregtechceu.gtceu.api.gui;

import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

public record ModuleUIHolder(IItemHandlerModifiable handler, boolean isRemote) implements IUIHolder {
    public ModuleUIHolder(Player player) {
        this(player.getInventory().items, player.level().isClientSide);
    }

    public ModuleUIHolder(NonNullList<ItemStack> stacks, boolean isRemote) {
        this(new CustomItemStackHandler(stacks), isRemote);
    }

    public void writeToByteBuf(FriendlyByteBuf buf) {
        buf.writeInt(handler.getSlots());
        for (int i = 0; i < handler.getSlots(); i++)
            buf.writeJsonWithCodec(ItemStack.CODEC, handler.getStackInSlot(i));
    }

    public static ModuleUIHolder fromByteBuf(FriendlyByteBuf buf) {
        int size = buf.readInt();
        NonNullList<ItemStack> stacks = NonNullList.create();
        for (int i = 0; i < size; i++) stacks.add(buf.readJsonWithCodec(ItemStack.CODEC));
        return new ModuleUIHolder(stacks, true);
    }

    public Widget createUIWidget(Player player) {
        WidgetGroup group = new WidgetGroup();
        int size = handler.getSlots();
        for (int i = 0; i < size; i++) {
            int row = i/9;
            int col = i % 9;
            SlotWidget slot = new SlotWidget(handler, i, 50 + col*16, 50 + row*16) {
                @Override
                public boolean mouseClicked(double mouseX, double mouseY, int button) {
                    if (slotReference != null && isMouseOverElement(mouseX, mouseY) && gui != null) {
                        ItemStack stack = slotReference.getItem();
                        //TODO actually make it work
                    }
                    return true;
                }
            };
            slot.setCanTakeItems(false);
            slot.setCanPutItems(false);
            group.addWidget(slot);
        }
        return group;
    }

    @Override
    public ModularUI createUI(Player player) {
        return new ModularUI(this, player).widget(createUIWidget(player));
    }

    @Override
    public boolean isInvalid() {
        return false;
    }

    @Override
    public void markAsDirty() {}
}
