package com.gregtechceu.gtceu.api.gui;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.gui.widget.SlotWidget;
import com.gregtechceu.gtceu.api.item.module.AppliedItemModule;
import com.gregtechceu.gtceu.api.item.module.IModularItem;
import com.gregtechceu.gtceu.api.transfer.item.CustomItemStackHandler;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.ArrayList;
import java.util.List;

public record ModuleUIHolder(IItemHandlerModifiable handler, boolean isRemote) implements IUIHolder {

    private static NonNullList<ItemStack> stacksFromPlayer(Player player) {
        NonNullList<ItemStack> list = NonNullList.create();
        list.addAll(player.getInventory().armor);
        list.addAll(player.getInventory().offhand);
        list.addAll(NonNullList.withSize(4, ItemStack.EMPTY));
        list.addAll(player.getInventory().items);
        return list;
    }

    public ModuleUIHolder(Player player) {
        this(stacksFromPlayer(player), player.level().isClientSide);
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
        WidgetGroup group = new WidgetGroup(0, 50, 400, 300);
        WidgetGroup modulesList = new WidgetGroup(164, 50, 100, 300);
        WidgetGroup moduleConfig = new WidgetGroup(164 + modulesList.getSizeWidth(), 50, 100, 300);
        group.addWidget(modulesList);
        group.addWidget(moduleConfig);
        int size = handler.getSlots();
        for (int i = 0; i < size; i++) {
            int row = i / 9;
            int col = i % 9;
            final ItemStack stack = handler.getStackInSlot(i);
            SlotWidget slot = new SlotWidget(handler, i, col * 18, row * 18) {

                @Override
                public ItemStack slotClick(int dragType, ClickType clickType, Player player) {
                    modulesList.clearAllWidgets();
                    IModularItem modularItem = GTCapabilityHelper.getModularItem(stack);
                    if (modularItem != null) {
                        int y = 0;
                        for (AppliedItemModule module : modularItem.getAppliedModules()) {
                            List<Component> tooltip = new ArrayList<>();
                            module.appendHoverText(null, TooltipFlag.NORMAL, tooltip);
                            modulesList.addWidget(new ButtonWidget(
                                    0, y,
                                    modulesList.getSizeWidth(), 10,
                                    click -> {})
                                    .setBackground(
                                            new ColorRectTexture(0xFF222222),
                                            new ColorBorderTexture(1, 0xFF111111),
                                            new TextTexture(tooltip.get(0).getString())));
                            y += 10;
                        }
                    }
                    return stack;
                }
            };
            group.addWidget(slot);
        }
        return group;
    }

    @Override
    public ModularUI createUI(Player player) {
        return new ModularUI(400, 300, this, player).widget(createUIWidget(player));
    }

    @Override
    public boolean isInvalid() {
        return false;
    }

    @Override
    public void markAsDirty() {}
}
