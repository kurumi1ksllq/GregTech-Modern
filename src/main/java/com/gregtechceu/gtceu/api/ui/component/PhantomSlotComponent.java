package com.gregtechceu.gtceu.api.ui.component;

import com.gregtechceu.gtceu.api.ui.ingredient.GhostIngredientSlot;

import com.lowdragmc.lowdraglib.gui.editor.annotation.ConfigSetter;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.function.Predicate;

@Accessors(fluent = true, chain = true)
public class PhantomSlotComponent extends SlotComponent implements GhostIngredientSlot<ItemStack> {

    @Setter
    private boolean clearSlotOnRightClick;

    @Setter
    private int maxStackSize = 64;

    private Predicate<ItemStack> validator = stack -> true;

    public PhantomSlotComponent() {
        super(0);
    }

    public PhantomSlotComponent(IItemHandlerModifiable itemHandler, int slotIndex) {
        super(itemHandler, slotIndex);
        this.canInsert = false;
        this.canExtract = false;
    }

    public PhantomSlotComponent(IItemHandlerModifiable itemHandler, int slotIndex, Predicate<ItemStack> validator) {
        super(itemHandler, slotIndex);
        this.canInsert = false;
        this.canExtract = false;
        this.validator = validator;
    }

    @ConfigSetter(field = "canTakeItems")
    public PhantomSlotComponent canExtractOverride(boolean v) {
        // you cant modify it
        return this;
    }

    @ConfigSetter(field = "canPutItems")
    public PhantomSlotComponent canInsertOverride(boolean v) {
        // you cant modify it
        return this;
    }

    @Override
    public boolean onMouseDown(double mouseX, double mouseY, int button) {
        if (slot != null && isMouseOverElement(mouseX, mouseY)) {
            if (!getCarried().isEmpty()) {
                slot.set(getCarried());
            } else if (button == 1 && clearSlotOnRightClick && !slot.getItem().isEmpty()) {
                slot.set(ItemStack.EMPTY);
                // sendMessage(2, buf -> {});
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean slotClick(int mouseButton, ClickType clickTypeIn, Player player) {
        if (slot != null) {
            ItemStack stackHeld = getCarried();
            slotClickPhantom(slot, mouseButton, clickTypeIn, stackHeld);
            return true;
        }
        return false;
    }

    @Override
    public void setGhostIngredient(@NotNull ItemStack ingredient) {
        long id = Minecraft.getInstance().getWindow().getWindow();
        boolean shiftDown = InputConstants.isKeyDown(id, GLFW.GLFW_KEY_LEFT_SHIFT);
        ClickType clickType = shiftDown ? ClickType.QUICK_MOVE : ClickType.PICKUP;
        slotClickPhantom(slot, 0, clickType, ingredient);
        /*
         * sendMessage(1, buffer -> {
         * buffer.writeItem(ingredient);
         * buffer.writeVarInt(0);
         * buffer.writeBoolean(shiftDown);
         * });
         */
    }

    @Override
    public Class<ItemStack> ghostIngredientClass() {
        return ItemStack.class;
    }

    /*
     * @Override
     * public void receiveMessage(int id, FriendlyByteBuf buffer) {
     * if (slot != null && id == 1) {
     * ItemStack stackHeld = buffer.readItem();
     * int mouseButton = buffer.readVarInt();
     * boolean shiftKeyDown = buffer.readBoolean();
     * ClickType clickType = shiftKeyDown ? ClickType.QUICK_MOVE : ClickType.PICKUP;
     * slotClickPhantom(slot, mouseButton, clickType, stackHeld);
     * } else if (slot != null && id == 2) {
     * slot.set(ItemStack.EMPTY);
     * }
     * }
     */

    public ItemStack slotClickPhantom(Slot slot, int mouseButton, ClickType clickTypeIn, ItemStack stackHeld) {
        ItemStack stack = ItemStack.EMPTY;

        ItemStack stackSlot = slot.getItem();
        if (!stackSlot.isEmpty()) {
            stack = stackSlot.copy();
        }

        if (mouseButton == 2) {
            fillPhantomSlot(slot, ItemStack.EMPTY, mouseButton);
        } else if (mouseButton == 0 || mouseButton == 1) {

            if (stackSlot.isEmpty()) {
                if (!stackHeld.isEmpty()) {
                    fillPhantomSlot(slot, stackHeld, mouseButton);
                }
            } else if (stackHeld.isEmpty()) {
                adjustPhantomSlot(slot, mouseButton, clickTypeIn);
            } else {
                if (!areItemsEqual(stackSlot, stackHeld)) {
                    adjustPhantomSlot(slot, mouseButton, clickTypeIn);
                }
                fillPhantomSlot(slot, stackHeld, mouseButton);
            }
        } else if (mouseButton == 5) {
            if (!slot.hasItem()) {
                fillPhantomSlot(slot, stackHeld, mouseButton);
            }
        }
        return stack;
    }

    private void adjustPhantomSlot(Slot slot, int mouseButton, ClickType clickTypeIn) {
        ItemStack stackSlot = slot.getItem();
        int stackSize;
        if (clickTypeIn == ClickType.QUICK_MOVE) {
            stackSize = mouseButton == 0 ? (stackSlot.getCount() + 1) / 2 : stackSlot.getCount() * 2;
        } else {
            stackSize = mouseButton == 0 ? stackSlot.getCount() - 1 : stackSlot.getCount() + 1;
        }

        if (stackSize > slot.getMaxStackSize()) {
            stackSize = slot.getMaxStackSize();
        }

        stackSlot.setCount(Math.min(maxStackSize, stackSize));

        slot.set(stackSlot);
    }

    private void fillPhantomSlot(Slot slot, ItemStack stackHeld, int mouseButton) {
        if (stackHeld.isEmpty()) {
            slot.set(ItemStack.EMPTY);
            return;
        }

        int stackSize = mouseButton == 0 ? stackHeld.getCount() : 1;
        if (stackSize > slot.getMaxStackSize()) {
            stackSize = slot.getMaxStackSize();
        }
        ItemStack phantomStack = stackHeld.copy();
        phantomStack.setCount(Math.min(maxStackSize, stackSize));
        if (validator.test(phantomStack)) slot.set(phantomStack);
    }

    public boolean areItemsEqual(ItemStack itemStack1, ItemStack itemStack2) {
        return ItemStack.matches(itemStack1, itemStack2);
    }
}
